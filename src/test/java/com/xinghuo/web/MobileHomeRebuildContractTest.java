package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MobileHomeRebuildContractTest {
    private static final Path MOBILE_HOME = Path.of("src/main/resources/templates/mobile/index.html");
    private static final Path DESKTOP_HOME = Path.of("src/main/resources/templates/index.html");
    private static final Path MOBILE_HOME_CSS = Path.of("src/main/resources/static/assets/mobile-home-v3.css");

    @Test
    void mobileHomeUsesIndependentV3ArchitectureAndKeepsRuntimeAnchors() throws Exception {
        String html = Files.readString(MOBILE_HOME);

        assertTrue(html.contains("id=\"mobileHomeV3\""));
        assertTrue(html.contains("class=\"xh-mh-hero"));
        assertTrue(html.contains("class=\"xh-mh-shortcuts\""));
        assertTrue(html.contains("id=\"quick-actions\""));
        assertTrue(html.contains("id=\"quoteForm\""));
        assertTrue(html.contains("id=\"homeGameRail\""));
        assertTrue(html.contains("id=\"hotServices\""));
        assertTrue(html.contains("id=\"homeContactList\""));
        assertTrue(html.contains("id=\"publicCaseGrid\""));
        assertTrue(html.contains("id=\"heroBackground\""));
        assertTrue(html.contains("data-primary-cta"));
        assertTrue(html.contains("name=\"privacyConsent\""));

        assertFalse(html.contains("desktop-secondary-panel"));
        assertFalse(html.contains("home-service-showcase"));
        assertFalse(html.contains("data-smart-dock"));
        assertFalse(html.contains("home-quick-grid.css"));
    }

    @Test
    void rebuiltMobileHomeDoesNotModifyDesktopHomepage() throws Exception {
        String desktop = Files.readString(DESKTOP_HOME);
        assertFalse(desktop.contains("mobileHomeV3"));
        assertFalse(desktop.contains("xh-mh-shortcuts"));
        assertFalse(desktop.contains("mobile-home-v3.css"));
    }

    @Test
    void mobileHomeCssIsStrictlyScopedAndRemovesGlassShell() throws Exception {
        String css = Files.readString(MOBILE_HOME_CSS);
        assertTrue(css.contains("body.mobile-frontend[data-page=\"home\"]"));
        assertTrue(css.contains(".mobile-hero-backdrop"));
        assertTrue(css.contains("display: none !important"));
        assertTrue(css.contains("backdrop-filter: none"));
        assertTrue(css.contains("grid-template-columns: repeat(2,minmax(0,1fr))"));
        assertTrue(css.contains("height: 40px !important"));
        assertTrue(css.contains("overflow-x: auto !important"));
    }
}
