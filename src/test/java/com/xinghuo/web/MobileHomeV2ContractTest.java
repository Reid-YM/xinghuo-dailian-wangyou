package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MobileHomeV2ContractTest {
    private static final Path MOBILE_HOME = Path.of("src/main/resources/templates/mobile/index.html");
    private static final Path DESKTOP_HOME = Path.of("src/main/resources/templates/index.html");
    private static final Path V2_CSS = Path.of("src/main/resources/static/assets/mobile-home-v2.css");

    @Test
    void mobileHomeV2UsesFreshNamespaceAndKeepsBusinessAnchors() throws Exception {
        String html = Files.readString(MOBILE_HOME);

        assertTrue(html.contains("data-mobile-home=\"v2\""));
        assertTrue(html.contains("/assets/mobile-home-v2.css?v=20261009mhv2-01"));
        assertTrue(html.contains("class=\"mh2-topbar\""));
        assertTrue(html.contains("class=\"mh2-hero\""));
        assertTrue(html.contains("class=\"mh2-task-grid\""));
        assertTrue(html.contains("class=\"mh2-bottom-nav\""));

        for (String anchor : new String[]{
            "id=\"homeHero\"",
            "id=\"heroBackground\"",
            "id=\"trustTags\"",
            "id=\"quick-actions\"",
            "id=\"quoteForm\"",
            "id=\"homeGameRail\"",
            "id=\"hotServices\"",
            "id=\"homeContactList\"",
            "id=\"publicCaseGrid\"",
            "data-primary-cta",
            "data-link-game",
            "data-link-platform",
            "data-link-category",
            "data-link-service",
            "name=\"privacyConsent\""
        }) {
            assertTrue(html.contains(anchor), () -> "missing runtime/business anchor: " + anchor);
        }

        assertFalse(html.contains("xh-v4"));
        assertFalse(html.contains("mobileHomeV4"));
        assertFalse(html.contains("mobile-home-v4.css"));
        assertFalse(html.contains("mobile-home-shell-v1.css"));
        assertFalse(html.contains("mobile-app.css"));
        assertFalse(html.contains("mobile-app.js"));
        assertFalse(html.contains("mobile-visual-runtime.js"));
        assertFalse(html.contains("fragments/site :: header"));
    }

    @Test
    void v2StylesAreSelfContainedAndDoNotDependOnLegacySelectors() throws Exception {
        String css = Files.readString(V2_CSS);
        assertTrue(css.contains("body[data-mobile-home=\"v2\"]"));
        assertTrue(css.contains(".mh2-topbar"));
        assertTrue(css.contains(".mh2-artwork"));
        assertTrue(css.contains("aspect-ratio: 16 / 9"));
        assertTrue(css.contains(".mh2-hero-actions"));
        assertTrue(css.contains(".mh2-bottom-nav"));
        assertTrue(css.contains("grid-template-columns: repeat(5, minmax(0,1fr))"));
        assertTrue(css.contains("font-size: 16px"));

        assertFalse(css.contains(".xh-v4"));
        assertFalse(css.contains(".mobile-primary-nav"));
        assertFalse(css.contains(".mobile-hero-backdrop"));
        assertFalse(css.contains("mobile-home-shell"));
    }

    @Test
    void desktopHomepageRemainsIndependentFromMobileV2() throws Exception {
        String desktop = Files.readString(DESKTOP_HOME);
        assertFalse(desktop.contains("mobile-home-v2.css"));
        assertFalse(desktop.contains("mh2-topbar"));
        assertFalse(desktop.contains("data-mobile-home=\"v2\""));
    }
}
