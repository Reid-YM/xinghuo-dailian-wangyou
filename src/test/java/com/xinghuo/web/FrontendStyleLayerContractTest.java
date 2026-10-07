package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendStyleLayerContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void sharedSiteUsesAuthoritativeExperienceLayerWithLegacyQuarantine() throws Exception {
        String site = Files.readString(MAIN.resolve("resources/templates/fragments/site.html"));
        String experience = "/assets/site-experience-v4.css?v=20261007ux04";
        String quarantine = "/assets/site-experience-v4-legacy-quarantine.css?v=20261007q001";
        assertTrue(site.contains(experience));
        assertTrue(site.contains(quarantine));
        assertTrue(site.indexOf(quarantine) > site.indexOf(experience));
        assertTrue(site.contains("data-experience-shell=\"v4\""));
        assertTrue(site.contains("/assets/site-shell.js?v=20261007ss02"));
        assertFalse(site.contains("site-experience-v2.css"));
        assertFalse(site.contains("site-shell.css"));
        assertFalse(site.contains("site-surface-refine.css"));
        assertFalse(site.contains("site-surface-refine.js"));
    }

    @Test
    void legacyQuarantineOnlyRestoresV4ContractAgainstHistoricalImportantRules() throws Exception {
        String css = Files.readString(MAIN.resolve("resources/static/assets/site-experience-v4-legacy-quarantine.css"));
        assertTrue(css.contains("body[data-page=\"home\"]"));
        assertTrue(css.contains("background:var(--xh-canvas)!important"));
        assertTrue(css.contains("max-width:var(--max)!important"));
        assertTrue(css.contains("body .mobile-dock"));
        assertTrue(css.contains("opacity:1!important"));
        assertTrue(css.contains("pointer-events:auto!important"));
        assertTrue(css.contains("background:rgba(255,255,255,.98)!important"));
        assertTrue(css.contains("min-height:46px!important"));
        assertFalse(css.contains("body.admin-solid-ui"));
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
    void experienceLayerOwnsBrandToWorkspaceContrastAndOperationalStates() throws Exception {
        String css = Files.readString(MAIN.resolve("resources/static/assets/site-experience-v4.css"));
        String js = Files.readString(MAIN.resolve("resources/static/assets/site-shell.js"));
        assertTrue(css.contains("--xh-z-header:40"));
        assertTrue(css.contains("--xh-z-floating:70"));
        assertTrue(css.contains("--xh-z-modal:100"));
        assertTrue(css.contains("--xh-z-toast:120"));
        assertTrue(css.contains("--xh-dark:#0b0c0f"));
        assertTrue(css.contains("--xh-canvas:#f1f2f4"));
        assertTrue(css.contains("background:var(--xh-canvas)!important"));
        assertTrue(css.contains(".hero.hero-r3"));
        assertTrue(css.contains("background:var(--xh-dark)!important"));
        assertTrue(css.contains(".floating-customer-service"));
        assertTrue(css.contains(".account-center-page"));
        assertTrue(js.contains("floatingCustomerService"));
        assertTrue(js.contains("href = '/contact'"));
    }
}
