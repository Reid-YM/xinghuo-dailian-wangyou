package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN26RecordsCenterContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void explicitPreviewRouteDelegatesToSharedNextDocument() throws Exception {
        String controller = read("java/com/xinghuo/web/FrontendNextN26RecordsController.java");
        assertTrue(controller.contains("@GetMapping(\"/next/records\")"));
        assertTrue(controller.contains("preview.frontendNext(request)"));
    }

    @Test
    void recordsCenterSharesLegacyBrowserHistoryAndPublicReadApi() throws Exception {
        String runtime = read("resources/static/next/n26-records-center.js");
        String legacy = read("resources/static/assets/public.js");

        assertTrue(runtime.contains("const KEY = 'xh_client_records_v1'"));
        assertTrue(legacy.contains("const RECORDS_KEY = 'xh_client_records_v1'"));
        assertTrue(runtime.contains("localStorage.getItem(KEY)"));
        assertTrue(runtime.contains("localStorage.setItem(KEY"));
        assertTrue(runtime.contains("localStorage.removeItem(KEY)"));
        assertTrue(runtime.contains("/api/records/${encodeURIComponent(businessNo)}"));
        assertTrue(runtime.contains("method: 'GET'"));
        assertTrue(runtime.contains("credentials: 'same-origin'"));
        assertTrue(runtime.contains("cache: 'no-store'"));
        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));
    }

    @Test
    void nextQuoteAndRecordDetailFeedSharedHistory() throws Exception {
        String bridge = read("resources/static/next/n26-record-history-bridge.js");
        String routes = read("resources/static/next/n14-route-continuity.js");

        assertTrue(bridge.contains("const KEY = 'xh_client_records_v1'"));
        assertTrue(bridge.contains("a[href^=\"/next/record/\"]"));
        assertTrue(bridge.contains("remember(decodeURIComponent(match[1]), 'QUOTE')"));
        assertTrue(bridge.contains("link.href = '/next/records'"));
        assertTrue(bridge.contains("link.dataset.n26RecordsCenter = '1'"));
        assertTrue(routes.contains("stageSource('/next/n26-record-history-bridge.js')"));
        assertTrue(routes.contains("/^\\/next\\/(?:quote\\/?$|record\\/)/"));
    }

    @Test
    void recordsLinksStayInsideNextAndLegacyEscapeRemainsExplicit() throws Exception {
        String runtime = read("resources/static/next/n26-records-center.js");
        String routes = read("resources/static/next/n14-route-continuity.js");

        assertTrue(runtime.contains("legacy.href = '/records'"));
        assertTrue(runtime.contains("legacy.dataset.n26Legacy = '1'"));
        assertTrue(runtime.contains("location.href = `/next/record/${encodeURIComponent(businessNo)}`"));
        assertTrue(runtime.contains("canonical.href = 'https://wydlgf.cn/records'"));
        assertTrue(runtime.contains("robots.content = 'noindex,nofollow'"));
        assertTrue(routes.contains("url.pathname === '/records'"));
        assertTrue(routes.contains("nextUrl(url, '/next/records')"));
        assertTrue(routes.contains("stageSource('/next/n26-records-center.js')"));
        assertTrue(routes.contains("link.dataset.n26Legacy === '1'"));
    }

    @Test
    void stagedRuntimesUseTheirOwnManifestFingerprint() throws Exception {
        String routes = read("resources/static/next/n14-route-continuity.js");
        assertTrue(routes.contains("document.currentScript?.dataset.stageVersions"));
        assertTrue(routes.contains("STAGE_RUNTIME_VERSIONS[path]"));
        assertTrue(routes.contains("/^[0-9a-f]{64}$/"));
        assertTrue(routes.contains("`${path}?v=${version}`"));
    }
}
