package com.xinghuo.controller;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.service.ServiceCatalogService;
import com.xinghuo.service.TaxonomyService;
import com.xinghuo.service.XingweiServicePublishReviewService;
import com.xinghuo.service.XingweiTaxonomyControlService;
import com.xinghuo.service.XingweiCatalogRevisionService;
import com.xinghuo.service.XingweiCatalogRevisionService.Kind;
import com.xinghuo.service.XingweiCatalogDetailsService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** V3I：服务目录真源 + 发布检查 + taxonomy 可见性预览、快照确认与受控启停。 */
@RestController
@RequestMapping("/api/admin/xingwei/catalog")
public class XingweiServiceCatalogController {
    @org.springframework.web.bind.annotation.ModelAttribute void privateResponse(jakarta.servlet.http.HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    private final ServiceCatalogService services;
    private final TaxonomyService taxonomy;
    private final XingweiServicePublishReviewService publishReview;
    private final XingweiTaxonomyControlService taxonomyControl;
    private final XingweiCatalogRevisionService revisions;
    private final XingweiCatalogDetailsService details;

    public XingweiServiceCatalogController(
            ServiceCatalogService services,
            TaxonomyService taxonomy,
            XingweiServicePublishReviewService publishReview,
            XingweiTaxonomyControlService taxonomyControl,XingweiCatalogRevisionService revisions,XingweiCatalogDetailsService details) {
        this.services = services;
        this.taxonomy = taxonomy;
        this.publishReview = publishReview;
        this.taxonomyControl = taxonomyControl;
        this.revisions=revisions;this.details=details;
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
        result.put("writeScope", "SERVICE_MANAGE_WITH_PUBLISH_PREFLIGHT_AND_TAXONOMY_VISIBILITY_CONFIRM");
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
        return revisions.serialized(Kind.GAME, taxonomy.normalizeGame(code), () -> taxonomyControl.setGameActive(
                code,
                request.active(),
                request.reason(),
                request.impactToken(),
                authentication.getName(),
                httpRequest));
    }

    @PutMapping("/taxonomy/categories/{id}/active")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:manage')")
    public ApiDtos.CategoryOptionResponse setCategoryActive(
            @PathVariable Long id,
            @Valid @RequestBody TaxonomyToggleRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return revisions.serialized(XingweiCatalogRevisionService.Kind.CATEGORY, id, () -> taxonomyControl.setCategoryActive(
                id,
                request.active(),
                request.reason(),
                request.impactToken(),
                authentication.getName(),
                httpRequest));
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

    // V3F: profile-only edits preserve active state, game/category codes and parent links.
    @GetMapping("/taxonomy/games/{code}/editor") @PreAuthorize("@xingweiPermissionGuard.has(authentication,'service:view')")
    public Object gameEditor(@PathVariable String code){return details.game(code);}
    @GetMapping("/taxonomy/categories/{id}/editor") @PreAuthorize("@xingweiPermissionGuard.has(authentication,'service:view')")
    public Object categoryEditor(@PathVariable long id){return details.category(id);}
    @PutMapping("/taxonomy/games/{code}/profile")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:manage')")
    public ApiDtos.GameOptionResponse updateGameProfile(
            @PathVariable String code, @Valid @RequestBody ApiDtos.CatalogUpdateRequest request,
            @RequestHeader(value="If-Match",required=false) String expected,Authentication authentication, HttpServletRequest httpRequest) {
        return revisions.change(Kind.GAME,taxonomy.normalizeGame(code),expected,()->taxonomy.updateGame(code, new ApiDtos.CatalogUpdateRequest(
                request.name(), request.iconKey(), request.description(), request.sortOrder(),
                null, request.reason()), authentication.getName(), httpRequest));
    }

    @PutMapping("/taxonomy/categories/{id}/profile")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:manage')")
    public ApiDtos.CategoryOptionResponse updateCategoryProfile(
            @PathVariable Long id, @Valid @RequestBody ApiDtos.CategoryUpdateRequest request,
            @RequestHeader(value="If-Match",required=false) String expected,Authentication authentication, HttpServletRequest httpRequest) {
        return revisions.change(Kind.CATEGORY,id,expected,()->taxonomy.updateCategory(id, new ApiDtos.CategoryUpdateRequest(
                request.name(), request.iconKey(), request.description(), request.sortOrder(),
                null, request.reason()), authentication.getName(), httpRequest));
    }

    @PostMapping("/services")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:manage')")
    public ApiDtos.AdminServiceResponse createService(
            @Valid @RequestBody ApiDtos.ServiceUpsert request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        var draft=new ApiDtos.ServiceUpsert(request.gameCode(),request.slug(),request.name(),request.category(),request.shortDesc(),request.priceMode(),request.priceFixed(),request.priceMin(),request.priceMax(),request.durationText(),"OFFLINE",request.coverImage(),request.hotScore(),request.manualPriority(),request.expiresAt(),false,request.sortOrder(),request.reason());
        return services.create(draft, authentication.getName(), httpRequest);
    }

    @PutMapping("/services/{id}")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'service:manage')")
    public ApiDtos.AdminServiceResponse updateService(
            @PathVariable Long id,
            @Valid @RequestBody ApiDtos.ServiceUpsert request,
            @RequestHeader(value="If-Match",required=false) String expected,Authentication authentication,
            HttpServletRequest httpRequest) {
        return revisions.change(Kind.SERVICE,id,expected,()->{
            var current=services.adminList().stream().filter(item->item.id().equals(id)).findFirst().orElseThrow(com.xinghuo.exception.NotFoundException::new);
            // The basic editor owns pricing/copy/order only. Keep full-profile fields and
            // visibility from the locked current record, rather than trusting stale copies.
            var safe=new ApiDtos.ServiceUpsert(current.gameCode(),current.slug(),request.name(),current.category(),request.shortDesc(),request.priceMode(),request.priceFixed(),request.priceMin(),request.priceMax(),request.durationText(),current.status(),current.coverImage(),current.hotScore(),request.manualPriority(),current.expiresAt(),current.active(),request.sortOrder(),request.reason());
            return services.update(id,safe,authentication.getName(),httpRequest);
        });
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
            @NotBlank @Size(max = 500) String reason,
            @NotBlank @Size(max = 128) String impactToken) {
    }
}
