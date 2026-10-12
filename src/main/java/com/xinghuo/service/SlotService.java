package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.HotSlot;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.repository.HotSlotRepository;
import com.xinghuo.repository.ServiceItemRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlotService {
    private static final int PUBLIC_SLOT_LIMIT = 2;

    private final HotSlotRepository repo;
    private final ServiceItemRepository services;
    private final AuditService audit;
    private final TaxonomyService taxonomy;

    public SlotService(
            HotSlotRepository repo,
            ServiceItemRepository services,
            AuditService audit,
            TaxonomyService taxonomy) {
        this.repo = repo;
        this.services = services;
        this.audit = audit;
        this.taxonomy = taxonomy;
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.ServiceResponse> publicHotServices() {
        List<ServiceItem> publicItems = services.findPublic(Instant.now()).stream()
                .filter(item -> taxonomy.publicAllowed(item.getGameCode(), item.getCategory()))
                .toList();

        Map<Long, ServiceItem> availableById = new LinkedHashMap<>();
        publicItems.forEach(item -> availableById.put(item.getId(), item));

        List<ServiceItem> selected = new ArrayList<>();
        Set<Long> added = new HashSet<>();
        for (HotSlot slot : repo.findAllByOrderBySlotNoAsc()) {
            if (slot.getMode() != HotSlot.Mode.MANUAL || slot.getService() == null) {
                continue;
            }
            ServiceItem item = availableById.get(slot.getService().getId());
            if (item != null && added.add(item.getId())) {
                selected.add(item);
            }
        }

        for (ServiceItem item : publicItems) {
            if (selected.size() >= PUBLIC_SLOT_LIMIT) {
                break;
            }
            if (added.add(item.getId())) {
                selected.add(item);
            }
        }

        return selected.stream()
                .limit(PUBLIC_SLOT_LIMIT)
                .map(this::toPublicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.SlotResponse> list() {
        return repo.findAllByOrderBySlotNoAsc().stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public ApiDtos.SlotResponse update(
            ApiDtos.SlotUpdate request,
            String operator,
            HttpServletRequest httpRequest) {
        HotSlot slot = repo.findBySlotNo(request.slotNo()).orElseThrow(NotFoundException::new);
        ApiDtos.SlotResponse before = response(slot);
        HotSlot.Mode mode = parseMode(request.mode());

        ServiceItem item = request.serviceId() == null
                ? null
                : services.findById(request.serviceId()).orElseThrow(NotFoundException::new);
        if (mode == HotSlot.Mode.AUTO) {
            item = null;
        }
        if (mode == HotSlot.Mode.MANUAL && item == null) {
            throw new BadRequestException("手动指定模式必须选择服务");
        }
        if (item != null && !isAvailableForHomepage(item)) {
            throw new BadRequestException("该服务当前不可用于首页展示");
        }

        slot.setMode(mode);
        slot.setService(item);
        slot.setUpdatedAt(Instant.now());

        ApiDtos.SlotResponse after = response(slot);
        audit.log(
                "SLOT_UPDATE",
                "HOT_SLOT",
                String.valueOf(request.slotNo()),
                operator,
                before,
                after,
                request.reason(),
                httpRequest);
        return after;
    }

    private boolean isAvailableForHomepage(ServiceItem item) {
        return item.isActive()
                && item.getStatus() != ServiceItem.Status.OFFLINE
                && taxonomy.publicAllowed(item.getGameCode(), item.getCategory())
                && (item.getExpiresAt() == null || item.getExpiresAt().isAfter(Instant.now()));
    }

    private ApiDtos.SlotResponse response(HotSlot slot) {
        ServiceItem item = slot.getService();
        return new ApiDtos.SlotResponse(
                slot.getSlotNo(),
                item == null ? null : item.getId(),
                item == null ? null : item.getName(),
                slot.getMode().name());
    }

    private HotSlot.Mode parseMode(String value) {
        try {
            return HotSlot.Mode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new BadRequestException("非法展示位模式");
        }
    }

    private ApiDtos.ServiceResponse toPublicResponse(ServiceItem item) {
        return new ApiDtos.ServiceResponse(
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
}
