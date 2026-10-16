package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN15AuthReturnContinuityContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerInjectsAndFingerprintsN15Runtime() throws Exception {
        String preview = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        assertTrue(preview.contains("N15_RUNTIME"));
        assertTrue(preview.contains("/next/n15-auth-return.js"));
        assertTrue(preview.contains("html.contains(N15_RUNTIME)"));
        assertTrue(preview.contains("next/n15-auth-return.js"));
    }

    @Test
    void authReturnNormalizesKnownLegacyRoutesButPreservesExplicitLegacyEscape() throws Exception {
        String runtime = read("resources/static/next/n15-auth-return.js");

        assertTrue(runtime.contains("url.pathname === '/services'"));
        assertTrue(runtime.contains("url.pathname = '/next/services'"));
        assertTrue(runtime.contains("url.pathname === '/quote'"));
        assertTrue(runtime.contains("url.pathname = '/next/quote'"));
        assertTrue(runtime.contains("url.pathname === '/account'"));
        assertTrue(runtime.contains("url.pathname = '/next/account'"));
        assertTrue(runtime.contains("/^\\/service\\/[^/]+\\/?$/"));
        assertTrue(runtime.contains("/^\\/record\\/[^/]+\\/?$/"));
        assertTrue(runtime.contains("url.searchParams.get('legacy') !== '1'"));
        assertTrue(runtime.contains("/^\\/(admin|api|logout)(\\/|$)/i"));
        assertTrue(runtime.contains("/^\\/account\\/(login|register|reset)(\\/|$)/i"));
    }

    @Test
    void passwordRegistrationOauthAndMiniLoginShareTheSameRequestedReturn() throws Exception {
        String runtime = read("resources/static/next/n15-auth-return.js");
        String account = read("resources/static/next/n4.js");
        String auth = read("resources/static/next/n7-auth.js");

        assertTrue(runtime.contains("shell.safeAccountReturn = safeAccountReturn"));
        assertTrue(account.contains("shell.safeAccountReturn(new URLSearchParams(location.search).get('next'))"));
        assertTrue(auth.contains("safeAccountReturn(new URLSearchParams(location.search).get('next'))"));
        assertTrue(runtime.contains("/api/account/wechat/start?next="));
        assertTrue(runtime.contains("/api/account/wechat-mini/complete"));
        assertTrue(runtime.contains("location.assign(requestedReturn())"));
        assertTrue(runtime.contains("event.stopImmediatePropagation()"));
    }

    @Test
    void oauthFailuresStayInNextOnlyWhenTheFlowOriginatedFromNext() throws Exception {
        String controller = read("java/com/xinghuo/account/web/AccountWeChatController.java");

        assertTrue(controller.contains("redirectLogin(response, \"unavailable\", next)"));
        assertTrue(controller.contains("redirectLogin(response, \"rate\", next)"));
        assertTrue(controller.contains("redirectLogin(response, \"cancelled\", flow.returnTo())"));
        assertTrue(controller.contains("redirectLogin(response, \"failed\", flow.returnTo())"));
        assertTrue(controller.contains("\"/next\".equals(returnTo) || returnTo.startsWith(\"/next/\")"));
        assertTrue(controller.contains("/next/account?wechat="));
        assertTrue(controller.contains("/account/login?wechat="));
        assertTrue(controller.contains("URLEncoder.encode(returnTo, StandardCharsets.UTF_8)"));
    }

    @Test
    void n15DoesNotIntroduceClientStorageOrBroadBusinessWrites() throws Exception {
        String runtime = read("resources/static/next/n15-auth-return.js");

        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
        assertFalse(runtime.contains("/api/payment"));
        assertFalse(runtime.contains("/api/refund"));
    }
}
