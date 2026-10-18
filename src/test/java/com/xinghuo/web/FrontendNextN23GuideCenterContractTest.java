package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN23GuideCenterContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerExposesGuideCenterAndFingerprintsRuntime() throws Exception {
        String preview = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        assertTrue(preview.contains("\"/next/guides\""));
        assertTrue(preview.contains("N23_RUNTIME"));
        assertTrue(preview.contains("/next/n23-guide-center.js"));
        assertTrue(preview.contains("\"next/n23-guide-center.js\""));
        assertTrue(preview.contains("\"攻略中心 · \" + brand"));
        assertTrue(preview.contains("\"/guides\".equals(path)"));
        assertTrue(preview.contains("noindex,follow"));
    }

    @Test
    void guideCenterReusesExistingPublicReadApiOnly() throws Exception {
        String runtime = read("resources/static/next/n23-guide-center.js");
        String api = read("java/com/xinghuo/guide/PublicGuideController.java");

        assertTrue(runtime.contains("const API = '/api/guides?limit=100'"));
        assertTrue(runtime.contains("method: 'GET'"));
        assertTrue(runtime.contains("credentials: 'same-origin'"));
        assertTrue(runtime.contains("cache: 'no-store'"));
        assertTrue(api.contains("@RequestMapping(\"/api/guides\")"));
        assertTrue(api.contains("return guides.publicList(game, q, limit)"));

        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));
    }

    @Test
    void guideCenterKeepsSearchAndGameStateInUrlAndSupportsHistory() throws Exception {
        String runtime = read("resources/static/next/n23-guide-center.js");

        assertTrue(runtime.contains("params.get('q')"));
        assertTrue(runtime.contains("params.get('game')"));
        assertTrue(runtime.contains("url.searchParams.set('q'"));
        assertTrue(runtime.contains("url.searchParams.set('game'"));
        assertTrue(runtime.contains("history.replaceState"));
        assertTrue(runtime.contains("history.pushState"));
        assertTrue(runtime.contains("window.addEventListener('popstate', activate)"));
        assertTrue(runtime.contains("request !== requestSeq"));
    }

    @Test
    void guideCardsStayInsideNextAndLegacyEscapeRemainsExplicit() throws Exception {
        String runtime = read("resources/static/next/n23-guide-center.js");
        String continuity = read("resources/static/next/n14-route-continuity.js");

        assertTrue(runtime.contains("`/next/guides/${encodeURIComponent(text(item?.slug))}`"));
        assertTrue(runtime.contains("legacy.href = '/guides?legacy=1'"));
        assertTrue(runtime.contains("legacy.dataset.n23Legacy = '1'"));
        assertTrue(continuity.contains("link.dataset.n23Legacy === '1'"));
        assertTrue(continuity.contains("url.pathname === '/guides' || url.pathname === '/guides/'"));
        assertTrue(continuity.contains("nextUrl(url, '/next/guides')"));
        assertTrue(continuity.contains("`/next/guides/${guide[1]}`"));
    }

    @Test
    void guideCenterUsesCanonicalLegacyUrlAndPreviewRobots() throws Exception {
        String runtime = read("resources/static/next/n23-guide-center.js");

        assertTrue(runtime.contains("canonical.href = 'https://wydlgf.cn/guides'"));
        assertTrue(runtime.contains("robots.content = 'noindex,follow'"));
        assertTrue(runtime.contains("document.title = '攻略中心 · 星火网游'"));
    }
}
