package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MobileReferenceV5ContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void mobileReferenceLayerLoadsAfterV4AndBeforeOverflowGuard() throws Exception {
        String site = Files.readString(MAIN.resolve("resources/templates/fragments/site.html"));
        int v4 = site.indexOf("mobile-home-v4.css?v=20261012mv401");
        int dock = site.indexOf("mobile-floating-dock-v4.css?v=20261012mv402");
        int v5 = site.indexOf("mobile-home-reference-v5.css?v=20261012mv501");
        int geometry = site.indexOf("mobile-home-reference-v5-layout.css?v=20261012mv502");
        int overflow = site.indexOf("site-overflow-guard.css?v=20261011of01");
        assertTrue(v4 >= 0 && dock > v4 && v5 > dock && geometry > v5 && overflow > geometry,
                "V5 visual and geometry layers must load after V4 and before the final overflow guard");
    }

    @Test
    void referenceLayerLocksApprovedFirstScreenComposition() throws Exception {
        String css = Files.readString(MAIN.resolve("resources/static/assets/mobile-home-reference-v5.css"));
        String geometry = Files.readString(MAIN.resolve("resources/static/assets/mobile-home-reference-v5-layout.css"));
        assertTrue(css.contains("height:350px!important"));
        assertTrue(css.contains("content:\"使命召唤\""));
        assertTrue(css.contains("content:\"MW4 代练服务\""));
        assertTrue(css.contains("grid-template-columns:repeat(4,minmax(0,1fr))"));
        assertTrue(css.contains(".xh-mobile-game-entry.hidden"));
        assertTrue(css.contains("codmw4-3.jpg"));
        assertTrue(css.contains(".mobile-dock>a[aria-current=\"page\"]"));
        assertTrue(css.contains(".floating-customer-service"));
        assertTrue(geometry.contains("#homeHero.xh-mobile-hero-v4"));
        assertTrue(geometry.contains("height:480px!important"));
        assertTrue(geometry.contains(">.xh-mobile-progress-card"));
        assertTrue(geometry.contains("top:360px!important"));
    }
}
