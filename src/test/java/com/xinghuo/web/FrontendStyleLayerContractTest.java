package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendStyleLayerContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void sharedSiteUsesSingleAuthoritativeExperienceLayer() throws Exception {
        String site = Files.readString(MAIN.resolve("resources/templates/fragments/site.html"));
        assertTrue(site.contains("/assets/site-experience-v2.css?v=20261007ux03"));
        assertTrue(site.contains("/assets/site-shell.js?v=20261008ss04"));
        assertTrue(site.contains("/assets/brand-logo.svg?v=20261008logo01"));
        assertFalse(site.contains("brand-dot"));
        assertFalse(site.contains("site-shell.css"));
        assertFalse(site.contains("site-surface-refine.css"));
        assertFalse(site.contains("site-surface-refine.js"));
    }

    @Test
    void guideAndLoginAdminUseUnifiedWhiteTheme() throws Exception {
        String guides = Files.readString(MAIN.resolve("resources/templates/admin-guides.html"));
        String login = Files.readString(MAIN.resolve("resources/templates/admin-login.html"));
        assertTrue(guides.contains("/assets/admin-guides-workspace.css"));
        assertTrue(guides.contains("/assets/admin-light-theme.css?v=20261007l001"));
        assertTrue(login.contains("/assets/admin-light-theme.css?v=20261007l001"));
        assertFalse(guides.contains("/assets/admin-guides.css"));
        assertFalse(guides.contains("/assets/admin-guides-refine.css"));
    }

    @Test
    void experienceLayerOwnsZIndexFloatingSupportSecurityAndBrandAssets() throws Exception {
        String css = Files.readString(MAIN.resolve("resources/static/assets/site-experience-v2.css"));
        String js = Files.readString(MAIN.resolve("resources/static/assets/site-shell.js"));
        String logo = Files.readString(MAIN.resolve("resources/static/assets/brand-logo.svg"));
        assertTrue(css.contains("--xh-z-header:40"));
        assertTrue(css.contains("--xh-z-floating:70"));
        assertTrue(css.contains("--xh-z-modal:100"));
        assertTrue(css.contains("--xh-z-toast:120"));
        assertTrue(css.contains(".floating-customer-service"));
        assertTrue(css.contains(".account-settings-view[data-settings-view=\"security\"]"));
        assertTrue(js.contains("floatingCustomerService"));
        assertTrue(js.contains("href = '/contact'"));
        assertTrue(js.contains("#07c160"));
        assertTrue(js.contains("/assets/icons.svg#i-support"));
        assertTrue(js.contains("data-xh-brand-favicon"));
        assertTrue(js.contains("/assets/brand-logo.svg?v=20261008logo01"));
        assertTrue(logo.contains("fill=\"#0A0C0F\""));
        assertTrue(logo.contains("fill=\"#FFD24A\""));
        assertTrue(logo.contains("fill=\"#D89A00\""));
    }
}
