package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN21AccountPersonalAssetsContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerExposesAssetRoutesAndFingerprintsN21() throws Exception {
        String preview = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        assertTrue(preview.contains("N21_RUNTIME"));
        assertTrue(preview.contains("/next/n21-account-personal-assets.js"));
        assertTrue(preview.contains("\"/next/account/favorites\""));
        assertTrue(preview.contains("\"/next/account/submissions\""));
        assertTrue(preview.contains("html.contains(N21_RUNTIME)"));
        assertTrue(preview.contains("next/n21-account-personal-assets.js"));
    }

    @Test
    void n16DeepLinksFocusExistingN5AssetSections() throws Exception {
        String n16 = read("resources/static/next/n16-account-deep-links.js");

        assertTrue(n16.contains("['favorites', { canonical: 'favorites', focus: 'favorites' }]"));
        assertTrue(n16.contains("['submissions', { canonical: 'submissions', focus: 'submissions' }]"));
        assertTrue(n16.contains("[data-personal-asset=\"favorites\"]"));
        assertTrue(n16.contains("[data-personal-asset=\"submissions\"]"));
        assertTrue(n16.contains("ensureGuestReturnParam"));
    }

    @Test
    void assetNavigationUsesExistingN5SectionsAndHistory() throws Exception {
        String runtime = read("resources/static/next/n21-account-personal-assets.js");
        String n17 = read("resources/static/next/n17-account-navigation.js");

        assertTrue(runtime.contains("['progress', { label: '工具存档', paged: false }]"));
        assertTrue(runtime.contains("['favorites', { label: '我的收藏', paged: true }]"));
        assertTrue(runtime.contains("['submissions', { label: '我的投稿', paged: true }]"));
        assertTrue(runtime.contains("#n5PersonalAssets"));
        assertTrue(runtime.contains("data-personal-asset"));
        assertTrue(runtime.contains("navigation.pushState"));
        assertTrue(runtime.contains("window.XHFrontendNextN16?.applyRoute"));
        assertTrue(n17.contains("['favorites', 'submissions'].includes(child)"));
    }

    @Test
    void paginationRestoresUrlByDrivingExistingPagerInsteadOfDuplicatingApiCalls() throws Exception {
        String runtime = read("resources/static/next/n21-account-personal-assets.js");

        assertTrue(runtime.contains("safePage"));
        assertTrue(runtime.contains("PAGE_MAX = 1000"));
        assertTrue(runtime.contains("new URLSearchParams(location.search).get('page')"));
        assertTrue(runtime.contains("pager.next.click()"));
        assertTrue(runtime.contains("pager.previous.click()"));
        assertTrue(runtime.contains("writePage(key, pager.page, 'replace')"));
        assertTrue(runtime.contains("queueMicrotask(() => writePage"));
        assertTrue(runtime.contains("window.addEventListener('popstate'"));
        assertFalse(runtime.contains("/api/account/favorites"));
        assertFalse(runtime.contains("/api/account/submissions"));
    }

    @Test
    void supportedAssetTargetsUseTheSharedRouteNamespaceWithoutObsoleteLegacyMarkers() throws Exception {
        String runtime = read("resources/static/next/n21-account-personal-assets.js");

        assertTrue(runtime.contains("normalizeAssetLinks"));
        assertFalse(runtime.contains("/^\\/guides\\//.test(raw)"));
        assertFalse(runtime.contains("link.dataset.n14Legacy = '1'"));
        assertTrue(runtime.contains("window.XHFrontendNextN14?.normalize?.(root)"));
        assertFalse(runtime.contains("a[href^=\"/game/\"]"));
    }

    @Test
    void n21AddsNoNetworkStorageOrBusinessWrites() throws Exception {
        String runtime = read("resources/static/next/n21-account-personal-assets.js");

        assertFalse(runtime.contains("fetch("));
        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
        assertFalse(runtime.contains("/api/payment"));
        assertFalse(runtime.contains("/api/refund"));
    }
}
