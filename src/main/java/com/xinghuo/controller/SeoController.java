package com.xinghuo.controller;

import com.xinghuo.model.GameCatalog;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.guide.GuideContentService;
import com.xinghuo.service.ServiceCatalogService;
import com.xinghuo.service.TaxonomyService;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeoController {
    private static final String SITE_URL = "https://wydlgf.cn";
    private final ServiceCatalogService services;
    private final TaxonomyService taxonomy;
    private final GuideContentService guides;

    @GetMapping(value={"/robots.txt"}, produces={"text/plain; charset=UTF-8"})
    ResponseEntity<String> robots() {
        // 动态 Controller 是 robots.txt 的唯一权威来源。
        // 公开展示页一律 Allow；后台、账号、API 与业务明细不参与抓取。
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
        List<GameCatalog> games = this.taxonomy.publicGames();
        List<ServiceItem> serviceItems = this.services.publicItems();
        var guideItems = this.guides.publicList(null, null, 200);

        Instant catalogLastModified = null;
        for (GameCatalog game : games) catalogLastModified = latest(catalogLastModified, game.getUpdatedAt());
        for (ServiceItem service : serviceItems) catalogLastModified = latest(catalogLastModified, service.getUpdatedAt());

        Instant guideLastModified = null;
        for (var guide : guideItems) guideLastModified = latest(guideLastModified, guide.updatedAt());
        Instant siteLastModified = latest(catalogLastModified, guideLastModified);

        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        this.appendUrl(xml, "/", siteLastModified, "daily", "1.0");
        this.appendUrl(xml, "/services", catalogLastModified, "daily", "0.9");
        this.appendUrl(xml, "/guides", guideLastModified, "daily", "0.8");
        this.appendUrl(xml, "/contact", null, "monthly", "0.6");
        this.appendUrl(xml, "/rules", null, "monthly", "0.5");
        this.appendUrl(xml, "/rules-en", null, "monthly", "0.4");
        this.appendUrl(xml, "/privacy", null, "monthly", "0.5");
        this.appendUrl(xml, "/privacy-en", null, "monthly", "0.4");
        this.appendUrl(xml, "/trust", null, "weekly", "0.7");
        this.appendUrl(xml, "/mastery", null, "weekly", "0.7");
        this.appendUrl(xml, "/quote", null, "daily", "0.7");
        for (GameCatalog game : games) {
            if (game.getSeoSlug() == null || game.getSeoSlug().isBlank()) continue;
            this.appendUrl(xml, "/game/" + game.getSeoSlug(), game.getUpdatedAt(), "weekly", "0.8");
        }
        for (ServiceItem service : serviceItems) {
            this.appendUrl(xml, "/service/" + service.getSlug(), service.getUpdatedAt(), "weekly", "0.8");
        }
        for (var guide : guideItems) {
            this.appendUrl(xml, "/guides/" + guide.slug(), guide.updatedAt(), "weekly", "0.7");
        }
        xml.append("</urlset>\n");
        return ResponseEntity.ok(xml.toString());
    }

    private void appendUrl(StringBuilder xml, String path, Instant lastModified, String changefreq, String priority) {
        xml.append("  <url>\n")
            .append("    <loc>").append(SITE_URL).append(this.escape(path)).append("</loc>\n");
        if (lastModified != null) {
            xml.append("    <lastmod>")
                .append(DateTimeFormatter.ISO_INSTANT.format(lastModified))
                .append("</lastmod>\n");
        }
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n")
            .append("    <priority>").append(priority).append("</priority>\n")
            .append("  </url>\n");
    }

    private Instant latest(Instant current, Instant candidate) {
        if (candidate == null) return current;
        if (current == null || candidate.isAfter(current)) return candidate;
        return current;
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public SeoController(ServiceCatalogService services, TaxonomyService taxonomy, GuideContentService guides) {
        this.services = services;
        this.taxonomy = taxonomy;
        this.guides = guides;
    }
}
