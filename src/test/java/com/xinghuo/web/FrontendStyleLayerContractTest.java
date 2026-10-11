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
        assertTrue(site.contains("th:if=\"${deviceMode == 'desktop'}\" defer src=\"/assets/desktop-storefront.js?v=20261011ds20\""));
        assertTrue(site.contains("th:if=\"${deviceMode == 'mobile'}\" defer src=\"/assets/mobile-storefront.js?v=20261011ds20\""));
        int sharedCss = site.indexOf("site-experience-v2.css?v=20261007ux03");
        int desktopCss = site.indexOf("desktop-storefront.css?v=20261011ds20");
        int mobileCss = site.indexOf("mobile-storefront.css?v=20261011ds20");
        assertTrue(sharedCss >= 0 && desktopCss > sharedCss && mobileCss > sharedCss,
                "device-specific CSS must load after the shared experience foundation");
        assertTrue(Files.exists(MAIN.resolve("resources/static/assets/desktop-storefront.css")));
        assertTrue(Files.exists(MAIN.resolve("resources/static/assets/mobile-storefront.css")));
        assertTrue(Files.exists(MAIN.resolve("resources/static/assets/desktop-storefront.js")));
        assertTrue(Files.exists(MAIN.resolve("resources/static/assets/mobile-storefront.js")));
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
        assertFalse(desktop.contains("xh-mobile-launcher"));
        assertFalse(mobile.contains("xh-desktop-command"));
        for (String hook : new String[]{"id=\"homeHero\"", "id=\"quoteForm\"", "id=\"orderForm\"", "id=\"gameEntrySection\"", "id=\"homeGameRail\"", "id=\"serviceGrid\"", "id=\"hotServices\"", "name=\"privacyConsent\""}) {
            assertTrue(desktop.contains(hook), "desktop missing " + hook);
            assertTrue(mobile.contains(hook), "mobile missing " + hook);
        }
        assertFalse(desktop.contains("class=\"mobile-dock\""));
        assertTrue(mobile.contains("class=\"mobile-dock\""));
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
