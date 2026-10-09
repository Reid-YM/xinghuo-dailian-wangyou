package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xinghuo.config.PublicFrontendAssetFilter;
import jakarta.servlet.FilterChain;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PublicNovaContractTest {
    private static final Path ROOT = Path.of("src/main/resources");

    @Test
    void desktopAndMobileHomeShareNovaCompositionAndBusinessAnchors() throws Exception {
        String desktop = Files.readString(ROOT.resolve("templates/index.html"));
        String mobile = Files.readString(ROOT.resolve("templates/mobile/index.html"));
        String home = Files.readString(ROOT.resolve("templates/fragments/home-nova.html"));

        assertTrue(desktop.contains("fragments/home-nova :: content"));
        assertTrue(mobile.contains("fragments/home-nova :: content"));
        assertTrue(desktop.contains("data-public-ui=\"nova\""));
        assertTrue(mobile.contains("data-public-ui=\"nova\""));
        assertFalse(desktop.contains("app.css"));
        assertFalse(mobile.contains("app.css"));
        assertFalse(mobile.contains("mobile-home-v2.css"));

        for (String anchor : new String[]{
                "id=\"homeHero\"", "id=\"heroBackground\"", "id=\"heroVisual\"",
                "id=\"trustTags\"", "id=\"quick-actions\"", "id=\"quoteForm\"",
                "id=\"homeGameRail\"", "id=\"hotServices\"", "id=\"homeContactList\"",
                "id=\"publicCaseGrid\"", "data-primary-cta", "data-link-game",
                "data-link-platform", "data-link-category", "data-link-service",
                "name=\"privacyConsent\""
        }) {
            assertTrue(home.contains(anchor), () -> "missing runtime/business anchor: " + anchor);
        }
        assertTrue(home.contains("class=\"nv-home-hero\""));
        assertTrue(home.contains("class=\"nv-mobile-dock\""));
        assertFalse(home.contains("mh2-"));
        assertFalse(home.contains("xh-v4"));
    }

    @Test
    void publicFilterRemovesLegacyVisualAssetsAndInjectsNova() throws Exception {
        var filter = new PublicFrontendAssetFilter();
        FilterChain html = (request, response) -> {
            var http = (jakarta.servlet.http.HttpServletResponse) response;
            http.setContentType("text/html;charset=UTF-8");
            http.getWriter().write("<!doctype html><html><head>"
                    + "<link rel=\"stylesheet\" href=\"/assets/app.css?v=old\">"
                    + "<link rel=\"stylesheet\" href=\"/assets/account.css?v=old\">"
                    + "<script defer src=\"/assets/site-shell.js?v=old\"></script>"
                    + "<script defer src=\"/assets/public.js?v=keep\"></script>"
                    + "</head><body>ok</body></html>");
        };
        var request = new MockHttpServletRequest("GET", "/account");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, html);
        String out = response.getContentAsString();
        assertTrue(out.contains("name=\"xh-public-ui\" content=\"nova\""));
        assertTrue(out.contains("nova-ui.css?v=20261009nova01"));
        assertTrue(out.contains("nova-shell.js?v=20261009nova01"));
        assertTrue(out.contains("data-public-ui=\"nova\""));
        assertTrue(out.contains("public.js?v=keep"));
        assertFalse(out.contains("app.css?v=old"));
        assertFalse(out.contains("account.css?v=old"));
        assertFalse(out.contains("site-shell.js?v=old"));
    }

    @Test
    void adminHtmlIsOutsideNovaFilterBoundary() throws Exception {
        var filter = new PublicFrontendAssetFilter();
        FilterChain html = (request, response) -> {
            var http = (jakarta.servlet.http.HttpServletResponse) response;
            http.setContentType("text/html;charset=UTF-8");
            http.getWriter().write("<!doctype html><html><head><link rel=\"stylesheet\" href=\"/assets/app.css?v=admin\"></head><body>admin</body></html>");
        };
        var request = new MockHttpServletRequest("GET", "/admin");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, html);
        String out = response.getContentAsString();
        assertTrue(out.contains("app.css?v=admin"));
        assertFalse(out.contains("xh-public-ui"));
        assertFalse(out.contains("nova-ui.css"));
    }
}
