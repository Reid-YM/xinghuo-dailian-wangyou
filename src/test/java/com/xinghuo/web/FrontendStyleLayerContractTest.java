package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendStyleLayerContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void sharedSiteUsesCanonicalShellAssets() throws Exception {
        String site = Files.readString(MAIN.resolve("resources/templates/fragments/site.html"));
        assertTrue(site.contains("/assets/site-shell.css?v=20261007ss02"));
        assertTrue(site.contains("/assets/site-shell.js?v=20261007ss02"));
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
    void shellDefinesCentralLayerTokensAndRestoresFloatingSupport() throws Exception {
        String css = Files.readString(MAIN.resolve("resources/static/assets/site-shell.css"));
        String js = Files.readString(MAIN.resolve("resources/static/assets/site-shell.js"));
        assertTrue(css.contains("--xh-z-header:40"));
        assertTrue(css.contains("--xh-z-floating:70"));
        assertTrue(css.contains("--xh-z-modal:100"));
        assertTrue(css.contains("--xh-z-toast:120"));
        assertTrue(css.contains(".floating-customer-service"));
        assertTrue(js.contains("floatingCustomerService"));
        assertTrue(js.contains("href = '/contact'"));
        assertTrue(css.contains("#password>header>div>span:first-child"));
        assertFalse(css.contains("#password::before"));
        assertFalse(css.contains("#devices::before"));
    }
}
