package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xinghuo.guide.GuideModels;
import com.xinghuo.model.ServiceItem;
import jakarta.servlet.RequestDispatcher;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class PublicCatalogFallbackTest {

    @Test
    void canonicalServicePageIncludesOnlyPublicItemsAndEscapesUntrustedDescriptions() throws Exception {
        ServiceItem publicItem = service("mw4-camo", "<img src=x onerror=alert(1)>",
                "武器迷彩 </noscript><script>alert(1)</script>", true, ServiceItem.Status.AVAILABLE);
        ServiceItem hidden = service("private-service", "未上架服务", "仅内部", false,
                ServiceItem.Status.OFFLINE);

        var request = canonical("/services", Map.of("hallServices", List.of(publicItem, hidden)));
        String html = new FrontendNextPreviewController().frontendNext(request).getBody();

        assertTrue(html.contains("<h2>公开服务目录</h2>"));
        assertTrue(html.contains("<section id=\"n23-services-ssr\""));
        assertTrue(html.contains("<section class=\"page\" style=\"display:none\" data-page=\"services\""));
        int ssrStart = html.indexOf("<section id=\"n23-services-ssr\"");
        int interactivePageStart = html.indexOf("<section class=\"page\" style=\"display:none\" data-page=\"services\"", ssrStart);
        assertTrue(ssrStart >= 0 && interactivePageStart > ssrStart);
        String visibleFallback = html.substring(ssrStart, interactivePageStart);
        assertTrue(visibleFallback.contains("href=\"/service/mw4-camo\""));
        assertFalse(visibleFallback.contains("private-service"));
        assertTrue(html.contains("href=\"/service/mw4-camo\""));
        assertTrue(html.contains("&lt;img src=x onerror=alert(1)&gt;"));
        assertTrue(html.contains("&lt;/noscript&gt;&lt;script&gt;"));
        assertFalse(html.contains("<img src=x onerror=alert(1)>"));
        assertFalse(html.contains("</noscript><script>alert(1)</script>"));
        assertFalse(html.contains("private-service"));
    }

    @Test
    void canonicalGuidePageLinksOnlyPublishedGuides() throws Exception {
        GuideModels.GuideSummary published = guide("camo-roadmap", "迷彩路线", "PUBLISHED",
                Instant.parse("2026-01-01T00:00:00Z"));
        GuideModels.GuideSummary draft = guide("hidden-roadmap", "草稿", "DRAFT",
                Instant.parse("2026-01-01T00:00:00Z"));
        var request = canonical("/guides", Map.of("guides", List.of(published, draft)));
        String html = new FrontendNextPreviewController().frontendNext(request).getBody();

        assertTrue(html.contains("<h2>已发布攻略</h2>"));
        assertTrue(html.contains("<section id=\"n23-guides-ssr\""));
        assertTrue(html.contains("href=\"/guides/camo-roadmap\""));
        assertTrue(html.contains("迷彩路线"));
        assertFalse(html.contains("hidden-roadmap"));
        assertFalse(html.contains("草稿"));
        int ssrStart = html.indexOf("<section id=\"n23-guides-ssr\"");
        int ssrEnd = html.indexOf("</section>", ssrStart);
        int interactivePageStart = html.indexOf("<section class=\"page\"", ssrStart);
        assertTrue(ssrStart >= 0 && ssrEnd > ssrStart);
        assertTrue(interactivePageStart > ssrEnd, "SSR block must precede the interactive shell");
        String visibleFallback = html.substring(ssrStart, ssrEnd);
        assertTrue(visibleFallback.contains("迷彩路线"));
        assertFalse(visibleFallback.contains("草稿"));
        assertFalse(visibleFallback.contains("<noscript"), "SSR must be visible without JavaScript");
    }

    @Test
    void canonicalDetailsUseOnlyTheExactPublicModelAndPrivateRouteNeverIncludesIt() throws Exception {
        ServiceItem publicItem = service("mw4-camo", "迷彩服务", "按目标询价",
                true, ServiceItem.Status.AVAILABLE);
        String detail = new FrontendNextPreviewController()
                .frontendNext(canonical("/service/mw4-camo", Map.of("service", publicItem)))
                .getBody();
        assertTrue(detail.contains("href=\"/quote?service=mw4-camo\""));
        assertTrue(detail.contains("按目标询价"));

        String mismatched = new FrontendNextPreviewController()
                .frontendNext(canonical("/service/other", Map.of("service", publicItem))).getBody();
        assertFalse(mismatched.contains("按目标询价"));

        String privatePage = new FrontendNextPreviewController()
                .frontendNext(canonical("/account", Map.of("service", publicItem,
                        "hallServices", List.of(publicItem)))).getBody();
        assertFalse(privatePage.contains("href=\"/service/mw4-camo\""));
        assertFalse(privatePage.contains("按目标询价"));
    }

    @Test
    void publishedGuideDetailIncludesSafeSectionText() throws Exception {
        GuideModels.GuideSummary guide = guide("camo-roadmap", "迷彩路线", "PUBLISHED",
                Instant.parse("2026-01-01T00:00:00Z"));
        GuideModels.GuideSection section = new GuideModels.GuideSection(1L, "step1",
                "第一步", "准备武器等级 <script>bad()</script>", null, null, 0);
        String html = new FrontendNextPreviewController().frontendNext(
                canonical("/guides/camo-roadmap",
                        Map.of("guide", guide, "sections", List.of(section)))).getBody();

        assertTrue(html.contains("<h2>迷彩路线</h2>"));
        assertTrue(html.contains("<h3>第一步</h3>"));
        assertTrue(html.contains("&lt;script&gt;bad()&lt;/script&gt;"));
        assertFalse(html.contains("<script>bad()</script>"));
    }

    private static MockHttpServletRequest canonical(String path, Map<String, Object> model) {
        var request = new MockHttpServletRequest("GET", "/next" + path);
        request.setAttribute(RequestDispatcher.FORWARD_REQUEST_URI, path);
        request.setAttribute("frontendOriginalMetadata", model);
        return request;
    }

    private static ServiceItem service(String slug, String name, String description,
                                       boolean active, ServiceItem.Status status) {
        return ServiceItem.builder().slug(slug).gameCode("MW4").name(name)
                .shortDesc(description).category("WEAPON").status(status)
                .active(active).durationText("1–3 天").build();
    }

    private static GuideModels.GuideSummary guide(String slug, String title, String status,
                                                   Instant publishedAt) {
        return new GuideModels.GuideSummary(1L, "MW4", "MW4", slug, title, "",
                "游戏攻略简介", null, "BEGINNER", 12, "zh-CN", "星火", "", "",
                status, true, 0, List.of(), null, null, null, 0L,
                publishedAt, publishedAt, 1, 0);
    }
}
