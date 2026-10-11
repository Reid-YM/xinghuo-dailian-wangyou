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
    void pageControllerOwnsFormerHomeAndContactPatches() throws Exception {
        String pages = Files.readString(ASSETS.resolve("pages.js"));
        assertTrue(pages.contains("renderHomeRecommendations"));
        assertTrue(pages.contains("normalizeContactPage"));
        assertTrue(pages.contains("observeContactChannelRefresh"));
        assertTrue(pages.contains("hero-hot-list"));
        assertTrue(pages.contains("contactChannelGrid"));
    }

    @Test
    void uiFixesIsAnInertCompatibilityStub() throws Exception {
        String patches = Files.readString(ASSETS.resolve("ui-fixes.js"));
        assertTrue(patches.length() < 1_000, "ui-fixes.js must remain an inert compatibility stub until template references are removed");
        assertTrue(patches.contains("Retired compatibility stub"));
        assertFalse(patches.contains("(() =>"));
        assertFalse(patches.contains("fetch("));
        assertFalse(patches.contains("addEventListener("));
        assertFalse(patches.contains("setInterval("));
        assertFalse(patches.contains("setTimeout("));
        assertFalse(patches.contains("new MutationObserver("));
        assertFalse(patches.contains("document.querySelector"));
    }
}
