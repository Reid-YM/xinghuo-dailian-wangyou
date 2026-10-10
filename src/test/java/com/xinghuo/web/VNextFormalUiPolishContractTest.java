package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class VNextFormalUiPolishContractTest {
    private static final Path CSS = Path.of("src/main/resources/static/assets/vnext-formal-ui.css");

    @Test
    void formalThemeHidesPreviewEnvironmentBadge() throws IOException {
        String css = Files.readString(CSS);
        assertTrue(css.contains(".v-production .v-env-badge{display:none}"));
    }

    @Test
    void formalThemeKeepsWideScreenAndMobileCoverage() throws IOException {
        String css = Files.readString(CSS);
        assertTrue(css.contains("@media(min-width:1920px)"));
        assertTrue(css.contains("@media(min-width:2400px)"));
        assertTrue(css.contains("@media(max-width:767px)"));
        assertTrue(css.contains(".v-production .v-mobile-dock"));
    }

    @Test
    void formalThemeKeepsWechatAndIosSafeAreaRules() throws IOException {
        String css = Files.readString(CSS);
        assertTrue(css.contains("html.is-wechat"));
        assertTrue(css.contains("html.is-ios"));
        assertTrue(css.contains("env(safe-area-inset-bottom"));
    }
}
