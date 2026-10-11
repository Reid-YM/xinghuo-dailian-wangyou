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
    void siteLoadsMutuallyExclusiveDeviceAssetLayersAfterSharedFoundation() throws Exception {
        String site = Files.readString(MAIN.resolve("resources/templates/fragments/site.html"));
        assertTrue(site.contains("th:if=\"${deviceMode == 'desktop'}\" rel=\"stylesheet\" href=\"/assets/desktop-storefront.css?v=20261011ds20\""));
        assertTrue(site.contains("th:if=\"${deviceMode == 'mobile'}\" rel=\"stylesheet\" href=\"/assets/mobile-storefront.css?v=20261011ds20\""));
        assertTrue(site.contains("th:if=\"${deviceMode == 'mobile'}\" rel=\"stylesheet\" href=\"/assets/mobile-home-v4.css?v=20261012mv401\""));
        assertTrue(site.contains("th:if=\"${deviceMode == 'mobile'}\" rel=\"stylesheet\" href=\"/assets/mobile-floating-dock-v4.css?v=20261012mv402\""));
        assertTrue(site.contains("th:if=\"${deviceMode == 'desktop'}\" defer src=\"/assets/desktop-storefront.js?v=20261011ds20\""));
        assertTrue(site.contains("th:if=\"${deviceMode == 'mobile'}\" defer src=\"/assets/mobile-storefront.js?v=20261011ds20\""));
        int sharedCss = site.indexOf("site-experience-v2.css?v=20261007ux03");
        int desktopCss = site.indexOf("desktop-storefront.css?v=20261011ds20");
        int mobileCss = site.indexOf("mobile-storefront.css?v=20261011ds20");
        int mobileV4 = site.indexOf("mobile-home-v4.css?v=20261012mv401");
        int dockV4 = site.indexOf("mobile-floating-dock-v4.css?v=20261012mv402");
        int overflow = site.indexOf("site-overflow-guard.css?v=20261011of01");
        assertTrue(sharedCss >= 0 && desktopCss > sharedCss && mobileCss > sharedCss,
                "device-specific CSS must load after the shared experience foundation");
        assertTrue(mobileV4 > mobileCss && dockV4 > mobileV4 && overflow > dockV4,
                "mobile V4, dock hardening and global overflow guard must remain in deterministic order");
        assertTrue(Files.exists(MAIN.resolve("resources/static/assets/desktop-storefront.css")));
        assertTrue(Files.exists(MAIN.resolve("resources/static/assets/mobile-storefront.css")));
        assertTrue(Files.exists(MAIN.resolve("resources/static/assets/mobile-home-v4.css")));
        assertTrue(Files.exists(MAIN.resolve("resources/static/assets/mobile-floating-dock-v4.css")));
        assertTrue(Files.exists(MAIN.resolve("resources/static/assets/desktop-storefront.js")));
        assertTrue(Files.exists(MAIN.resolve("resources/static/assets/mobile-storefront.js")));
    }

    @Test
    void frontendControlRuntimeTargetsBothHomeRootsAndUsesStableStructuralHooks() throws Exception {
        String site = Files.readString(MAIN.resolve("resources/templates/fragments/site.html"));
        String runtime = Files.readString(MAIN.resolve("resources/static/assets/frontend-controls-runtime.js"));
        assertTrue(site.contains("/assets/frontend-controls-runtime.js?v=20261012fc40"));
        assertTrue(runtime.contains("'.xh-desktop-proof-grid'"));
        assertTrue(runtime.contains("'.xh-mobile-proof-grid'"));
        assertTrue(runtime.contains("function resolveHomeModule"));
        assertTrue(runtime.contains("main.querySelector(selector)"));
        assertTrue(runtime.contains("candidate.closest('section')"));
        assertTrue(runtime.contains("main.xh-desktop-home"));
        assertTrue(runtime.contains("main.xh-mobile-home"));
        assertTrue(runtime.contains("data-dock-key=\"home\""));
        assertTrue(runtime.contains(".mobile-dock-label"));
        assertFalse(runtime.contains("text('.mobile-dock a[href=\"/services\"]'"),
                "runtime must not replace whole visual dock anchors and destroy their SVG icons");
        assertFalse(runtime.contains("main section[aria-label=\"为什么选择星火\"]"),
                "V3 ordering must use stable structural hooks instead of localized display text");
        assertFalse(runtime.contains("main.container section[aria-label=\"为什么选择星火\"]"),
                "mobile home controls must not depend on a desktop-only container class");
    }

    @Test
    void desktopAndMobileHomeOwnIndependentDomWhileKeepingBusinessHooks() throws Exception {
        String desktop = Files.readString(MAIN.resolve("resources/templates/desktop/index.html"));
        String mobile = Files.readString(MAIN.resolve("resources/templates/mobile/index.html"));
        assertFalse(desktop.contains("th:replace=\"~{index :: html}\""));
        assertFalse(mobile.contains("th:replace=\"~{index :: html}\""));
        assertTrue(desktop.contains("xh-desktop-home"));
        assertTrue(mobile.contains("xh-mobile-home"));
        assertTrue(desktop.contains("xh-desktop-command"));
        assertTrue(mobile.contains("xh-mobile-launcher"));
        assertTrue(mobile.contains("xh-mobile-hero-v4"));
        assertTrue(mobile.contains("xh-mobile-hero-features"));
        assertTrue(mobile.contains("xh-mobile-progress-card"));
        assertTrue(desktop.contains("xh-desktop-proof-grid"));
        assertTrue(mobile.contains("xh-mobile-proof-grid"));
        assertFalse(desktop.contains("xh-mobile-launcher"));
        assertFalse(mobile.contains("xh-desktop-command"));
        for (String hook : new String[]{"id=\"homeHero\"", "id=\"quoteForm\"", "id=\"orderForm\"", "id=\"gameEntrySection\"", "id=\"homeGameRail\"", "id=\"serviceGrid\"", "id=\"hotServices\"", "name=\"privacyConsent\""}) {
            assertTrue(desktop.contains(hook), "desktop missing " + hook);
            assertTrue(mobile.contains(hook), "mobile missing " + hook);
        }
        assertFalse(desktop.contains("class=\"mobile-dock\""));
        assertTrue(mobile.contains("class=\"mobile-dock\""));
        for (String key : new String[]{"home", "services", "records", "tools", "account"}) {
            assertTrue(mobile.contains("data-dock-key=\"" + key + "\""), "mobile dock missing " + key);
        }
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
        String icons = Files.readString(MAIN.resolve("resources/static/assets/icons.svg"));
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
        assertTrue(icons.contains("id=\"i-home\""));
        assertTrue(icons.contains("id=\"i-user\""));
    }
}
