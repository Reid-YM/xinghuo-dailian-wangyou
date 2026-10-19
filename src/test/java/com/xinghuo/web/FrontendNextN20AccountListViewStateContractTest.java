package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN20AccountListViewStateContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerInjectsAndFingerprintsN20Runtime() throws Exception {
        String preview = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        assertTrue(preview.contains("N20_RUNTIME"));
        assertTrue(preview.contains("/next/n20-account-list-view-state.js"));
        assertTrue(preview.contains("html.contains(N20_RUNTIME)"));
        assertTrue(preview.contains("next/n20-account-list-view-state.js"));
    }

    @Test
    void listViewStateUsesOnlyAlreadyLoadedN5Records() throws Exception {
        String runtime = read("resources/static/next/n20-account-list-view-state.js");

        assertTrue(runtime.contains("state.quotes?.items"));
        assertTrue(runtime.contains("state.orders?.items"));
        assertTrue(runtime.contains("function filteredItems"));
        assertTrue(runtime.contains("function visibleBusinessNos"));
        assertTrue(runtime.contains("当前已加载"));
        assertTrue(runtime.contains("最多显示最近 50 条"));
        assertTrue(runtime.contains("当前筛选条件没有匹配记录"));
        assertFalse(runtime.contains("?page="));
        assertFalse(runtime.contains("&page="));
    }

    @Test
    void searchStatusAndSortAreShareableSafeUrlState() throws Exception {
        String runtime = read("resources/static/next/n20-account-list-view-state.js");

        assertTrue(runtime.contains("searchParams.get('q')"));
        assertTrue(runtime.contains("searchParams.get('status')"));
        assertTrue(runtime.contains("searchParams.get('sort')"));
        assertTrue(runtime.contains("slice(0, 60)"));
        assertTrue(runtime.contains("STATUS_RE"));
        assertTrue(runtime.contains("updated-desc"));
        assertTrue(runtime.contains("updated-asc"));
        assertTrue(runtime.contains("navigation.replaceState"));
        assertTrue(runtime.contains("statusSignature"));
        assertTrue(runtime.contains("MutationObserver"));
    }

    @Test
    void filteredOrderFeedsN19PreviousAndNextContext() throws Exception {
        String n19 = read("resources/static/next/n19-account-detail-context.js");

        assertTrue(n19.contains("window.XHFrontendNextN20?.visibleBusinessNos?.(info)"));
        assertTrue(n19.contains("Array.isArray(filtered)"));
        assertTrue(n19.contains("previous:"));
        assertTrue(n19.contains("next:"));
        assertTrue(n19.contains("context.ids.join('|')"));
    }

    @Test
    void publicRecordReturnKeepsOnlyWhitelistedAccountViewParameters() throws Exception {
        String runtime = read("resources/static/next/n20-account-list-view-state.js");
        String record = read("resources/static/next/n13-record-detail.js");

        assertTrue(runtime.contains("function appendViewState"));
        assertTrue(runtime.contains("/^\\/next\\/record\\//"));
        assertTrue(record.contains("function appendAccountViewState"));
        assertTrue(record.contains("from === 'account-requests'"));
        assertTrue(record.contains("from === 'account-orders'"));
        assertTrue(record.contains("STATUS_RE.test(status)"));
        assertTrue(record.contains("SORTS.has(sort)"));
        assertTrue(record.contains("slice(0, 60)"));
    }

    @Test
    void n20AddsNoNetworkStorageOrBusinessWrites() throws Exception {
        String runtime = read("resources/static/next/n20-account-list-view-state.js");

        assertFalse(runtime.contains("fetch("));
        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
        assertFalse(runtime.contains("/api/console"));
        assertFalse(runtime.contains("/api/payment"));
        assertFalse(runtime.contains("/api/refund"));
    }
}
