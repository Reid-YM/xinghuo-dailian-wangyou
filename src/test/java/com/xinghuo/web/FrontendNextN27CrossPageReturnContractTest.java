package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN27CrossPageReturnContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerInjectsAndFingerprintsN27Runtime() throws Exception {
        String preview = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        assertTrue(preview.contains("N27_RUNTIME"));
        assertTrue(preview.contains("/next/n27-cross-page-return.js"));
        assertTrue(preview.contains("html.contains(N27_RUNTIME)"));
        assertTrue(preview.contains("\"next/n27-cross-page-return.js\""));
    }

    @Test
    void migratedPublicReturnTargetsStayInsideNext() throws Exception {
        String runtime = read("resources/static/next/n27-cross-page-return.js");

        assertTrue(runtime.contains("['/services', '/next/services']"));
        assertTrue(runtime.contains("['/quote', '/next/quote']"));
        assertTrue(runtime.contains("['/guides', '/next/guides']"));
        assertTrue(runtime.contains("['/mastery', '/next/mastery']"));
        assertTrue(runtime.contains("['/records', '/next/records']"));
        assertTrue(runtime.contains("['/contact', '/next/contact']"));
        assertTrue(runtime.contains("['/trust', '/next/trust']"));
        assertTrue(runtime.contains("['/account/submit', '/next/account/submit']"));
        assertTrue(runtime.contains("/^\\/service\\/[^/]+$/"));
        assertTrue(runtime.contains("/^\\/guides\\/[^/]+$/"));
        assertTrue(runtime.contains("/^\\/record\\/[^/]+$/"));
    }

    @Test
    void accountDeepLinksAndBusinessDetailsRemainInNextAfterAuthentication() throws Exception {
        String runtime = read("resources/static/next/n27-cross-page-return.js");

        assertTrue(runtime.contains("'favorites','submissions','submit','settings','profile','settings/profile'"));
        assertTrue(runtime.contains("'security','settings/security','devices','activity'"));
        assertTrue(runtime.contains("/^\\/account\\/(requests|orders)\\/[^/]+$/"));
        assertTrue(runtime.contains("if (account && accountPaths.has(account[1])) return '/next' + path"));
    }

    @Test
    void currentNextParameterAndNestedAuthLinksAreNormalizedBeforeLoginActions() throws Exception {
        String runtime = read("resources/static/next/n27-cross-page-return.js");

        assertTrue(runtime.contains("const raw = current.searchParams.get('next')"));
        assertTrue(runtime.contains("current.searchParams.set('next', normalized)"));
        assertTrue(runtime.contains("history.replaceState"));
        assertTrue(runtime.contains("url.searchParams.has('next')"));
        assertTrue(runtime.contains("/^\\/(?:next\\/account|account\\/(?:login|register|reset)|api\\/account\\/wechat\\/start)/"));
        assertTrue(runtime.contains("link.dataset.n27Return = '1'"));
        assertTrue(runtime.contains("shell.safeAccountReturn = (requested) =>"));
        assertTrue(runtime.contains("previous ? previous(normalized)"));
    }

    @Test
    void N27PreservesExplicitLegacyAndDoesNotAddStorageOrBusinessWrites() throws Exception {
        String runtime = read("resources/static/next/n27-cross-page-return.js");

        assertTrue(runtime.contains("url.searchParams.get('legacy') === '1'"));
        assertTrue(runtime.contains("!value.startsWith('/') || value.startsWith('//')"));
        assertTrue(runtime.contains("url.origin === location.origin"));
        assertFalse(runtime.contains("fetch("));
        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
    }
}
