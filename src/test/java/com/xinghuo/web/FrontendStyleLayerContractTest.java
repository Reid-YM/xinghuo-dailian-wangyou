package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendStyleLayerContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void sharedSiteUsesNovaShellWithoutLegacyExperienceRuntime() throws Exception {
        String site = Files.readString(MAIN.resolve("resources/templates/fragments/site.html"));
        assertTrue(site.contains("class=\"nv-header\""));
        assertTrue(site.contains("data-shell=\"nova\""));
        assertTrue(site.contains("data-nv-menu-toggle"));
        assertTrue(site.contains("class=\"nv-footer\""));
        assertTrue(site.contains("/assets/brand-logo.svg?v=20261008logo01"));
        assertFalse(site.contains("site-experience-v2.css"));
        assertFalse(site.contains("site-shell.js"));
        assertFalse(site.contains("mobile-home"));
    }

    @Test
    void guideAndLoginAdminKeepDedicatedAdminTheme() throws Exception {
        String guides = Files.readString(MAIN.resolve("resources/templates/admin-guides.html"));
        String login = Files.readString(MAIN.resolve("resources/templates/admin-login.html"));
        assertTrue(guides.contains("/assets/admin-guides-workspace.css"));
        assertTrue(guides.contains("/assets/admin-light-theme.css?v=20261007l001"));
        assertTrue(login.contains("/assets/admin-light-theme.css?v=20261007l001"));
        assertFalse(guides.contains("/assets/admin-guides.css"));
        assertFalse(guides.contains("/assets/admin-guides-refine.css"));
    }

    @Test
    void novaOwnsPublicTokensShellComponentsAndResponsiveNavigation() throws Exception {
        String css = Files.readString(MAIN.resolve("resources/static/assets/nova-ui.css"));
        String js = Files.readString(MAIN.resolve("resources/static/assets/nova-shell.js"));
        assertTrue(css.contains("--nv-accent:#ff7a1a"));
        assertTrue(css.contains(".nv-header"));
        assertTrue(css.contains(".nv-home-hero"));
        assertTrue(css.contains(".nv-mobile-dock"));
        assertTrue(css.contains("input,select,textarea{font-size:16px"));
        assertTrue(js.contains("document.documentElement.dataset.publicUi = 'nova'"));
        assertTrue(js.contains("data-nv-route"));
        assertFalse(css.contains("cyGridDrift"));
        assertFalse(css.contains(".xh-v4"));
    }
}
