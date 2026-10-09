package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendV6SourceContractTest {
    private static final Path CSS = Path.of("src/main/resources/static/assets/frontend-v6.css");
    private static final Path JS = Path.of("src/main/resources/static/assets/frontend-v6.js");

    @Test
    void v6KeepsOriginalBrandAndOwnResponsiveDesignTokens() throws Exception {
        String css = Files.readString(CSS);
        String js = Files.readString(JS);

        assertTrue(css.contains("--v6-brand:#f5c84f"));
        assertTrue(css.contains("--v6-max:1240px"));
        assertTrue(css.contains("body.mobile-frontend[data-page=\"home\"] #homeHero"));
        assertTrue(css.contains(".mobile-primary-nav"));
        assertTrue(css.contains("@media (max-width:760px)"));
        assertTrue(js.contains("root.dataset.xhUi = 'v6'"));
        assertTrue(js.contains("aria-current"));
    }
}
