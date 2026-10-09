package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MobileHomeRebuildContractTest {
    private static final Path MOBILE_HOME = Path.of("src/main/resources/templates/mobile/index.html");
    private static final Path DESKTOP_HOME = Path.of("src/main/resources/templates/index.html");
    private static final Path MOBILE_HOME_CSS = Path.of("src/main/resources/static/assets/mobile-home-v4.css");

    @Test
    void mobileHomeUsesIndependentV4AppArchitectureAndKeepsRuntimeAnchors() throws Exception {
        String html = Files.readString(MOBILE_HOME);

        assertTrue(html.contains("id=\"mobileHomeV4\""));
        assertTrue(html.contains("class=\"xh-v4-appbar\""));
        assertTrue(html.contains("class=\"xh-v4-task-grid\""));
        assertTrue(html.contains("class=\"xh-v4-bottom-nav\""));
        assertTrue(html.contains("id=\"quick-actions\""));
        assertTrue(html.contains("id=\"quoteForm\""));
        assertTrue(html.contains("id=\"homeGameRail\""));
        assertTrue(html.contains("id=\"hotServices\""));
        assertTrue(html.contains("id=\"homeContactList\""));
        assertTrue(html.contains("id=\"publicCaseGrid\""));
        assertTrue(html.contains("id=\"heroBackground\""));
        assertTrue(html.contains("id=\"trustTags\""));
        assertTrue(html.contains("data-primary-cta"));
        assertTrue(html.contains("name=\"privacyConsent\""));

        assertFalse(html.contains("mobileHomeV3"));
        assertFalse(html.contains("xh-mh-shortcuts"));
        assertFalse(html.contains("desktop-secondary-panel"));
        assertFalse(html.contains("home-service-showcase"));
        assertFalse(html.contains("data-smart-dock"));
    }

    @Test
    void rebuiltMobileHomeDoesNotModifyDesktopHomepage() throws Exception {
        String desktop = Files.readString(DESKTOP_HOME);
        assertFalse(desktop.contains("mobileHomeV4"));
        assertFalse(desktop.contains("xh-v4-appbar"));
        assertFalse(desktop.contains("xh-v4-bottom-nav"));
        assertFalse(desktop.contains("mobile-home-v4.css"));
    }

    @Test
    void mobileHomeVisualLayerKeepsOriginalLogoAndFullBleedBackdrop() throws Exception {
        String css = Files.readString(MOBILE_HOME_CSS);
        assertTrue(css.contains("body.mobile-frontend[data-page=\"home\"]"));
        assertTrue(css.contains("> header"));
        assertTrue(css.contains(".xh-v4-appbar"));
        assertTrue(css.contains(".xh-v4-task-grid"));
        assertTrue(css.contains("grid-template-columns:repeat(2,minmax(0,1fr))"));
        assertTrue(css.contains("height:40px!important"));
        assertTrue(css.contains("overflow-x:auto!important"));
        assertTrue(css.contains(".xh-v4-bottom-nav"));
        assertTrue(css.contains("position:fixed"));
        assertTrue(css.contains("/assets/brand-logo.svg?v=20261008logo01"));
        assertTrue(css.contains("#homeHero .mobile-hero-backdrop"));
        assertTrue(css.contains("background-size:cover!important"));
        assertFalse(css.contains("#06080b 100%"));
    }
}
