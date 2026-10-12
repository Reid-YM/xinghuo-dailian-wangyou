package com.xinghuo.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xinghuo.guide.GuideContentService;
import com.xinghuo.model.GameCatalog;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.service.ServiceCatalogService;
import com.xinghuo.service.TaxonomyService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SeoControllerTest {

    @Test
    void sitemapUsesRealEntityUpdatesAndOmitsFabricatedStaticLastmod() {
        ServiceCatalogService services = mock(ServiceCatalogService.class);
        TaxonomyService taxonomy = mock(TaxonomyService.class);
        GuideContentService guides = mock(GuideContentService.class);

        Instant gameUpdated = Instant.parse("2026-09-20T10:15:30Z");
        Instant serviceUpdated = Instant.parse("2026-10-01T09:00:00Z");
        GameCatalog game = GameCatalog.builder()
                .code("CODMW4")
                .name("使命召唤 MW4")
                .seoSlug("cod-mw4")
                .updatedAt(gameUpdated)
                .build();
        ServiceItem service = ServiceItem.builder()
                .slug("weapon-leveling")
                .gameCode("CODMW4")
                .name("武器升级")
                .updatedAt(serviceUpdated)
                .build();

        when(taxonomy.publicGames()).thenReturn(List.of(game));
        when(services.publicItems()).thenReturn(List.of(service));
        when(guides.publicList(null, null, 200)).thenReturn(List.of());

        SeoController controller = new SeoController(services, taxonomy, guides);
        String xml = controller.sitemap().getBody();

        assertTrue(urlBlock(xml, "/game/cod-mw4").contains("<lastmod>2026-09-20T10:15:30Z</lastmod>"));
        assertTrue(urlBlock(xml, "/service/weapon-leveling").contains("<lastmod>2026-10-01T09:00:00Z</lastmod>"));
        assertTrue(urlBlock(xml, "/services").contains("<lastmod>2026-10-01T09:00:00Z</lastmod>"));
        assertTrue(urlBlock(xml, "/").contains("<lastmod>2026-10-01T09:00:00Z</lastmod>"));
        assertFalse(urlBlock(xml, "/contact").contains("<lastmod>"));
        assertTrue(urlBlock(xml, "/rules-en").contains("<changefreq>monthly</changefreq>"));
        assertTrue(urlBlock(xml, "/privacy-en").contains("<changefreq>monthly</changefreq>"));
    }

    @Test
    void robotsControllerIsTheOnlyAuthority() {
        ServiceCatalogService services = mock(ServiceCatalogService.class);
        TaxonomyService taxonomy = mock(TaxonomyService.class);
        GuideContentService guides = mock(GuideContentService.class);
        SeoController controller = new SeoController(services, taxonomy, guides);

        String robots = controller.robots().getBody();
        assertTrue(robots.contains("Disallow: /api/"));
        assertTrue(robots.contains("Disallow: /account"));
        assertTrue(robots.contains("Disallow: /record/"));
        assertTrue(robots.contains("Sitemap: https://wydlgf.cn/sitemap.xml"));
        assertFalse(Files.exists(Path.of("src/main/resources/static/robots.txt")));
    }

    private String urlBlock(String xml, String path) {
        String loc = "<loc>https://wydlgf.cn" + path + "</loc>";
        int start = xml.indexOf(loc);
        assertTrue(start >= 0, "missing sitemap URL: " + path);
        int end = xml.indexOf("</url>", start);
        assertTrue(end > start, "unterminated sitemap URL block: " + path);
        return xml.substring(start, end);
    }
}
