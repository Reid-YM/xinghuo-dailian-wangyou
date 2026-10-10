package com.xinghuo.slot.infrastructure;

import com.xinghuo.audit.infrastructure.AuditService;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.HotSlot;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.repository.HotSlotRepository;
import com.xinghuo.repository.ServiceItemRepository;
import com.xinghuo.slot.application.SlotPort;
import com.xinghuo.slot.application.SlotPublicServiceView;
import com.xinghuo.slot.application.SlotView;
import com.xinghuo.slot.application.UpdateSlotCommand;
import com.xinghuo.taxonomy.application.TaxonomyApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Slot-owned selection, persistence, validation, and audit adapter. */
@Component
public class SlotPersistenceAdapter implements SlotPort {
    private static final int PUBLIC_SLOT_LIMIT = 8;

    private final HotSlotRepository repo;
    private final ServiceItemRepository services;
    private final AuditService audit;
    private final TaxonomyApplicationService taxonomy;

    public SlotPersistenceAdapter(
            HotSlotRepository repo,
            ServiceItemRepository services,
            AuditService audit,
            TaxonomyApplicationService taxonomy) {
        this.repo = repo;
        this.services = services;
        this.audit = audit;
        this.taxonomy = taxonomy;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlotPublicServiceView> publicHotServices() {
        List<ServiceItem> publicList = services.findPublic(Instant.now()).stream()
                .filter(item -> taxonomy.publicAllowed(item.getGameCode(), item.getCategory()))
                .toList();

        LinkedHashMap<Long, ServiceItem> available = new LinkedHashMap<>();
        publicList.forEach(item -> available.put(item.getId(), item));

        ArrayList<ServiceItem> selected = new ArrayList<>();
        Set<Long> added = new HashSet<>();
        for (HotSlot slot : repo.findAllByOrderBySlotNoAsc()) {
            if (slot.getMode() != HotSlot.Mode.MANUAL || slot.getService() == null) {
                continue;
            }
            ServiceItem item = available.get(slot.getService().getId());
            if (item != null && added.add(item.getId())) {
                selected.add(item);
            }
        }

        for (ServiceItem item : publicList) {
            if (selected.size() >= PUBLIC_SLOT_LIMIT) {
                break;
            }
            if (added.add(item.getId())) {
                selected.add(item);
            }
        }

        return selected.stream()
                .limit(PUBLIC_SLOT_LIMIT)
                .map(this::publicView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlotView> list() {
        return repo.findAllByOrderBySlotNoAsc().stream()
                .map(this::slotView)
                .toList();
    }

    @Override
    @Transactional
    public SlotView update(UpdateSlotCommand command, String operator) {
        HotSlot slot = repo.findBySlotNo(command.slotNo()).orElseThrow(NotFoundException::new);
        SlotView before = slotView(slot);
        HotSlot.Mode mode = parseMode(command.mode());
        ServiceItem item = command.serviceId() == null
                ? null
                : services.findById(command.serviceId()).orElseThrow(NotFoundException::new);

        if (mode == HotSlot.Mode.AUTO) {
            item = null;
        }
        if (mode == HotSlot.Mode.MANUAL && item == null) {
            throw new BadRequestException("手动指定模式必须选择服务");
        }
        if (item != null && !eligibleForPublicSlot(item)) {
            throw new BadRequestException("该服务当前不可用于首页展示");
        }

        slot.setMode(mode);
        slot.setService(item);
        slot.setUpdatedAt(Instant.now());
        SlotView after = slotView(slot);
        audit.log(
                "SLOT_UPDATE",
                "HOT_SLOT",
                String.valueOf(command.slotNo()),
                operator,
                before,
                after,
                command.reason(),
                currentRequest());
        return after;
    }

    private boolean eligibleForPublicSlot(ServiceItem item) {
        return item.isActive()
                && item.getStatus() != ServiceItem.Status.OFFLINE
                && taxonomy.publicAllowed(item.getGameCode(), item.getCategory())
                && (item.getExpiresAt() == null || item.getExpiresAt().isAfter(Instant.now()));
    }

    private SlotView slotView(HotSlot slot) {
        return new SlotView(
                slot.getSlotNo(),
                slot.getService() == null ? null : slot.getService().getId(),
                slot.getService() == null ? null : slot.getService().getName(),
                slot.getMode().name());
    }

    private HotSlot.Mode parseMode(String value) {
        try {
            return HotSlot.Mode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("非法展示位模式");
        }
    }

    private SlotPublicServiceView publicView(ServiceItem item) {
        return new SlotPublicServiceView(
                item.getId(),
                item.getSlug(),
                item.getGameCode(),
                item.getName(),
                item.getCategory().toLowerCase(Locale.ROOT),
                item.getShortDesc(),
                item.getPriceMode().name().toLowerCase(Locale.ROOT),
                item.getPriceFixed(),
                item.getPriceMin(),
                item.getPriceMax(),
                item.getDurationText(),
                item.getStatus().name().toLowerCase(Locale.ROOT),
                item.getCoverImage());
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest();
        }
        return null;
    }
}
