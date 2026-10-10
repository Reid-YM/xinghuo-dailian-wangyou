package com.xinghuo.catalog.infrastructure;

import com.xinghuo.catalog.application.CatalogAdminServiceView;
import com.xinghuo.catalog.application.CatalogLegacyPort;
import com.xinghuo.catalog.application.CatalogPageServiceView;
import com.xinghuo.catalog.application.CatalogServiceCommand;
import com.xinghuo.catalog.application.CatalogServiceView;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.service.ServiceCatalogService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Bridges catalog-owned application contracts to the legacy implementation during migration. */
@Component
public class LegacyCatalogAdapter implements CatalogLegacyPort {
    private final ServiceCatalogService legacy;

    public LegacyCatalogAdapter(ServiceCatalogService legacy) {
        this.legacy = legacy;
    }

    @Override
    public List<CatalogServiceView> publicList() {
        return legacy.publicList().stream().map(this::view).toList();
    }

    @Override
    public List<CatalogPageServiceView> publicPageItems() {
        return legacy.publicItems().stream().map(this::pageView).toList();
    }

    @Override
    public CatalogPageServiceView publicPageBySlug(String slug) {
        return pageView(legacy.publicBySlug(slug));
    }

    @Override
    public List<CatalogAdminServiceView> adminList() {
        return legacy.adminList().stream().map(this::adminView).toList();
    }

    @Override
    public CatalogAdminServiceView create(CatalogServiceCommand command, String operator) {
        return adminView(legacy.create(toLegacy(command), operator, currentRequest()));
    }

    @Override
    public CatalogAdminServiceView update(Long id, CatalogServiceCommand command, String operator) {
        return adminView(legacy.update(id, toLegacy(command), operator, currentRequest()));
    }

    @Override
    public void delete(Long id, String operator) {
        legacy.delete(id, operator, currentRequest());
    }

    @Override
    public CatalogAdminServiceView restore(Long id, String operator) {
        return adminView(legacy.restore(id, operator, currentRequest()));
    }

    private ApiDtos.ServiceUpsert toLegacy(CatalogServiceCommand source) {
        return new ApiDtos.ServiceUpsert(
                source.gameCode(),
                source.slug(),
                source.name(),
                source.category(),
                source.shortDesc(),
                source.priceMode(),
                source.priceFixed(),
                source.priceMin(),
                source.priceMax(),
                source.durationText(),
                source.status(),
                source.coverImage(),
                source.hotScore(),
                source.manualPriority(),
                source.expiresAt(),
                source.active(),
                source.sortOrder(),
                source.reason());
    }

    private CatalogServiceView view(ApiDtos.ServiceResponse source) {
        return new CatalogServiceView(
                source.id(),
                source.slug(),
                source.gameCode(),
                source.name(),
                source.category(),
                source.shortDesc(),
                source.priceMode(),
                source.priceFixed(),
                source.priceMin(),
                source.priceMax(),
                source.durationText(),
                source.status(),
                source.coverImage());
    }

    private CatalogPageServiceView pageView(ServiceItem source) {
        return new CatalogPageServiceView(
                source.getId(),
                source.getSlug(),
                source.getGameCode(),
                source.getName(),
                source.getCategory(),
                source.getShortDesc(),
                CatalogPageServiceView.PriceMode.valueOf(source.getPriceMode().name()),
                source.getPriceFixed(),
                source.getPriceMin(),
                source.getPriceMax(),
                source.getDurationText(),
                CatalogPageServiceView.Status.valueOf(source.getStatus().name()),
                source.getCoverImage());
    }

    private CatalogAdminServiceView adminView(ApiDtos.AdminServiceResponse source) {
        return new CatalogAdminServiceView(
                source.id(),
                source.slug(),
                source.gameCode(),
                source.name(),
                source.category(),
                source.shortDesc(),
                source.priceMode(),
                source.priceFixed(),
                source.priceMin(),
                source.priceMax(),
                source.durationText(),
                source.status(),
                source.coverImage(),
                source.hotScore(),
                source.manualPriority(),
                source.expiresAt(),
                source.active(),
                source.sortOrder(),
                source.version());
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest();
        }
        return null;
    }
}
