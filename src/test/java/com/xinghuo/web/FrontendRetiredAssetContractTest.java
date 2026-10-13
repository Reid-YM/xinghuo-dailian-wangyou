package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendRetiredAssetContractTest {
    private static final Path MAIN = Path.of("src/main");
    private static final Path ASSETS = MAIN.resolve("resources/static/assets");

    @Test
    void retiredVisualOverrideLayersStayDeleted() throws Exception {
        assertFalse(Files.exists(ASSETS.resolve("site-shell.css")));
        assertFalse(Files.exists(ASSETS.resolve("site-surface-refine.css")));
        assertFalse(Files.exists(ASSETS.resolve("site-surface-refine.js")));
        assertFalse(Files.exists(ASSETS.resolve("admin-guides.css")));
        assertFalse(Files.exists(ASSETS.resolve("admin-guides-refine.css")));
    }

    @Test
    void currentTemplatesUseOnlyAuthoritativeReplacementLayers() throws Exception {
        String site = Files.readString(MAIN.resolve("resources/templates/fragments/site.html"));
        String guides = Files.readString(MAIN.resolve("resources/templates/admin-guides.html"));

        assertTrue(site.contains("/assets/site-experience-v2.css"));
        assertTrue(site.contains("/assets/site-shell.js"));
        assertTrue(guides.contains("/assets/admin-guides-workspace.css"));
        assertTrue(guides.contains("/assets/admin-light-theme.css"));
    }
}
