package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class VNextPreviewContractTest {
    private static final Path ROOT = Path.of("");

    @Test
    void vnextBuildIsIsolatedFromCurrentProductionEntry() throws Exception {
        String pom = Files.readString(ROOT.resolve("pom.xml"));
        String vite = Files.readString(ROOT.resolve("frontend-vnext/vite.config.ts"));
        String controller = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/controller/VNextPreviewController.java"));
        String security = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/security/SecurityConfig.java"));

        assertTrue(pom.contains("frontend-maven-plugin"));
        assertTrue(pom.contains("frontend-vnext"));
        assertTrue(vite.contains("base: '/vnext/'"));
        assertTrue(vite.contains("../target/classes/static/vnext"));
        assertTrue(controller.contains("forward:/vnext/index.html"));
        assertTrue(security.contains("\"/vnext\", \"/vnext/**\""));
        assertFalse(controller.contains("@GetMapping(\"/\")"),
                "VNext preview must not replace the production home route before UI approval");
    }

    @Test
    void responsiveAndWechatContractsAreExplicit() throws Exception {
        String css = Files.readString(ROOT.resolve("frontend-vnext/src/styles.css"));
        String environment = Files.readString(ROOT.resolve("frontend-vnext/src/lib/environment.ts"));
        String main = Files.readString(ROOT.resolve("frontend-vnext/src/main.tsx"));

        assertTrue(css.contains("env(safe-area-inset-top"));
        assertTrue(css.contains("env(safe-area-inset-bottom"));
        assertTrue(css.contains("@media (max-width: 479px)"));
        assertTrue(css.contains("@media (max-width: 767px)"));
        assertTrue(css.contains("@media (max-width: 1023px)"));
        assertTrue(css.contains("@media (max-width: 1279px)"));
        assertTrue(css.contains("@media (min-width: 1920px)"));
        assertTrue(css.contains("font-size: 16px !important"));
        assertTrue(css.contains("@media (prefers-reduced-motion: reduce)"));
        assertTrue(css.contains("@media (forced-colors: active)"));
        assertTrue(environment.contains("MicroMessenger"));
        assertTrue(environment.contains("visualViewport"));
        assertTrue(main.contains("/api/services" ) || Files.readString(ROOT.resolve("frontend-vnext/src/lib/api.ts")).contains("/api/services"));
        assertFalse(main.contains("fetch('/api/quote'"),
                "UI review preview must not create real quotes before approval");
    }
}
