package com.xinghuo.catalog.infrastructure;

import com.xinghuo.catalog.application.CatalogAdminServiceView;
import com.xinghuo.catalog.application.CatalogPageServiceView;
import com.xinghuo.catalog.application.CatalogPort;
import com.xinghuo.catalog.application.CatalogServiceCommand;
import com.xinghuo.catalog.application.CatalogServiceView;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Direct Catalog persistence adapter; owns the behavior retired from ServiceCatalogService. */
@Component
public class CatalogPersistenceAdapter implements CatalogPort {
    private final ServiceItemRepository repo;
    private final AuditService audit;
    private final TaxonomyService taxonomy;

    public CatalogPersistenceAdapter(ServiceItemRepository repo, AuditService audit, TaxonomyService taxonomy) {
        this.repo = repo;
        this.audit = audit;
        this.taxonomy = taxonomy;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogServiceView> publicList() {
        return repo.findPublic(Instant.now()).stream()
                .filter(item -> taxonomy.publicAllowed(item.getGameCode(), item.getCategory()))
                .map(this::publicView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogPageServiceView> publicPageItems() {
        return repo.findPublic(Instant.now()).stream()
                .filter(item -> taxonomy.publicAllowed(item.getGameCode(), item.getCategory()))
                .map(this::pageView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogPageServiceView publicPageBySlug(String slug) {
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
        return pageView(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogAdminServiceView> adminList() {
        return repo.findAll().stream()
                .sorted(Comparator.comparingInt(ServiceItem::getSortOrder).thenComparing(ServiceItem::getId))
                .map(this::adminView)
                .toList();
    }

    @Override
    @Transactional
    public CatalogAdminServiceView create(CatalogServiceCommand command, String operator) {
        validatePrice(command);
        if (repo.findBySlug(command.slug()).isPresent()) {
            throw new ConflictException("\u670d\u52a1\u6807\u8bc6\u5df2\u5b58\u5728");
        }

        Instant now = Instant.now();
        ServiceItem.PriceMode priceMode = mode(command.priceMode());
        ServiceItem item = ServiceItem.builder()
                .slug(command.slug())
                .gameCode(validGame(command.gameCode(), command.category()))
                .name(command.name().trim())
                .category(command.category().trim().toUpperCase(Locale.ROOT))
                .shortDesc(command.shortDesc().trim())
                .priceMode(priceMode)
                .priceFixed(priceMode == ServiceItem.PriceMode.FIXED ? command.priceFixed() : null)
                .priceMin(priceMode == ServiceItem.PriceMode.RANGE ? command.priceMin() : null)
                .priceMax(priceMode == ServiceItem.PriceMode.RANGE ? command.priceMax() : null)
                .durationText(command.durationText().trim())
                .status(status(command.status()))
                .coverImage(trimNull(command.coverImage()))
                .hotScore(nvl(command.hotScore()))
                .manualPriority(command.manualPriority() == null ? 0 : command.manualPriority())
                .expiresAt(command.expiresAt())
                .active(command.active() == null || command.active())
                .sortOrder(command.sortOrder() == null ? 0 : command.sortOrder())
                .publishedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        repo.save(item);
        CatalogAdminServiceView after = adminView(item);
        audit.log("SERVICE_CREATE", "SERVICE", item.getId().toString(), operator,
                null, after, command.reason(), currentRequest());
        return after;
    }

    @Override
    @Transactional
    public CatalogAdminServiceView update(Long id, CatalogServiceCommand command, String operator) {
        validatePrice(command);
        ServiceItem item = repo.findById(id).orElseThrow(NotFoundException::new);
        CatalogAdminServiceView before = adminView(item);

        repo.findBySlug(command.slug())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("\u670d\u52a1\u6807\u8bc6\u5df2\u5b58\u5728");
                });

        ServiceItem.PriceMode priceMode = mode(command.priceMode());
        item.setSlug(command.slug());
        item.setGameCode(validGame(command.gameCode(), command.category()));
        item.setName(command.name().trim());
        item.setCategory(command.category().trim().toUpperCase(Locale.ROOT));
        item.setShortDesc(command.shortDesc().trim());
        item.setPriceMode(priceMode);
        item.setPriceFixed(priceMode == ServiceItem.PriceMode.FIXED ? command.priceFixed() : null);
        item.setPriceMin(priceMode == ServiceItem.PriceMode.RANGE ? command.priceMin() : null);
        item.setPriceMax(priceMode == ServiceItem.PriceMode.RANGE ? command.priceMax() : null);
        item.setDurationText(command.durationText().trim());
        item.setStatus(status(command.status()));
        item.setCoverImage(trimNull(command.coverImage()));
        item.setHotScore(nvl(command.hotScore()));
        item.setManualPriority(command.manualPriority() == null ? 0 : command.manualPriority());
        item.setExpiresAt(command.expiresAt());
        item.setActive(command.active() == null || command.active());
        item.setSortOrder(command.sortOrder() == null ? 0 : command.sortOrder());
        item.setUpdatedAt(Instant.now());

        CatalogAdminServiceView after = adminView(item);
        audit.log("SERVICE_UPDATE", "SERVICE", id.toString(), operator,
                before, after, command.reason(), currentRequest());
        return after;
    }

    @Override
    @Transactional
    public void delete(Long id, String operator) {
        ServiceItem item = repo.findById(id).orElseThrow(NotFoundException::new);
        CatalogAdminServiceView before = adminView(item);
        item.setActive(false);
        item.setStatus(ServiceItem.Status.OFFLINE);
        item.setUpdatedAt(Instant.now());
        audit.log("SERVICE_OFFLINE", "SERVICE", id.toString(), operator,
                before, adminView(item), "\u540e\u53f0\u4e0b\u7ebf", currentRequest());
    }

    @Override
    @Transactional
    public CatalogAdminServiceView restore(Long id, String operator) {
        ServiceItem item = repo.findById(id).orElseThrow(NotFoundException::new);
        taxonomy.validate(item.getGameCode(), item.getCategory());
        CatalogAdminServiceView before = adminView(item);
        item.setActive(true);
        if (item.getStatus() == ServiceItem.Status.OFFLINE) {
            item.setStatus(ServiceItem.Status.AVAILABLE);
        }
        item.setUpdatedAt(Instant.now());
        CatalogAdminServiceView after = adminView(item);
        audit.log("SERVICE_RESTORE", "SERVICE", id.toString(), operator,
                before, after, "\u6062\u590d\u670d\u52a1\u9879\u76ee\u4e0a\u67b6", currentRequest());
        return after;
    }

    private void validatePrice(CatalogServiceCommand command) {
        ServiceItem.PriceMode priceMode = mode(command.priceMode());
        if (priceMode == ServiceItem.PriceMode.FIXED && command.priceFixed() == null) {
            throw new BadRequestException("\u56fa\u5b9a\u4ef7\u683c\u6a21\u5f0f\u5fc5\u987b\u586b\u5199\u56fa\u5b9a\u4ef7");
        }
        if (priceMode == ServiceItem.PriceMode.RANGE
                && (command.priceMin() == null
                || command.priceMax() == null
                || command.priceMin().compareTo(command.priceMax()) > 0)) {
            throw new BadRequestException("\u533a\u95f4\u4ef7\u683c\u5fc5\u987b\u586b\u5199\u5408\u6cd5\u7684\u6700\u4f4e\u4ef7\u548c\u6700\u9ad8\u4ef7");
        }
        for (BigDecimal price : Arrays.asList(command.priceFixed(), command.priceMin(), command.priceMax())) {
            if (price != null && price.signum() < 0) {
                throw new BadRequestException("\u4ef7\u683c\u4e0d\u80fd\u4e3a\u8d1f\u6570");
            }
        }
    }

    private CatalogServiceView publicView(ServiceItem item) {
        return new CatalogServiceView(
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

    private CatalogPageServiceView pageView(ServiceItem item) {
        return new CatalogPageServiceView(
                item.getId(),
                item.getSlug(),
                item.getGameCode(),
                item.getName(),
                item.getCategory(),
                item.getShortDesc(),
                CatalogPageServiceView.PriceMode.valueOf(item.getPriceMode().name()),
                item.getPriceFixed(),
                item.getPriceMin(),
                item.getPriceMax(),
                item.getDurationText(),
                CatalogPageServiceView.Status.valueOf(item.getStatus().name()),
                item.getCoverImage());
    }

    private CatalogAdminServiceView adminView(ServiceItem item) {
        return new CatalogAdminServiceView(
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

    private ServiceItem.PriceMode mode(String value) {
        try {
            return ServiceItem.PriceMode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("\u975e\u6cd5\u4ef7\u683c\u6a21\u5f0f");
        }
    }

    private ServiceItem.Status status(String value) {
        try {
            return ServiceItem.Status.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("\u975e\u6cd5\u670d\u52a1\u72b6\u6001");
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest();
        }
        return null;
    }
}
