package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendPatchDebtContractTest {
    private static final Path MAIN = Path.of("src/main/resources");
    private static final Path ASSETS = MAIN.resolve("static/assets");
    private static final Path TEMPLATES = MAIN.resolve("templates");

    @Test
    void globalShellOwnsGlobalInteractions() throws Exception {
        String shell = Files.readString(ASSETS.resolve("site-shell.js"));
        assertTrue(shell.contains("ensureMobileNavigation"));
        assertTrue(shell.contains("bindOverflowHints"));
        assertTrue(shell.contains("hydrateAccountEntry"));
        assertTrue(shell.contains("mobileNavDrawer"));
        assertTrue(shell.contains("data-account-entry"));
        assertTrue(shell.contains("x-overflow"));
    }

    @Test
    void pageControllerOwnsFormerHomeAndContactPatches() throws Exception {
        String pages = Files.readString(ASSETS.resolve("pages.js"));
        assertTrue(pages.contains("renderHomeRecommendations"));
        assertTrue(pages.contains("normalizeContactPage"));
        assertTrue(pages.contains("observeContactChannelRefresh"));
        assertTrue(pages.contains("hero-hot-list"));
        assertTrue(pages.contains("contactChannelGrid"));
    }

    @Test
    void retiredUiFixesAssetAndTemplateReferencesAreGone() throws Exception {
        assertFalse(Files.exists(ASSETS.resolve("ui-fixes.js")), "retired ui-fixes.js must not be restored");
        try (var files = Files.walk(TEMPLATES)) {
            for (Path path : files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".html")).toList()) {
                assertFalse(
                        Files.readString(path).contains("/assets/ui-fixes.js"),
                        () -> "template must not reference retired ui-fixes.js: " + path);
            }
        }
    }
}
