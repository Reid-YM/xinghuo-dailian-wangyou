/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.controller.SeoController
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

import com.xinghuo.model.GameCatalog;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.guide.GuideContentService;
import com.xinghuo.service.ServiceCatalogService;
import com.xinghuo.service.TaxonomyService;
import java.time.LocalDate;
import lombok.Generated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeoController {
    private static final String SITE_URL = "https://wydlgf.cn";
    private static final String LAST_MOD = LocalDate.now().toString();
    private final ServiceCatalogService services;
    private final TaxonomyService taxonomy;
    private final GuideContentService guides;

    @GetMapping(value={"/robots.txt"}, produces={"text/plain; charset=UTF-8"})
    ResponseEntity<String> robots() {
        return ResponseEntity.ok("User-agent: *\n"
            + "Allow: /\n"
            + "Disallow: /admin\n"
            + "Disallow: /account\n"
            + "Disallow: /order\n"
            + "Disallow: /record/\n"
            + "Sitemap: " + SITE_URL + "/sitemap.xml\n");
    }

    @GetMapping(value={"/sitemap.xml"}, produces={"application/xml"})
    ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        this.appendUrl(xml, "/", "daily", "1.0");
        this.appendUrl(xml, "/services", "daily", "0.9");
        this.appendUrl(xml, "/guides", "daily", "0.8");
        this.appendUrl(xml, "/contact", "monthly", "0.6");
        this.appendUrl(xml, "/rules", "monthly", "0.5");
        this.appendUrl(xml, "/privacy", "monthly", "0.5");
        this.appendUrl(xml, "/trust", "weekly", "0.7");
        this.appendUrl(xml, "/mastery", "weekly", "0.7");
        for (GameCatalog game : this.taxonomy.publicGames()) {
            if (game.getSeoSlug() == null || game.getSeoSlug().isBlank()) continue;
            this.appendUrl(xml, "/game/" + game.getSeoSlug(), "weekly", "0.8");
        }
        for (ServiceItem service : this.services.publicItems()) {
            this.appendUrl(xml, "/service/" + service.getSlug(), "weekly", "0.8");
        }
        for (var guide : this.guides.publicList(null, null, 200)) {
            this.appendUrl(xml, "/guides/" + guide.slug(), "weekly", "0.7");
        }
        xml.append("</urlset>\n");
        return ResponseEntity.ok(xml.toString());
    }

    private void appendUrl(StringBuilder xml, String path, String changefreq, String priority) {
        xml.append("  <url>\n")
            .append("    <loc>").append(SITE_URL).append(this.escape(path)).append("</loc>\n")
            .append("    <lastmod>").append(LAST_MOD).append("</lastmod>\n")
            .append("    <changefreq>").append(changefreq).append("</changefreq>\n")
            .append("    <priority>").append(priority).append("</priority>\n")
            .append("  </url>\n");
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
