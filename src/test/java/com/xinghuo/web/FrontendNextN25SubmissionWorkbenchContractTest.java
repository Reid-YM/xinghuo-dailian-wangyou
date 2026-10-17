package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN25SubmissionWorkbenchContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void explicitPreviewRouteDelegatesToSharedNextDocument() throws Exception {
        String controller = read("java/com/xinghuo/web/FrontendNextN25SubmissionController.java");
        assertTrue(controller.contains("@GetMapping(\"/next/account/submit\")"));
        assertTrue(controller.contains("preview.frontendNext(request)"));
    }

    @Test
    void workbenchReusesRealSubmissionApisAndLegacyDraftKey() throws Exception {
        String runtime = read("resources/static/next/n25-submission-workbench.js");
        String legacy = read("resources/static/assets/account-submit.js");

        assertTrue(runtime.contains("const DRAFT_KEY = 'xinghuo:submission-draft:v1'"));
        assertTrue(legacy.contains("const draftKey = 'xinghuo:submission-draft:v1'"));
        assertTrue(runtime.contains("api('/api/account/submissions?size=50')"));
        assertTrue(runtime.contains("api('/api/account/submissions', { method: 'POST', body })"));
        assertTrue(runtime.contains("/api/account/submissions/${encodeURIComponent(id)}/withdraw"));
        assertTrue(runtime.contains("/api/account/submissions/${encodeURIComponent(item.id)}"));
        assertTrue(runtime.contains("'X-Requested-With': 'fetch'"));
        assertTrue(runtime.contains("localStorage.setItem(DRAFT_KEY"));
        assertTrue(runtime.contains("localStorage.getItem(DRAFT_KEY)"));
        assertTrue(runtime.contains("localStorage.removeItem(DRAFT_KEY)"));
    }

    @Test
    void previewIsTextOnlyAndDoesNotInventSubmittedEditApi() throws Exception {
        String runtime = read("resources/static/next/n25-submission-workbench.js");
        assertTrue(runtime.contains("renderMarkdown"));
        assertTrue(runtime.contains("host.append(node('p'"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertTrue(runtime.contains("当前后端只支持新建投稿，不伪造“修改已提交稿件”"));
    }

    @Test
    void accountSubmitLinksStayInsideNextButLegacyEscapeRemainsExplicit() throws Exception {
        String runtime = read("resources/static/next/n25-submission-workbench.js");
        String routes = read("resources/static/next/n14-route-continuity.js");

        assertTrue(runtime.contains("legacy.href = '/account/submit'"));
        assertTrue(runtime.contains("legacy.dataset.n25Legacy = '1'"));
        assertTrue(runtime.contains("published.href = `/next/guides/${encodeURIComponent(item.publishedSlug)}`"));
        assertTrue(runtime.contains("canonical.href = 'https://wydlgf.cn/account/submit'"));
        assertTrue(runtime.contains("robots.content = 'noindex,nofollow'"));

        assertTrue(routes.contains("url.pathname === '/account/submit'"));
        assertTrue(routes.contains("nextUrl(url, '/next/account/submit')"));
        assertTrue(routes.contains("/next/n25-submission-workbench.js?v=20261017-n25"));
        assertTrue(routes.contains("url.pathname === '/mastery' || url.pathname === '/account/submit'"));
        assertTrue(routes.contains("link.dataset.n25Legacy === '1'"));
    }
}
