package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiRuntimeResilienceContractTest {
    private static final Path JS = Path.of("src/main/resources/static/assets/admin-xingwei.js");

    @Test
    void runtimeHealthUsesTimeoutPollingAndLifecycleRechecks() throws Exception {
        String js = Files.readString(JS);
        assertTrue(js.contains("const REQUEST_TIMEOUT_MS = 8000"));
        assertTrue(js.contains("const HEALTH_CHECK_INTERVAL_MS = 60000"));
        assertTrue(js.contains("new AbortController()"));
        assertTrue(js.contains("controller.abort()"));
        assertTrue(js.contains("cache: 'no-store'"));
        assertTrue(js.contains("checkBackend('interval')"));
        assertTrue(js.contains("visibilitychange"));
        assertTrue(js.contains("checkBackend('visibility')"));
        assertTrue(js.contains("window.addEventListener('online'"));
        assertTrue(js.contains("window.addEventListener('offline'"));
        assertTrue(js.contains("healthInFlight"));
        assertTrue(js.contains("state = hasVerifiedSnapshot && healthFailures === 1 ? 'stale' : 'error'"));
        assertTrue(js.contains("网络已断开 · 等待恢复后复核"));
    }

    @Test
    void permissionSnapshotAlsoFailsClosedOnTimeout() throws Exception {
        String js = Files.readString(JS);
        assertTrue(js.contains("fetchWithTimeout('/api/admin/xingwei/access'"));
        assertTrue(js.contains("dataScope: 'NONE'"));
        assertTrue(js.contains("新版权限读取超时 · 受控模块已隐藏"));
        assertTrue(js.contains("badge.classList.add('error')"));
        assertTrue(js.contains("filterModules()"));
        assertTrue(js.contains("applyActionPermissions(document)"));
    }
}
