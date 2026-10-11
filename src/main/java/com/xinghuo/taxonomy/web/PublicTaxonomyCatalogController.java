package com.xinghuo.taxonomy.web;

import com.xinghuo.taxonomy.application.TaxonomyApplicationService;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/catalog"})
public class PublicTaxonomyCatalogController {
    private final TaxonomyApplicationService taxonomy;

    @GetMapping
    CatalogResponse catalog() {
        return TaxonomyWebMapper.toResponse(this.taxonomy.catalog(false));
    }

    @Generated
    public PublicTaxonomyCatalogController(TaxonomyApplicationService taxonomy) {
        this.taxonomy = taxonomy;
    }
}
