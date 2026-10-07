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
        assertTrue(site.contains("/assets/site-shell.css"));
        assertTrue(site.contains("/assets/site-shell.js"));
        assertFalse(site.contains("site-surface-refine.css"));
        assertFalse(site.contains("site-surface-refine.js"));
    }

    @Test
    void guideAdminLoadsOneCanonicalWorkspaceStylesheet() throws Exception {
        String admin = Files.readString(MAIN.resolve("resources/templates/admin-guides.html"));
        assertTrue(admin.contains("/assets/admin-guides-workspace.css"));
        assertFalse(admin.contains("/assets/admin-guides.css"));
        assertFalse(admin.contains("/assets/admin-guides-refine.css"));
    }

    @Test
    void shellDefinesCentralLayerTokensAndUsesRealSectionLabels() throws Exception {
        String css = Files.readString(MAIN.resolve("resources/static/assets/site-shell.css"));
        assertTrue(css.contains("--xh-z-header:40"));
        assertTrue(css.contains("--xh-z-modal:100"));
        assertTrue(css.contains("--xh-z-toast:120"));
        assertTrue(css.contains("#password>header>div>span:first-child"));
        assertFalse(css.contains("#password::before"));
        assertFalse(css.contains("#devices::before"));
    }
}
