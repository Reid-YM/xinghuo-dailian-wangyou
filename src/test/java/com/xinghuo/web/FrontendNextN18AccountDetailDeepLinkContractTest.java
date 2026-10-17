package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN18AccountDetailDeepLinkContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerExposesAndFingerprintsAccountDetailRoutes() throws Exception {
        String preview = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        assertTrue(preview.contains("N18_RUNTIME"));
        assertTrue(preview.contains("/next/n18-account-detail-deep-links.js"));
        assertTrue(preview.contains("\"/next/account/requests/{businessNo}\""));
        assertTrue(preview.contains("\"/next/account/orders/{businessNo}\""));
        assertTrue(preview.contains("html.contains(N18_RUNTIME)"));
        assertTrue(preview.contains("next/n18-account-detail-deep-links.js"));
    }

    @Test
    void detailRuntimeUsesTheExistingBusinessNumberAllowlistAndN5Ui() throws Exception {
        String runtime = read("resources/static/next/n18-account-detail-deep-links.js");

        assertTrue(runtime.contains("/^(XH[A-Z0-9]{10}|XH-[A-Z2-9]{24})$/"));
        assertTrue(runtime.contains("#n5RecordsBody tr"));
        assertTrue(runtime.contains("#n5Detail"));
        assertTrue(runtime.contains("window.XHFrontendNextN5?.state?.detail"));
        assertTrue(runtime.contains("button.click()"));
        assertFalse(runtime.contains("fetch("));
        assertFalse(runtime.contains("/api/console/quotes/"));
        assertFalse(runtime.contains("/api/console/orders/"));
    }

    @Test
    void openingAndClosingDetailsSynchronizeBrowserHistory() throws Exception {
        String runtime = read("resources/static/next/n18-account-detail-deep-links.js");

        assertTrue(runtime.contains("/next/account/${section}/${encodeURIComponent(normalized)}"));
        assertTrue(runtime.contains("history.pushState"));
        assertTrue(runtime.contains("n18AccountDetail"));
        assertTrue(runtime.contains("pushParent(info)"));
        assertTrue(runtime.contains("window.addEventListener('popstate'"));
        assertTrue(runtime.contains("closeRenderedDetail()"));
    }

    @Test
    void directGuestDetailPreservesTheRequestedReturnAcrossAuthentication() throws Exception {
        String runtime = read("resources/static/next/n18-account-detail-deep-links.js");

        assertTrue(runtime.contains("#n4LoginForm"));
        assertTrue(runtime.contains("current.searchParams.set('next', target)"));
        assertTrue(runtime.contains("transientParams"));
        assertTrue(runtime.contains("'auth', 'token', 'verifyToken', 'wechat', 'next', 'legacy'"));
        assertTrue(runtime.contains("history.replaceState"));
    }

    @Test
    void detailRouteReusesN16TabSynchronizationWithoutAddingHistoryEntries() throws Exception {
        String runtime = read("resources/static/next/n18-account-detail-deep-links.js");

        assertTrue(runtime.contains("history.replaceState(history.state, '', parent)"));
        assertTrue(runtime.contains("window.XHFrontendNextN16?.applyRoute?.({ canonicalize: false })"));
        assertTrue(runtime.contains("history.replaceState(history.state, '', original)"));
    }

    @Test
    void accountNavigationKeepsDetailRoutesInTheirParentSection() throws Exception {
        String navigation = read("resources/static/next/n17-account-navigation.js");

        assertTrue(navigation.contains("child.startsWith('requests/')"));
        assertTrue(navigation.contains("child.startsWith('orders/')"));
    }

    @Test
    void n18DoesNotIntroduceClientStorageUnsafeHtmlOrBusinessWrites() throws Exception {
        String runtime = read("resources/static/next/n18-account-detail-deep-links.js");

        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));
        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
        assertFalse(runtime.contains("/api/payment"));
        assertFalse(runtime.contains("/api/refund"));
    }
}
