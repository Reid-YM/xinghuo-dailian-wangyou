package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.repository.ServiceItemRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceCatalogService {
    private final ServiceItemRepository repo;
    private final AuditService audit;
    private final TaxonomyService taxonomy;

    public ServiceCatalogService(
            ServiceItemRepository repo,
            AuditService audit,
            TaxonomyService taxonomy) {
        this.repo = repo;
        this.audit = audit;
        this.taxonomy = taxonomy;
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.ServiceResponse> publicList() {
        return repo.findPublic(Instant.now()).stream()
                .filter(item -> taxonomy.publicAllowed(item.getGameCode(), item.getCategory()))
                .map(this::pub)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceItem publicBySlug(String slug) {
        ServiceItem item = repo.findBySlug(slug).orElseThrow(NotFoundException::new);
        if (!item.isActive() || item.getStatus() == ServiceItem.Status.OFFLINE) {
            throw new NotFoundException();
        }
        if (item.getExpiresAt() != null && !item.getExpiresAt().isAfter(Instant.now())) {
            throw new NotFoundException();
        }
        if (!taxonomy.publicAllowed(item.getGameCode(), item.getCategory())) {
            throw new NotFoundException();
        }
        return item;
    }

    @Transactional(readOnly = true)
    public List<ServiceItem> publicItems() {
        return repo.findPublic(Instant.now()).stream()
                .filter(item -> taxonomy.publicAllowed(item.getGameCode(), item.getCategory()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.AdminServiceResponse> adminList() {
        return repo.findAll().stream()
                .sorted(Comparator.comparingInt(ServiceItem::getSortOrder).thenComparing(ServiceItem::getId))
                .map(this::admin)
                .toList();
    }

    @Transactional
    public ApiDtos.AdminServiceResponse create(
            ApiDtos.ServiceUpsert request,
            String operator,
            HttpServletRequest httpRequest) {
        validatePrice(request);
        if (repo.findBySlug(request.slug()).isPresent()) {
            throw new ConflictException("服务标识已存在");
        }

        Instant now = Instant.now();
        ServiceItem.PriceMode priceMode = priceMode(request.priceMode());
        ServiceItem item = ServiceItem.builder()
                .slug(request.slug())
                .gameCode(validGame(request.gameCode(), request.category()))
                .name(request.name().trim())
                .category(request.category().trim().toUpperCase(Locale.ROOT))
                .shortDesc(request.shortDesc().trim())
                .priceMode(priceMode)
                .priceFixed(priceMode == ServiceItem.PriceMode.FIXED ? request.priceFixed() : null)
                .priceMin(priceMode == ServiceItem.PriceMode.RANGE ? request.priceMin() : null)
                .priceMax(priceMode == ServiceItem.PriceMode.RANGE ? request.priceMax() : null)
                .durationText(request.durationText().trim())
                .status(status(request.status()))
                .coverImage(trimToNull(request.coverImage()))
                .hotScore(zeroIfNull(request.hotScore()))
                .manualPriority(request.manualPriority() == null ? 0 : request.manualPriority())
                .expiresAt(request.expiresAt())
                .active(request.active() == null || request.active() != false)
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .publishedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        repo.save(item);

        ApiDtos.AdminServiceResponse after = admin(item);
        audit.log(
                "SERVICE_CREATE",
                "SERVICE",
                item.getId().toString(),
                operator,
                null,
                after,
                request.reason(),
                httpRequest);
        return after;
    }

    @Transactional
    public ApiDtos.AdminServiceResponse update(
            Long id,
            ApiDtos.ServiceUpsert request,
            String operator,
            HttpServletRequest httpRequest) {
        validatePrice(request);
        ServiceItem item = repo.findById(id).orElseThrow(NotFoundException::new);
        ApiDtos.AdminServiceResponse before = admin(item);
        repo.findBySlug(request.slug())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("服务标识已存在");
                });

        ServiceItem.PriceMode priceMode = priceMode(request.priceMode());
        item.setSlug(request.slug());
        item.setGameCode(validGame(request.gameCode(), request.category()));
        item.setName(request.name().trim());
        item.setCategory(request.category().trim().toUpperCase(Locale.ROOT));
        item.setShortDesc(request.shortDesc().trim());
        item.setPriceMode(priceMode);
        item.setPriceFixed(priceMode == ServiceItem.PriceMode.FIXED ? request.priceFixed() : null);
        item.setPriceMin(priceMode == ServiceItem.PriceMode.RANGE ? request.priceMin() : null);
        item.setPriceMax(priceMode == ServiceItem.PriceMode.RANGE ? request.priceMax() : null);
        item.setDurationText(request.durationText().trim());
        item.setStatus(status(request.status()));
        item.setCoverImage(trimToNull(request.coverImage()));
        item.setHotScore(zeroIfNull(request.hotScore()));
        item.setManualPriority(request.manualPriority() == null ? 0 : request.manualPriority());
        item.setExpiresAt(request.expiresAt());
        item.setActive(request.active() == null || request.active() != false);
        item.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        item.setUpdatedAt(Instant.now());

        ApiDtos.AdminServiceResponse after = admin(item);
        audit.log(
                "SERVICE_UPDATE",
                "SERVICE",
                id.toString(),
                operator,
                before,
                after,
                request.reason(),
                httpRequest);
        return after;
    }

    @Transactional
    public void delete(Long id, String operator, HttpServletRequest httpRequest) {
        ServiceItem item = repo.findById(id).orElseThrow(NotFoundException::new);
        ApiDtos.AdminServiceResponse before = admin(item);
        item.setActive(false);
        item.setStatus(ServiceItem.Status.OFFLINE);
        item.setUpdatedAt(Instant.now());
        audit.log(
                "SERVICE_OFFLINE",
                "SERVICE",
                id.toString(),
                operator,
                before,
                admin(item),
                "后台下线",
                httpRequest);
    }

    @Transactional
    public ApiDtos.AdminServiceResponse restore(
            Long id,
            String operator,
            HttpServletRequest httpRequest) {
        ServiceItem item = repo.findById(id).orElseThrow(NotFoundException::new);
        taxonomy.validate(item.getGameCode(), item.getCategory());
        ApiDtos.AdminServiceResponse before = admin(item);
        item.setActive(true);
        if (item.getStatus() == ServiceItem.Status.OFFLINE) {
            item.setStatus(ServiceItem.Status.AVAILABLE);
        }
        item.setUpdatedAt(Instant.now());

        ApiDtos.AdminServiceResponse after = admin(item);
        audit.log(
                "SERVICE_RESTORE",
                "SERVICE",
                id.toString(),
                operator,
                before,
                after,
                "恢复服务项目上架",
                httpRequest);
        return after;
    }

    private void validatePrice(ApiDtos.ServiceUpsert request) {
        ServiceItem.PriceMode mode = priceMode(request.priceMode());
        if (mode == ServiceItem.PriceMode.FIXED && request.priceFixed() == null) {
            throw new BadRequestException("固定价格模式必须填写固定价");
        }
        if (mode == ServiceItem.PriceMode.RANGE
                && (request.priceMin() == null
                || request.priceMax() == null
                || request.priceMin().compareTo(request.priceMax()) > 0)) {
            throw new BadRequestException("区间价格必须填写合法的最低价和最高价");
        }
        for (BigDecimal value : Arrays.asList(
                request.priceFixed(), request.priceMin(), request.priceMax())) {
            if (value != null && value.signum() < 0) {
                throw new BadRequestException("价格不能为负数");
            }
        }
    }

    public ApiDtos.ServiceResponse pub(ServiceItem item) {
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

    public ApiDtos.AdminServiceResponse admin(ServiceItem item) {
        return new ApiDtos.AdminServiceResponse(
                item.getId(),
                item.getSlug(),
                item.getGameCode(),
                item.getName(),
                item.getCategory(),
                item.getShortDesc(),
                item.getPriceMode().name(),
                item.getPriceFixed(),
                item.getPriceMin(),
                item.getPriceMax(),
                item.getDurationText(),
                item.getStatus().name(),
                item.getCoverImage(),
                item.getHotScore(),
                item.getManualPriority(),
                item.getExpiresAt(),
                item.isActive(),
                item.getSortOrder(),
                item.getVersion());
    }

    private String validGame(String gameCode, String category) {
        String normalized = taxonomy.normalizeGame(gameCode);
        taxonomy.validate(normalized, category);
        return normalized;
    }

    private ServiceItem.PriceMode priceMode(String value) {
        try {
            return ServiceItem.PriceMode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new BadRequestException("非法价格模式");
        }
    }

    private ServiceItem.Status status(String value) {
        try {
            return ServiceItem.Status.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new BadRequestException("非法服务状态");
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
