package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN14RouteContinuityContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerInjectsAndFingerprintsN14Runtime() throws Exception {
        String preview = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        assertTrue(preview.contains("N14_RUNTIME"));
        assertTrue(preview.contains("/next/n14-route-continuity.js"));
        assertTrue(preview.contains("next/n14-route-continuity.js"));
        assertTrue(preview.contains("html.contains(N14_RUNTIME)"));
    }

    @Test
    void ordinaryLegacyRoutesAreNormalizedInsideNextWhileExplicitFallbacksStayLegacy() throws Exception {
        String runtime = read("resources/static/next/n14-route-continuity.js");

        assertTrue(runtime.contains("url.pathname === '/services'"));
        assertTrue(runtime.contains("nextUrl(url, '/next/services')"));
        assertTrue(runtime.contains("url.pathname === '/quote'"));
        assertTrue(runtime.contains("nextUrl(url, '/next/quote')"));
        assertTrue(runtime.contains("url.pathname === '/account'"));
        assertTrue(runtime.contains("nextUrl(url, '/next/account')"));
        assertTrue(runtime.contains("/^\\/service\\/([^/?#]+)\\/?$/"));
        assertTrue(runtime.contains("`/next/service/${service[1]}`"));
        assertTrue(runtime.contains("/^\\/record\\/([^/?#]+)\\/?$/"));
        assertTrue(runtime.contains("`/next/record/${record[1]}`"));

        assertTrue(runtime.contains("link.dataset.n10Legacy === '1'"));
        assertTrue(runtime.contains("link.dataset.n13Legacy === '1'"));
        assertTrue(runtime.contains("link.dataset.n14Legacy === '1'"));
        assertTrue(runtime.contains("url.searchParams.get('legacy') === '1'"));
    }

    @Test
    void homeGameCardsKeepTheIndependentGameProfileInNext() throws Exception {
        String runtime = read("resources/static/next/n14-route-continuity.js");
        String shell = read("resources/static/next/shell.js");
        assertTrue(shell.contains("game.seoSlug?'/game/'"));
        assertTrue(runtime.contains("`/next/game/${game[1]}`"));
        assertFalse(runtime.contains("gameHallTarget"));
    }

    @Test
    void continuityRuntimeIsReadOnlyAndCoversAsyncLinks() throws Exception {
        String runtime = read("resources/static/next/n14-route-continuity.js");

        assertTrue(runtime.contains("new MutationObserver"));
        assertTrue(runtime.contains("document.addEventListener('click'"));
        assertTrue(runtime.contains("url.origin === location.origin"));
        assertFalse(runtime.contains("fetch("));
        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));
        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
    }
}
