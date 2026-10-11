package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MobileHeroBackendControlContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void backendOwnsMobileHeroBackgroundImageSource() throws Exception {
        String site = Files.readString(MAIN.resolve("resources/templates/fragments/site.html"));
        String guard = Files.readString(MAIN.resolve("resources/static/assets/mobile-home-v5-backend-hero.css"));
        String publicJs = Files.readString(MAIN.resolve("resources/static/assets/public.js"));
        String imageControls = Files.readString(MAIN.resolve("resources/static/assets/site-image-controls.js"));
        String mobile = Files.readString(MAIN.resolve("resources/templates/mobile/index.html"));

        int visual = site.indexOf("mobile-home-reference-v5.css?v=20261012mv501");
        int geometry = site.indexOf("mobile-home-reference-v5-layout.css?v=20261012mv502");
        int backendGuard = site.indexOf("mobile-home-v5-backend-hero.css?v=20261012mv503");
        int overflow = site.indexOf("site-overflow-guard.css?v=20261011of01");

        assertTrue(visual >= 0 && geometry > visual && backendGuard > geometry && overflow > backendGuard,
                "backend hero ownership guard must load after V5 and before overflow guard");
        assertTrue(mobile.contains("id=\"heroBackground\""));
        assertTrue(publicJs.contains("config.heroBackgroundImage"));
        assertTrue(publicJs.contains("SiteImageControls.background"));
        assertTrue(imageControls.contains("heroBackgroundFit"));
        assertTrue(imageControls.contains("heroBackgroundPositionX"));
        assertTrue(imageControls.contains("heroBackgroundPositionY"));
        assertTrue(imageControls.contains("heroBackgroundOpacity"));
        assertTrue(imageControls.contains("heroBackgroundBlur"));
        assertTrue(guard.contains("background-image:none!important"));
        assertFalse(guard.contains("url("), "guard must never hardcode a Hero image URL");
    }
}
