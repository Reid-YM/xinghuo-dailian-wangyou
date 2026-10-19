package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN24MasteryContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void explicitPreviewRouteDelegatesToSharedNextDocument() throws Exception {
        String controller = read("java/com/xinghuo/web/FrontendNextN24MasteryController.java");
        assertTrue(controller.contains("@GetMapping(\"/next/mastery\")"));
        assertTrue(controller.contains("preview.frontendNext(request)"));
    }

    @Test
    void nextTrackerSharesLegacyLocalAndCloudProgressProtocols() throws Exception {
        String runtime = read("resources/static/next/n24-mastery.js");
        String legacy = read("resources/static/assets/mw4-camo-tracker.js");

        assertTrue(runtime.contains("const STORAGE_KEY = 'xinghuo-mw4-camo-tracker-v2'"));
        assertTrue(runtime.contains("const TOOL_KEY = 'mw4_camo_tracker'"));
        assertTrue(legacy.contains("const STORAGE_KEY = 'xinghuo-mw4-camo-tracker-v2'"));
        assertTrue(legacy.contains("const TOOL_KEY = 'mw4_camo_tracker'"));
        assertTrue(runtime.contains("localStorage.getItem(STORAGE_KEY)"));
        assertTrue(runtime.contains("localStorage.setItem(STORAGE_KEY"));
        assertTrue(runtime.contains("/api/account/progress/${TOOL_KEY}"));
        assertTrue(runtime.contains("method: 'PUT'"));
        assertTrue(runtime.contains("'X-Requested-With': 'fetch'"));
        assertTrue(runtime.contains("version: cloud.version ?? 0"));
        assertTrue(runtime.contains("response.status === 409"));
    }

    @Test
    void masteryUsesRealToolDataAndKeepsExplicitLegacyEscape() throws Exception {
        String runtime = read("resources/static/next/n24-mastery.js");
        String routes = read("resources/static/next/n14-route-continuity.js");

        assertTrue(runtime.contains("fetch('/api/tool-station'"));
        assertTrue(runtime.contains("method: 'GET'"));
        assertTrue(runtime.contains("game.camoTracks || {}"));
        assertTrue(runtime.contains("legacy.href = '/mastery'"));
        assertTrue(runtime.contains("legacy.dataset.n24Legacy = '1'"));
        assertTrue(runtime.contains("canonical.href = 'https://wydlgf.cn/mastery'"));
        assertTrue(runtime.contains("robots.content = 'noindex,follow'"));
        assertTrue(runtime.contains("const QUOTE_KEY = 'xinghuo-cod-tool-quote-snapshot'"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));

        assertTrue(routes.contains("url.pathname === '/mastery'"));
        assertTrue(routes.contains("nextUrl(url, '/next/mastery')"));
        assertTrue(routes.contains("stageSource('/next/n24-mastery.js')"));
        assertTrue(routes.contains("url.pathname === '/account/submit'"));
        assertTrue(routes.contains("link.dataset.n24Legacy === '1'"));
    }
}
