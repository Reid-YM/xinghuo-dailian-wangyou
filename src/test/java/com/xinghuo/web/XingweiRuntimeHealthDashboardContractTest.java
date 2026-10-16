package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiRuntimeHealthDashboardContractTest {
    private static final Path TEMPLATE = Path.of("src/main/resources/templates/admin-xingwei-releases.html");
    private static final Path JS = Path.of("src/main/resources/static/assets/admin-xingwei-runtime-health.js");
    private static final Path CSS = Path.of("src/main/resources/static/assets/admin-xingwei-runtime-health.css");

    @Test
    void deliveryPageExposesReadOnlyRuntimeHealthDashboard() throws Exception {
        String html = Files.readString(TEMPLATE);
        assertTrue(html.contains("id=\"xwrRuntimeHealth\""));
        assertTrue(html.contains("id=\"xwrRuntimeDatabase\""));
        assertTrue(html.contains("id=\"xwrRuntimeUptime\""));
        assertTrue(html.contains("id=\"xwrRuntimeHeap\""));
        assertTrue(html.contains("id=\"xwrRuntimeRequests\""));
        assertTrue(html.contains("id=\"xwrRuntime4xx\""));
        assertTrue(html.contains("id=\"xwrRuntime5xx\""));
        assertTrue(html.contains("id=\"xwrRuntimeLastServerError\""));
        assertTrue(html.contains("admin-xingwei-runtime-health.css?v=20261017v1"));
        assertTrue(html.contains("admin-xingwei-runtime-health.js?v=20261017v1"));
        assertTrue(html.contains("应用运行真值只来自当前 JVM"));
        assertTrue(html.contains("不能证明 Nginx upstream"));
        assertTrue(html.contains("生产主机发布真值"));
        assertTrue(html.contains("应用本身不能生成“发布成功”结论"));
    }

    @Test
    void dashboardUsesProtectedNoStoreApiAndLifecycleRefresh() throws Exception {
        String js = Files.readString(JS);
        assertTrue(js.contains("const API = '/api/admin/xingwei/runtime-health'"));
        assertTrue(js.contains("credentials: 'same-origin'"));
        assertTrue(js.contains("cache: 'no-store'"));
        assertTrue(js.contains("'Cache-Control':'no-cache'"));
        assertTrue(js.contains("const REQUEST_TIMEOUT_MS = 8000"));
        assertTrue(js.contains("const REFRESH_INTERVAL_MS = 60000"));
        assertTrue(js.contains("status === 403"));
        assertTrue(js.contains("没有 deploy:view 权限"));
        assertTrue(js.contains("visibilitychange"));
        assertTrue(js.contains("window.addEventListener('online'"));
        assertTrue(js.contains("window.addEventListener('offline'"));
        assertTrue(js.contains("refreshButton?.addEventListener('click'"));
        assertFalse(js.contains("requestBody"));
        assertFalse(js.contains("businessNo"));
        assertFalse(js.contains("clientIp"));
    }

    @Test
    void dashboardRemainsResponsiveAndDistinguishesWarningFromFailure() throws Exception {
        String css = Files.readString(CSS);
        assertTrue(css.contains(".xwr-health-actions .xw-status.warning"));
        assertTrue(css.contains(".xwr-health-card[data-tone=\"error\"]"));
        assertTrue(css.contains("@media(max-width:620px)"));
        assertTrue(css.contains("grid-template-columns:1fr"));
    }
}
