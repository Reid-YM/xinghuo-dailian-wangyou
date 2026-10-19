package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendStyleLayerContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void sharedSiteLoadsCompatibilityThenNextFoundation() throws Exception {
        String site = Files.readString(MAIN.resolve("resources/templates/fragments/site.html"));
        assertTrue(site.contains("/assets/site-experience-v2.css?v=20261012ux04"));
        assertTrue(site.contains("/assets/frontend-next-foundation.css?v=20261019cc01"));
        assertTrue(site.contains("/assets/site-shell.js?v=20261012ss06"));
        assertTrue(site.contains("/assets/brand-logo.svg?v=20261008logo01"));
        assertTrue(site.contains("width=\"32\" height=\"32\" decoding=\"async\""));
        assertFalse(site.contains("style=\"display:block;width:32px;height:32px;flex:0 0 32px;border-radius:8px\""));
        assertFalse(site.contains("brand-dot"));
        assertFalse(site.contains("site-shell.css"));
        assertFalse(site.contains("site-surface-refine.css"));
        assertFalse(site.contains("site-surface-refine.js"));
        assertFalse(site.contains("style=\"-webkit-backdrop-filter:none!important;backdrop-filter:none!important;transition:none!important\""));

        int compatibility = site.indexOf("site-experience-v2.css?v=20261012ux04");
        int next = site.indexOf("frontend-next-foundation.css?v=20261019cc01");
        int runtime = site.indexOf("frontend-controls-runtime.js");
        assertTrue(compatibility >= 0 && next > compatibility,
                "Frontend Next must override the existing compatibility layer without deleting business CSS contracts");
        assertTrue(runtime > next,
                "runtime frontend controls must remain after visual CSS so theme variables and visibility controls still work");
        assertTrue(site.contains("data-experience-shell=\"v4\""));
    }

    @Test
    void nextFoundationOwnsWideCanvasHomeHierarchyAndMobileSafety() throws Exception {
        String css = Files.readString(MAIN.resolve("resources/static/assets/frontend-next-foundation.css"));
        assertTrue(css.contains("--xh-next-max:1720px"));
        assertTrue(css.contains("body[data-page=\"home\"] .hero.hero-r3"));
        assertTrue(css.contains("grid-template-columns:minmax(0,1.28fr) minmax(360px,.72fr)"));
        assertTrue(css.contains(".linked-selects.four-step"));
        assertTrue(css.contains("@media (min-width:1600px)"));
        assertTrue(css.contains("@media (max-width:760px)"));
        assertTrue(css.contains("input,select,textarea{font-size:16px!important}"));
        assertTrue(css.contains("env(safe-area-inset-bottom,0px)"));
        assertTrue(css.contains("@media (prefers-reduced-motion:reduce)"));
        assertTrue(css.contains("@media (forced-colors:active)"));
        assertTrue(css.contains("var(--accent-rgb"));
        assertFalse(css.contains("width:1920px"), "wide layout must stay fluid rather than hard-code one monitor size");
    }

    @Test
    void recoveryGuideAndNativeLoginUseTheirOwnTheme() throws Exception {
        String guides = Files.readString(MAIN.resolve("resources/templates/admin-guides.html"));
        String login = Files.readString(MAIN.resolve("resources/templates/admin-login.html"));
        assertTrue(guides.contains("/assets/admin-guides-workspace.css"));
        assertTrue(guides.contains("/assets/admin-light-theme.css?v=20261007l001"));
        assertTrue(login.contains("/assets/admin-xingwei-login.css?v=20261016audit2"));
        assertFalse(login.contains("admin-light-theme"));
        assertFalse(guides.contains("/assets/admin-guides.css"));
        assertFalse(guides.contains("/assets/admin-guides-refine.css"));
    }

    @Test
    void compatibilityLayerStillOwnsSharedBehaviorAssets() throws Exception {
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
