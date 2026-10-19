package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN19AccountDetailContextContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerInjectsAndFingerprintsN19Runtime() throws Exception {
        String preview = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        assertTrue(preview.contains("N19_RUNTIME"));
        assertTrue(preview.contains("/next/n19-account-detail-context.js"));
        assertTrue(preview.contains("html.contains(N19_RUNTIME)"));
        assertTrue(preview.contains("next/n19-account-detail-context.js"));
    }

    @Test
    void previousAndNextStayInsideAlreadyLoadedAccountLists() throws Exception {
        String runtime = read("resources/static/next/n19-account-detail-context.js");

        assertTrue(runtime.contains("state.quotes?.items"));
        assertTrue(runtime.contains("state.orders?.items"));
        assertTrue(runtime.contains("previous:"));
        assertTrue(runtime.contains("next:"));
        assertTrue(runtime.contains("当前已加载列表第"));
        assertTrue(runtime.contains("context.ids.join('|')"));
        assertTrue(runtime.contains("dataset.n19Signature"));
        assertTrue(runtime.contains("window.XHFrontendNextN18?.detailUrl"));
        assertTrue(runtime.contains("navigation.pushState"));
    }

    @Test
    void orderCanReturnToLoadedSourceQuoteAndOpenPublicRecordWithStrictOriginMarker() throws Exception {
        String runtime = read("resources/static/next/n19-account-detail-context.js");

        assertTrue(runtime.contains("detail?.sourceQuote?.businessNo"));
        assertTrue(runtime.contains("loadedQuote(source)"));
        assertTrue(runtime.contains("来源询价"));
        assertTrue(runtime.contains("/next/record/${encodeURIComponent(info.businessNo)}"));
        assertTrue(runtime.contains("record.searchParams.set('from'"));
        assertTrue(runtime.contains("account-requests"));
        assertTrue(runtime.contains("account-orders"));
    }

    @Test
    void recordPreviewOnlyAcceptsKnownAccountReturnOrigins() throws Exception {
        String record = read("resources/static/next/n13-record-detail.js");

        assertTrue(record.contains("function accountReturnTarget(businessNo)"));
        assertTrue(record.contains("from === 'account-requests'"));
        assertTrue(record.contains("from === 'account-orders'"));
        assertTrue(record.contains("if (!section) return ''"));
        assertTrue(record.contains("/next/account/${section}/${encodeURIComponent(normalized)}"));
        assertTrue(record.contains("data-n19-account-return") || record.contains("dataset.n19AccountReturn"));
        assertTrue(record.contains("返回询价详情"));
        assertTrue(record.contains("返回订单详情"));
    }

    @Test
    void n19DoesNotAddNetworkStorageOrBusinessWrites() throws Exception {
        String runtime = read("resources/static/next/n19-account-detail-context.js");

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
