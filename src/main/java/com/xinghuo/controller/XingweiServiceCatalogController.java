package com.xinghuo.controller;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.service.ServiceCatalogService;
import com.xinghuo.service.TaxonomyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

/** V3B：复用现有服务目录真源，提供查看与受控基础管理。 */
@RestController
@RequestMapping("/api/admin/xingwei/catalog")
public class XingweiServiceCatalogController {
    private final ServiceCatalogService services;
    private final TaxonomyService taxonomy;

    public XingweiServiceCatalogController(ServiceCatalogService services, TaxonomyService taxonomy) {
        this.services = services;
        this.taxonomy = taxonomy;
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
        result.put("writeScope", "EXISTING_SERVICE_UPDATE_ONLY");
        result.put("counts", counts);
        result.put("services", serviceRows);
        result.put("games", catalog.games());
        result.put("categories", catalog.categories());
        result.put("platforms", catalog.platforms());
        result.put("gamePlatforms", catalog.gamePlatforms());
        return result;
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
        return services.restore(id, authentication.getName(), httpRequest);
    }
}
