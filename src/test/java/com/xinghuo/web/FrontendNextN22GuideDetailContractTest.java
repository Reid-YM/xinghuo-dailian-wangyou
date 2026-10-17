package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN22GuideDetailContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerExposesGuideDetailAndFingerprintsN22() throws Exception {
        String preview = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        assertTrue(preview.contains("N22_RUNTIME"));
        assertTrue(preview.contains("/next/n22-guide-detail.js"));
        assertTrue(preview.contains("\"/next/guides/{slug}\""));
        assertTrue(preview.contains("html.contains(N22_RUNTIME)"));
        assertTrue(preview.contains("next/n22-guide-detail.js"));
        assertTrue(preview.contains("path.startsWith(\"/guides/\") ? \"攻略详情 · \" + brand"));
        assertTrue(preview.contains("path.startsWith(\"/service/\") || path.startsWith(\"/guides/\")"));
    }

    @Test
    void routeContinuityKeepsGuideDetailsInsideNextUnlessExplicitLegacy() throws Exception {
        String n14 = read("resources/static/next/n14-route-continuity.js");
        String n21 = read("resources/static/next/n21-account-personal-assets.js");

        assertTrue(n14.contains("const guide = /^\\/guides\\/([^/?#]+)\\/?$/.exec(url.pathname)"));
        assertTrue(n14.contains("`/next/guides/${guide[1]}`"));
        assertTrue(n14.contains("link.dataset.n22Legacy === '1'"));
        assertFalse(n21.contains("/^\\/guides\\//.test(raw)"));
        assertTrue(n21.contains("raw === '/mastery' || raw === '/account/submit'"));
        assertTrue(n21.contains("攻略详情由 N22 留在 Next 预览链路"));
    }

    @Test
    void guideDetailReadsOnlyExistingPublicGuideApi() throws Exception {
        String runtime = read("resources/static/next/n22-guide-detail.js");

        assertTrue(runtime.contains("DETAIL_RE = /^\\/next\\/guides\\/([^/?#]+)\\/?$/"));
        assertTrue(runtime.contains("getJson(`/api/guides/${encodeURIComponent(slug)}`)"));
        assertTrue(runtime.contains("headers: { Accept: 'application/json' }"));
        assertTrue(runtime.contains("credentials: 'same-origin'"));
        assertTrue(runtime.contains("cache: 'no-store'"));
        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
    }

    @Test
    void guideContentUsesSafeDomAndNextRelatedLinks() throws Exception {
        String runtime = read("resources/static/next/n22-guide-detail.js");

        assertTrue(runtime.contains("element.textContent = String(value)"));
        assertTrue(runtime.contains("appendMarkdownText"));
        assertFalse(runtime.contains("innerHTML"));
        assertTrue(runtime.contains("`/next/service/${encodeURIComponent(slug)}`"));
        assertTrue(runtime.contains("`/next/quote?service=${encodeURIComponent(slug)}`"));
        assertTrue(runtime.contains("`/next/guides/${encodeURIComponent(slug)}`"));
        assertTrue(runtime.contains("legacy.dataset.n22Legacy = '1'"));
        assertTrue(runtime.contains("canonical.href = `https://wydlgf.cn/guides/${encodeURIComponent(slug)}`"));
        assertTrue(runtime.contains("robots.content = 'noindex,follow'"));
    }

    @Test
    void loadingAndRetryIgnoreStaleResponses() throws Exception {
        String runtime = read("resources/static/next/n22-guide-detail.js");

        assertTrue(runtime.contains("const request = ++routeRequest"));
        assertTrue(runtime.contains("request !== routeRequest || detailRoute() !== slug"));
        assertTrue(runtime.contains("data-n22-retry"));
        assertTrue(runtime.contains("window.addEventListener('popstate', syncRoute)"));
    }
}
