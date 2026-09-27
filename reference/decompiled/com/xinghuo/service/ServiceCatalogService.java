/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.dto.ApiDtos$AdminServiceResponse
 *  com.xinghuo.dto.ApiDtos$ServiceResponse
 *  com.xinghuo.dto.ApiDtos$ServiceUpsert
 *  com.xinghuo.exception.BadRequestException
 *  com.xinghuo.exception.ConflictException
 *  com.xinghuo.exception.NotFoundException
 *  com.xinghuo.model.ServiceItem
 *  com.xinghuo.model.ServiceItem$PriceMode
 *  com.xinghuo.model.ServiceItem$Status
 *  com.xinghuo.repository.ServiceItemRepository
 *  com.xinghuo.service.AuditService
 *  com.xinghuo.service.ServiceCatalogService
 *  com.xinghuo.service.TaxonomyService
 *  jakarta.servlet.http.HttpServletRequest
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.repository.ServiceItemRepository;
import com.xinghuo.service.AuditService;
import com.xinghuo.service.TaxonomyService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceCatalogService {
    private final ServiceItemRepository repo;
    private final AuditService audit;
    private final TaxonomyService taxonomy;

    @Transactional(readOnly=true)
    public List<ApiDtos.ServiceResponse> publicList() {
        return this.repo.findPublic(Instant.now()).stream().filter(x -> this.taxonomy.publicAllowed(x.getGameCode(), x.getCategory())).map(arg_0 -> this.pub(arg_0)).toList();
    }

    @Transactional(readOnly=true)
    public ServiceItem publicBySlug(String slug) {
        ServiceItem s = (ServiceItem)this.repo.findBySlug(slug).orElseThrow(NotFoundException::new);
        if (!s.isActive() || s.getStatus() == ServiceItem.Status.OFFLINE) {
            throw new NotFoundException();
        }
        if (s.getExpiresAt() != null && !s.getExpiresAt().isAfter(Instant.now())) {
            throw new NotFoundException();
        }
        if (!this.taxonomy.publicAllowed(s.getGameCode(), s.getCategory())) {
            throw new NotFoundException();
        }
        return s;
    }

    @Transactional(readOnly=true)
    public List<ServiceItem> publicItems() {
        return this.repo.findPublic(Instant.now()).stream().filter(x -> this.taxonomy.publicAllowed(x.getGameCode(), x.getCategory())).toList();
    }

    @Transactional(readOnly=true)
    public List<ApiDtos.AdminServiceResponse> adminList() {
        return this.repo.findAll().stream().sorted(Comparator.comparingInt(ServiceItem::getSortOrder).thenComparing(ServiceItem::getId)).map(arg_0 -> this.admin(arg_0)).toList();
    }

    @Transactional
    public ApiDtos.AdminServiceResponse create(ApiDtos.ServiceUpsert r, String operator, HttpServletRequest req) {
        this.validatePrice(r);
        if (this.repo.findBySlug(r.slug()).isPresent()) {
            throw new ConflictException("\u670d\u52a1\u6807\u8bc6\u5df2\u5b58\u5728");
        }
        Instant now = Instant.now();
        ServiceItem.PriceMode priceMode = this.mode(r.priceMode());
        ServiceItem s = ServiceItem.builder().slug(r.slug()).gameCode(this.validGame(r.gameCode(), r.category())).name(r.name().trim()).category(r.category().trim().toUpperCase(Locale.ROOT)).shortDesc(r.shortDesc().trim()).priceMode(priceMode).priceFixed(priceMode == ServiceItem.PriceMode.FIXED ? r.priceFixed() : null).priceMin(priceMode == ServiceItem.PriceMode.RANGE ? r.priceMin() : null).priceMax(priceMode == ServiceItem.PriceMode.RANGE ? r.priceMax() : null).durationText(r.durationText().trim()).status(this.status(r.status())).coverImage(this.trimNull(r.coverImage())).hotScore(this.nvl(r.hotScore())).manualPriority(r.manualPriority() == null ? 0 : r.manualPriority()).expiresAt(r.expiresAt()).active(r.active() == null || r.active() != false).sortOrder(r.sortOrder() == null ? 0 : r.sortOrder()).publishedAt(now).createdAt(now).updatedAt(now).build();
        this.repo.save((Object)s);
        ApiDtos.AdminServiceResponse after = this.admin(s);
        this.audit.log("SERVICE_CREATE", "SERVICE", s.getId().toString(), operator, null, (Object)after, r.reason(), req);
        return after;
    }

    @Transactional
    public ApiDtos.AdminServiceResponse update(Long id, ApiDtos.ServiceUpsert r, String operator, HttpServletRequest req) {
        this.validatePrice(r);
        ServiceItem s = (ServiceItem)this.repo.findById((Object)id).orElseThrow(NotFoundException::new);
        ApiDtos.AdminServiceResponse before = this.admin(s);
        this.repo.findBySlug(r.slug()).filter(x -> !x.getId().equals(id)).ifPresent(x -> {
            throw new ConflictException("\u670d\u52a1\u6807\u8bc6\u5df2\u5b58\u5728");
        });
        ServiceItem.PriceMode priceMode = this.mode(r.priceMode());
        s.setSlug(r.slug());
        s.setGameCode(this.validGame(r.gameCode(), r.category()));
        s.setName(r.name().trim());
        s.setCategory(r.category().trim().toUpperCase(Locale.ROOT));
        s.setShortDesc(r.shortDesc().trim());
        s.setPriceMode(priceMode);
        s.setPriceFixed(priceMode == ServiceItem.PriceMode.FIXED ? r.priceFixed() : null);
        s.setPriceMin(priceMode == ServiceItem.PriceMode.RANGE ? r.priceMin() : null);
        s.setPriceMax(priceMode == ServiceItem.PriceMode.RANGE ? r.priceMax() : null);
        s.setDurationText(r.durationText().trim());
        s.setStatus(this.status(r.status()));
        s.setCoverImage(this.trimNull(r.coverImage()));
        s.setHotScore(this.nvl(r.hotScore()));
        s.setManualPriority(r.manualPriority() == null ? 0 : r.manualPriority());
        s.setExpiresAt(r.expiresAt());
        s.setActive(r.active() == null || r.active() != false);
        s.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder());
        s.setUpdatedAt(Instant.now());
        ApiDtos.AdminServiceResponse after = this.admin(s);
        this.audit.log("SERVICE_UPDATE", "SERVICE", id.toString(), operator, (Object)before, (Object)after, r.reason(), req);
        return after;
    }

    @Transactional
    public void delete(Long id, String operator, HttpServletRequest req) {
        ServiceItem s = (ServiceItem)this.repo.findById((Object)id).orElseThrow(NotFoundException::new);
        ApiDtos.AdminServiceResponse before = this.admin(s);
        s.setActive(false);
        s.setStatus(ServiceItem.Status.OFFLINE);
        s.setUpdatedAt(Instant.now());
        this.audit.log("SERVICE_OFFLINE", "SERVICE", id.toString(), operator, (Object)before, (Object)this.admin(s), "\u540e\u53f0\u4e0b\u7ebf", req);
    }

    @Transactional
    public ApiDtos.AdminServiceResponse restore(Long id, String operator, HttpServletRequest req) {
        ServiceItem s = (ServiceItem)this.repo.findById((Object)id).orElseThrow(NotFoundException::new);
        this.taxonomy.validate(s.getGameCode(), s.getCategory());
        ApiDtos.AdminServiceResponse before = this.admin(s);
        s.setActive(true);
        if (s.getStatus() == ServiceItem.Status.OFFLINE) {
            s.setStatus(ServiceItem.Status.AVAILABLE);
        }
        s.setUpdatedAt(Instant.now());
        ApiDtos.AdminServiceResponse after = this.admin(s);
        this.audit.log("SERVICE_RESTORE", "SERVICE", id.toString(), operator, (Object)before, (Object)after, "\u6062\u590d\u670d\u52a1\u9879\u76ee\u4e0a\u67b6", req);
        return after;
    }

    private void validatePrice(ApiDtos.ServiceUpsert r) {
        ServiceItem.PriceMode m = this.mode(r.priceMode());
        if (m == ServiceItem.PriceMode.FIXED && r.priceFixed() == null) {
            throw new BadRequestException("\u56fa\u5b9a\u4ef7\u683c\u6a21\u5f0f\u5fc5\u987b\u586b\u5199\u56fa\u5b9a\u4ef7");
        }
        if (m == ServiceItem.PriceMode.RANGE && (r.priceMin() == null || r.priceMax() == null || r.priceMin().compareTo(r.priceMax()) > 0)) {
            throw new BadRequestException("\u533a\u95f4\u4ef7\u683c\u5fc5\u987b\u586b\u5199\u5408\u6cd5\u7684\u6700\u4f4e\u4ef7\u548c\u6700\u9ad8\u4ef7");
        }
        for (BigDecimal x : Arrays.asList(r.priceFixed(), r.priceMin(), r.priceMax())) {
            if (x == null || x.signum() >= 0) continue;
            throw new BadRequestException("\u4ef7\u683c\u4e0d\u80fd\u4e3a\u8d1f\u6570");
        }
    }

    public ApiDtos.ServiceResponse pub(ServiceItem s) {
        return new ApiDtos.ServiceResponse(s.getId(), s.getSlug(), s.getGameCode(), s.getName(), s.getCategory().toLowerCase(Locale.ROOT), s.getShortDesc(), s.getPriceMode().name().toLowerCase(Locale.ROOT), s.getPriceFixed(), s.getPriceMin(), s.getPriceMax(), s.getDurationText(), s.getStatus().name().toLowerCase(Locale.ROOT), s.getCoverImage());
    }

    public ApiDtos.AdminServiceResponse admin(ServiceItem s) {
        return new ApiDtos.AdminServiceResponse(s.getId(), s.getSlug(), s.getGameCode(), s.getName(), s.getCategory(), s.getShortDesc(), s.getPriceMode().name(), s.getPriceFixed(), s.getPriceMin(), s.getPriceMax(), s.getDurationText(), s.getStatus().name(), s.getCoverImage(), s.getHotScore(), s.getManualPriority(), s.getExpiresAt(), s.isActive(), s.getSortOrder(), s.getVersion());
    }

    private String validGame(String gameCode, String category) {
        String g = this.taxonomy.normalizeGame(gameCode);
        this.taxonomy.validate(g, category);
        return g;
    }

    private ServiceItem.PriceMode mode(String s) {
        try {
            return ServiceItem.PriceMode.valueOf((String)s.toUpperCase(Locale.ROOT));
        }
        catch (Exception e) {
            throw new BadRequestException("\u975e\u6cd5\u4ef7\u683c\u6a21\u5f0f");
        }
    }

    private ServiceItem.Status status(String s) {
        try {
            return ServiceItem.Status.valueOf((String)s.toUpperCase(Locale.ROOT));
        }
        catch (Exception e) {
            throw new BadRequestException("\u975e\u6cd5\u670d\u52a1\u72b6\u6001");
        }
    }

    private BigDecimal nvl(BigDecimal x) {
        return x == null ? BigDecimal.ZERO : x;
    }

    private String trimNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    @Generated
    public ServiceCatalogService(ServiceItemRepository repo, AuditService audit, TaxonomyService taxonomy) {
        this.repo = repo;
        this.audit = audit;
        this.taxonomy = taxonomy;
    }
}

