package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class VNextMobileFocusContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void mobileFocusLayerCoversReactAndMaturePublicSurfaces() throws IOException {
        String css = read("src/main/resources/static/assets/vnext-mobile-focus-v7.css");

        assertTrue(css.contains("@media (max-width: 767px)"));
        assertTrue(css.contains(".v-app .v-hero-actions"));
        assertTrue(css.contains(".v-app .v-trust-row"));
        assertTrue(css.contains(".v-app .v-service-preview-grid"));
        assertTrue(css.contains(".v-app .v-service-toolbar"));
        assertTrue(css.contains(".v-app .v-form-actions"));
        assertTrue(css.contains(".v-app .v-mobile-dock button:nth-child(3)"));
        assertTrue(css.contains("body[data-page=\"account-login\"] .auth-shell"));
        assertTrue(css.contains("body[data-page=\"guides\"] .guide-shell"));
        assertTrue(css.contains("env(safe-area-inset-bottom"));
        assertFalse(css.contains("@import url("), "mobile focus CSS must remain self-contained for WebKit fixtures");
    }

    @Test
    void reactBuildAndLegacyShellLoadTheSameCanonicalMobileLayer() throws IOException {
        String buildEntry = read("frontend-vnext/src/formal-entry.css");
        String siteFragment = read("src/main/resources/templates/fragments/site.html");
        String shell = read("src/main/resources/static/assets/site-shell.js");

        assertTrue(buildEntry.contains("../../src/main/resources/static/assets/vnext-mobile-focus-v7.css"));
        assertTrue(siteFragment.contains("/assets/vnext-mobile-focus-v7.css?v=20261010ui07"));
        assertTrue(siteFragment.contains("data-vnext-mobile-focus=\"1\""));
        assertFalse(shell.contains("vnext-mobile-focus-v7.css"), "shell JS must not own CSS loading");
        assertFalse(shell.contains("ensureMobileFocusStyles"), "shell JS must remain behavior-only");
    }
}
