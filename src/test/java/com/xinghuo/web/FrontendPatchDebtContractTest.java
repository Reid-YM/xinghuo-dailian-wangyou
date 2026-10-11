package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendPatchDebtContractTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

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
    void transitionalUiFixesContainsNoPollingOrRetiredToolSelectors() throws Exception {
        String patches = Files.readString(ASSETS.resolve("ui-fixes.js"));
        assertFalse(patches.contains("setInterval("));
        assertFalse(patches.contains("new MutationObserver("));
        assertFalse(patches.contains("weaponList"));
        assertFalse(patches.contains("progressSuggest"));
        assertFalse(patches.contains("camo-mode-preview-art"));
        assertFalse(patches.contains(".camo-check"));
        assertFalse(patches.contains("language-switcher"));
        assertFalse(patches.contains("clearLocalRecords"));
        assertFalse(patches.contains("data-account-entry"));
        assertFalse(patches.contains("nav-toggle"));
        assertFalse(patches.contains("x-overflow"));
    }

    @Test
    void transitionalUiFixesStaysSmallAndPageScoped() throws Exception {
        String patches = Files.readString(ASSETS.resolve("ui-fixes.js"));
        assertTrue(patches.length() < 6_000, "ui-fixes.js must remain transitional, not become a global patch bucket again");
        assertTrue(patches.contains("dataset?.page !== 'contact'"));
        assertTrue(patches.contains("dataset?.page !== 'home'"));
    }
}
