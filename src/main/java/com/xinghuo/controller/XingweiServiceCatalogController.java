package com.xinghuo.controller;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.service.ServiceCatalogService;
import com.xinghuo.service.TaxonomyService;
import com.xinghuo.service.XingweiServicePublishReviewService;
import com.xinghuo.service.XingweiTaxonomyControlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** V3E：服务目录真源 + 发布检查 + taxonomy 影响预览与受控启停。 */
@RestController
@RequestMapping("/api/admin/xingwei/catalog")
public class XingweiServiceCatalogController {
    private final ServiceCatalogService services;
    private final TaxonomyService taxonomy;
    private final XingweiServicePublishReviewService publishReview;
    private final XingweiTaxonomyControlService taxonomyControl;

    public XingweiServiceCatalogController(
            ServiceCatalogService services,
            TaxonomyService taxonomy,
            XingweiServicePublishReviewService publishReview,
            XingweiTaxonomyControlService taxonomyControl) {
        this.services = services;
        this.taxonomy = taxonomy;
        this.publishReview = publishReview;
        this.taxonomyControl = taxonomyControl;
    }

    @GetMapping("/snapshot")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:view')")
    public Map<String, Object> snapshot(Authentication authentication) {
        List<ApiDtos.AdminServiceResponse> serviceRows = services.adminList();
        ApiDtos.CatalogResponse catalog = taxonomy.catalog(true);

        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("services", serviceRows.size());
        counts.put("activeServices", serviceRows.stream().filter(ApiDtos.AdminServiceResponse::active).count());
        counts.put("games", catalog.games().size());
        counts.put("categories", catalog.categories().size());
        counts.put("platforms", catalog.platforms().size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", "EXISTING_SERVICE_CATALOG");
        result.put("writeScope", "SERVICE_MANAGE_WITH_PUBLISH_PREFLIGHT_AND_TAXONOMY_TOGGLE_PREVIEW");
        result.put("counts", counts);
        result.put("services", serviceRows);
        result.put("games", catalog.games());
        result.put("categories", catalog.categories());
        result.put("platforms", catalog.platforms());
        result.put("gamePlatforms", catalog.gamePlatforms());
        return result;
    }

    @GetMapping("/services/{id}/publish-review")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:view')")
    public XingweiServicePublishReviewService.PublishReview publishReview(
            @PathVariable Long id,
            Authentication authentication) {
        return publishReview.review(id);
    }

    @GetMapping("/taxonomy/games/{code}/impact")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:view')")
    public XingweiTaxonomyControlService.TaxonomyImpact gameImpact(
            @PathVariable String code,
            Authentication authentication) {
        return taxonomyControl.gameImpact(code);
    }

    @GetMapping("/taxonomy/categories/{id}/impact")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:view')")
    public XingweiTaxonomyControlService.TaxonomyImpact categoryImpact(
            @PathVariable Long id,
            Authentication authentication) {
        return taxonomyControl.categoryImpact(id);
    }

    @PutMapping("/taxonomy/games/{code}/active")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:manage')")
    public ApiDtos.GameOptionResponse setGameActive(
            @PathVariable String code,
            @Valid @RequestBody TaxonomyToggleRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return taxonomyControl.setGameActive(
                code,
                request.active(),
                request.reason(),
                authentication.getName(),
                httpRequest);
    }

    @PutMapping("/taxonomy/categories/{id}/active")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:manage')")
    public ApiDtos.CategoryOptionResponse setCategoryActive(
            @PathVariable Long id,
            @Valid @RequestBody TaxonomyToggleRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return taxonomyControl.setCategoryActive(
                id,
                request.active(),
                request.reason(),
                authentication.getName(),
                httpRequest);
    }

    // V3F: controlled creation. New taxonomy is always offline until separately enabled.
    @PostMapping("/taxonomy/games")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:manage')")
    public ApiDtos.GameOptionResponse createGame(
            @Valid @RequestBody ApiDtos.CatalogNameRequest request,
            Authentication authentication, HttpServletRequest httpRequest) {
        return taxonomy.addGame(new ApiDtos.CatalogNameRequest(
                request.name(), request.iconKey(), request.description(), request.sortOrder(),
                false, request.reason()), authentication.getName(), httpRequest);
    }

    @PostMapping("/taxonomy/categories")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:manage')")
    public ApiDtos.CategoryOptionResponse createCategory(
            @Valid @RequestBody ApiDtos.CategoryCreateRequest request,
            Authentication authentication, HttpServletRequest httpRequest) {
        return taxonomy.addCategory(new ApiDtos.CategoryCreateRequest(
                request.gameCode(), request.name(), request.iconKey(), request.description(),
                request.sortOrder(), false, request.reason()), authentication.getName(), httpRequest);
    }

    @PostMapping("/services")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:manage')")
    public ApiDtos.AdminServiceResponse createService(
            @Valid @RequestBody ApiDtos.ServiceUpsert request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return services.create(request, authentication.getName(), httpRequest);
    }

    @PutMapping("/services/{id}")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:manage')")
    public ApiDtos.AdminServiceResponse updateService(
            @PathVariable Long id,
            @Valid @RequestBody ApiDtos.ServiceUpsert request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return services.update(id, request, authentication.getName(), httpRequest);
    }

    @DeleteMapping("/services/{id}")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:manage')")
    public ResponseEntity<Void> offlineService(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        services.delete(id, authentication.getName(), httpRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/services/{id}/restore")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:manage')")
    public ApiDtos.AdminServiceResponse restoreService(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        publishReview.requireReady(id);
        return services.restore(id, authentication.getName(), httpRequest);
    }

    public record TaxonomyToggleRequest(
            @NotNull Boolean active,
            @NotBlank @Size(max = 500) String reason) {
    }
}
