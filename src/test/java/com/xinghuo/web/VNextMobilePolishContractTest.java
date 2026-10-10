package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class VNextMobilePolishContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void v8PolishTargetsDensityThumbZoneAndIosWebviewWithoutChangingBusinessBehavior() throws IOException {
        String css = read("src/main/resources/static/assets/vnext-mobile-polish-v8.css");

        assertTrue(css.contains("@media (max-width: 767px)"));
        assertTrue(css.contains("100dvh"));
        assertTrue(css.contains("-webkit-text-size-adjust: 100%"));
        assertTrue(css.contains("touch-action: manipulation"));
        assertTrue(css.contains(".v-app[data-page=\"quote\"] .v-mobile-dock"));
        assertTrue(css.contains(".v-app[data-page=\"quote\"] .v-form-actions"));
        assertTrue(css.contains("grid-template-columns: minmax(0, 1.12fr) minmax(0, .88fr)"));
        assertTrue(css.contains("@supports (-webkit-touch-callout: none)"));
        assertTrue(css.contains("env(safe-area-inset-bottom"));
        assertTrue(css.contains("body[data-page=\"account-login\"] .auth-shell"));
        assertTrue(css.contains("body[data-page=\"guides\"] .guide-shell"));
        assertFalse(css.contains("@import url("), "v8 polish CSS must stay self-contained");
    }

    @Test
    void v8LoadsAfterV7InReactBuildAndLegacyRuntime() throws IOException {
        String buildEntry = read("frontend-vnext/src/formal-entry.css");
        String shell = read("src/main/resources/static/assets/site-shell.js");

        int buildV7 = buildEntry.indexOf("vnext-mobile-focus-v7.css");
        int buildV8 = buildEntry.indexOf("vnext-mobile-polish-v8.css");
        assertTrue(buildV7 >= 0 && buildV8 > buildV7, "React must bundle v8 after v7");

        int shellV7 = shell.indexOf("vnext-mobile-focus-v7.css?v=20261010ui07");
        int shellV8 = shell.indexOf("vnext-mobile-polish-v8.css?v=20261010ui08");
        assertTrue(shellV7 >= 0 && shellV8 > shellV7, "legacy runtime must declare v8 after v7");
        assertTrue(shell.contains("data-vnext-mobile-polish"));
        assertTrue(shell.contains("ensureMobileFocusStyles();"));
    }
}
