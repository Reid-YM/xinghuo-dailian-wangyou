package com.xinghuo.controller;

import com.xinghuo.catalog.application.CatalogApplicationService;
import com.xinghuo.catalog.application.CatalogPageServiceView;
import com.xinghuo.guide.GuideContentService;
import com.xinghuo.taxonomy.application.TaxonomyApplicationService;
import com.xinghuo.taxonomy.application.TaxonomyCatalogView;
import java.time.LocalDate;
import lombok.Generated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeoController {
    private static final String SITE_URL = "https://wydlgf.cn";
    private static final String LAST_MOD = LocalDate.now().toString();
    private final CatalogApplicationService services;
    private final TaxonomyApplicationService taxonomy;
    private final GuideContentService guides;

    @GetMapping(value={"/robots.txt"}, produces={"text/plain; charset=UTF-8"})
    ResponseEntity<String> robots() {
        // SEO-g：与明细页自带的 noindex 口径统一 —— 公开展示页一律 Allow，
        // 只屏蔽后台/账号/API 与业务明细；明细页自身的 noindex 是主防护，
        // robots 的 Disallow 作为第二道，两者互补不打架。
        return ResponseEntity.ok("User-agent: *\n"
            + "Allow: /\n"
            + "Disallow: /admin\n"
            + "Disallow: /adm/*\n"
            + "Disallow: /api/\n"
            + "Disallow: /account\n"
            + "Disallow: /order/\n"
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
        // SEO-h：补齐可索引漏斗页。
        this.appendUrl(xml, "/quote", "daily", "0.7");
        for (TaxonomyCatalogView.GameView game : this.taxonomy.publicGames()) {
            if (game.getSeoSlug() == null || game.getSeoSlug().isBlank()) continue;
            this.appendUrl(xml, "/game/" + game.getSeoSlug(), "weekly", "0.8");
        }
        for (CatalogPageServiceView service : this.services.publicPageItems()) {
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
    public SeoController(CatalogApplicationService services, TaxonomyApplicationService taxonomy, GuideContentService guides) {
        this.services = services;
        this.taxonomy = taxonomy;
        this.guides = guides;
    }
}
