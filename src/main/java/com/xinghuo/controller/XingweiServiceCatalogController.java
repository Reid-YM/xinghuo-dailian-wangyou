package com.xinghuo.controller;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.service.ServiceCatalogService;
import com.xinghuo.service.TaxonomyService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** V3A：服务项目与游戏资料只读聚合 API。 */
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
        result.put("readOnly", true);
        result.put("source", "EXISTING_SERVICE_CATALOG");
        result.put("counts", counts);
        result.put("services", serviceRows);
        result.put("games", catalog.games());
        result.put("categories", catalog.categories());
        result.put("platforms", catalog.platforms());
        result.put("gamePlatforms", catalog.gamePlatforms());
        return result;
    }
}
