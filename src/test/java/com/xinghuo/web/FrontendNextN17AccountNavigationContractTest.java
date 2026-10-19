package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN17AccountNavigationContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerInjectsAndFingerprintsN17Runtime() throws Exception {
        String preview = read("java/com/xinghuo/web/FrontendNextPreviewController.java");
        assertTrue(preview.contains("N17_RUNTIME"));
        assertTrue(preview.contains("/next/n17-account-navigation.js"));
        assertTrue(preview.contains("html.contains(N17_RUNTIME)"));
        assertTrue(preview.contains("next/n17-account-navigation.js"));
    }

    @Test
    void navigationUsesCanonicalN16AccountRoutes() throws Exception {
        String runtime = read("resources/static/next/n17-account-navigation.js");
        assertTrue(runtime.contains("['overview', '总览', '/next/account']"));
        assertTrue(runtime.contains("['requests', '询价记录', '/next/account/requests']"));
        assertTrue(runtime.contains("['orders', '订单记录', '/next/account/orders']"));
        assertTrue(runtime.contains("['progress', '工具存档', '/next/account/progress']"));
        assertTrue(runtime.contains("['settings', '账号设置', '/next/account/settings']"));
        assertTrue(runtime.contains("window.XHFrontendNextN16?.applyRoute?.()"));
    }

    @Test
    void aliasesResolveToTheCorrectActiveNavigationItem() throws Exception {
        String runtime = read("resources/static/next/n17-account-navigation.js");
        assertTrue(runtime.contains("['requests', 'quotes', 'demands']"));
        assertTrue(runtime.contains("['orders', 'order', 'order-list']"));
        assertTrue(runtime.contains("['progress', 'tools', 'tool-progress']"));
        assertTrue(runtime.contains("child.startsWith('settings/')"));
        assertTrue(runtime.contains("aria-current"));
    }

    @Test
    void navigationIsMountedOnlyForAuthenticatedAccountShellAndSupportsHistory() throws Exception {
        String runtime = read("resources/static/next/n17-account-navigation.js");
        assertTrue(runtime.contains("document.querySelector('#n4LoginForm')"));
        assertTrue(runtime.contains("document.querySelector('.page[data-page=\"account\"] > .n4-shell')"));
        assertTrue(runtime.contains("navigation.pushState"));
        assertTrue(runtime.contains("window.addEventListener('popstate'"));
        assertTrue(runtime.contains("new MutationObserver"));
        assertTrue(runtime.contains("existing?.remove()"));
    }

    @Test
    void n17IsNavigationOnly() throws Exception {
        String runtime = read("resources/static/next/n17-account-navigation.js");
        assertFalse(runtime.contains("fetch("));
        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));
        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
        assertFalse(runtime.contains("/api/payment"));
        assertFalse(runtime.contains("/api/refund"));
    }
}
