package com.xinghuo.catalog.web;

import com.xinghuo.catalog.application.CatalogApplicationService;
import com.xinghuo.dto.ApiDtos;
import java.util.List;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/services"})
public class PublicCatalogServiceController {
    private final CatalogApplicationService services;

    @GetMapping
    List<ApiDtos.ServiceResponse> services() {
        return CatalogWebMapper.toResponses(this.services.publicList());
    }

    @Generated
    public PublicCatalogServiceController(CatalogApplicationService services) {
        this.services = services;
    }
}
