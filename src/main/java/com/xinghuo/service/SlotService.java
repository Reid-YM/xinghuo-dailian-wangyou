/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.servlet.http.HttpServletRequest
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.HotSlot;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.repository.HotSlotRepository;
import com.xinghuo.repository.ServiceItemRepository;
import com.xinghuo.taxonomy.application.TaxonomyApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlotService {
    private final HotSlotRepository repo;
    private final ServiceItemRepository services;
    private final AuditService audit;
    private final TaxonomyApplicationService taxonomy;

    @Transactional(readOnly=true)
    public List<ApiDtos.ServiceResponse> publicHotServices() {
        List<ServiceItem> publicList = this.services.findPublic(Instant.now()).stream().filter(x -> this.taxonomy.publicAllowed(x.getGameCode(), x.getCategory())).toList();
        LinkedHashMap available = new LinkedHashMap();
        publicList.forEach(s -> available.put(s.getId(), s));
        ArrayList<ServiceItem> out = new ArrayList<ServiceItem>();
        HashSet<Long> added = new HashSet<Long>();
        for (HotSlot slot : this.repo.findAllByOrderBySlotNoAsc()) {
            ServiceItem item;
            if (slot.getMode() != HotSlot.Mode.MANUAL || slot.getService() == null || (item = (ServiceItem)available.get(slot.getService().getId())) == null || !added.add(item.getId())) continue;
            out.add(item);
        }
        for (ServiceItem s2 : publicList) {
            if (out.size() >= 8) break;
            if (!added.add(s2.getId())) continue;
            out.add(s2);
        }
        return out.stream().limit(8L).map(this::pub).toList();
    }

    @Transactional(readOnly=true)
    public List<ApiDtos.SlotResponse> list() {
        return this.repo.findAllByOrderBySlotNoAsc().stream().map(s -> new ApiDtos.SlotResponse(s.getSlotNo(), s.getService() == null ? null : s.getService().getId(), s.getService() == null ? null : s.getService().getName(), s.getMode().name())).toList();
    }

    @Transactional
    public ApiDtos.SlotResponse update(ApiDtos.SlotUpdate r, String operator, HttpServletRequest req) {
        ServiceItem item;
        HotSlot s = this.repo.findBySlotNo(r.slotNo()).orElseThrow(NotFoundException::new);
        ApiDtos.SlotResponse before = this.response(s);
        HotSlot.Mode mode = this.parseMode(r.mode());
        ServiceItem serviceItem = item = r.serviceId() == null ? null : (ServiceItem)this.services.findById(r.serviceId()).orElseThrow(NotFoundException::new);
        if (mode == HotSlot.Mode.AUTO) {
            item = null;
        }
        if (mode == HotSlot.Mode.MANUAL && item == null) {
            throw new BadRequestException("手动指定模式必须选择服务");
        }
        if (!(item == null || item.isActive() && item.getStatus() != ServiceItem.Status.OFFLINE && this.taxonomy.publicAllowed(item.getGameCode(), item.getCategory()) && (item.getExpiresAt() == null || item.getExpiresAt().isAfter(Instant.now())))) {
            throw new BadRequestException("该服务当前不可用于首页展示");
        }
        s.setMode(mode);
        s.setService(item);
        s.setUpdatedAt(Instant.now());
        ApiDtos.SlotResponse after = this.response(s);
        this.audit.log("SLOT_UPDATE", "HOT_SLOT", String.valueOf(r.slotNo()), operator, before, after, r.reason(), req);
        return after;
    }

    private ApiDtos.SlotResponse response(HotSlot s) {
        return new ApiDtos.SlotResponse(s.getSlotNo(), s.getService() == null ? null : s.getService().getId(), s.getService() == null ? null : s.getService().getName(), s.getMode().name());
    }

    private HotSlot.Mode parseMode(String s) {
        try {
            return HotSlot.Mode.valueOf(s.toUpperCase(Locale.ROOT));
        }
        catch (Exception e) {
            throw new BadRequestException("非法展示位模式");
        }
    }

    private ApiDtos.ServiceResponse pub(ServiceItem s) {
        return new ApiDtos.ServiceResponse(s.getId(), s.getSlug(), s.getGameCode(), s.getName(), s.getCategory().toLowerCase(Locale.ROOT), s.getShortDesc(), s.getPriceMode().name().toLowerCase(Locale.ROOT), s.getPriceFixed(), s.getPriceMin(), s.getPriceMax(), s.getDurationText(), s.getStatus().name().toLowerCase(Locale.ROOT), s.getCoverImage());
    }

    @Generated
    public SlotService(HotSlotRepository repo, ServiceItemRepository services, AuditService audit, TaxonomyApplicationService taxonomy) {
        this.repo = repo;
        this.services = services;
        this.audit = audit;
        this.taxonomy = taxonomy;
    }
}
