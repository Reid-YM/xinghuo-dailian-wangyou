/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.controller.SeoController
 *  com.xinghuo.guide.GuideContentService
 *  com.xinghuo.guide.GuideModels$GuideSummary
 *  com.xinghuo.model.GameCatalog
 *  com.xinghuo.model.ServiceItem
 *  com.xinghuo.service.ServiceCatalogService
 *  com.xinghuo.service.TaxonomyService
 *  lombok.Generated
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.xinghuo.controller;

import com.xinghuo.guide.GuideContentService;
import com.xinghuo.guide.GuideModels;
import com.xinghuo.model.GameCatalog;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.service.ServiceCatalogService;
import com.xinghuo.service.TaxonomyService;
import lombok.Generated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeoController {
    private final ServiceCatalogService services;
    private final TaxonomyService taxonomy;
    private final GuideContentService guides;

    @GetMapping(value={"/sitemap.xml"}, produces={"application/xml"})
    ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String path : new String[]{"/", "/services", "/guides", "/quote", "/records", "/contact", "/rules", "/privacy", "/trust", "/mastery"}) {
            this.appendUrl(xml, path);
        }
        for (GameCatalog game : this.taxonomy.publicGames()) {
            if (game.getSeoSlug() == null || game.getSeoSlug().isBlank()) continue;
            this.appendUrl(xml, "/game/" + game.getSeoSlug());
        }
        for (ServiceItem service : this.services.publicItems()) {
            this.appendUrl(xml, "/service/" + service.getSlug());
        }
        for (GuideModels.GuideSummary guide : this.guides.publicList(null, null, 200)) {
            this.appendUrl(xml, "/guides/" + guide.slug());
        }
        xml.append("</urlset>\n");
        return ResponseEntity.ok((Object)xml.toString());
    }

    private void appendUrl(StringBuilder xml, String path) {
        xml.append("  <url><loc>https://wydlgf.cn").append(this.escape(path)).append("</loc></url>\n");
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Generated
    public SeoController(ServiceCatalogService services, TaxonomyService taxonomy, GuideContentService guides) {
        this.services = services;
        this.taxonomy = taxonomy;
        this.guides = guides;
    }
}

