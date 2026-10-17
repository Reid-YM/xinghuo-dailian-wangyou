package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN24AccountSubmissionContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerExposesSubmissionConsoleAndFingerprintsRuntime() throws Exception {
        String preview = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        assertTrue(preview.contains("\"/next/account/submit\""));
        assertTrue(preview.contains("N24_RUNTIME"));
        assertTrue(preview.contains("/next/n24-account-submission.js"));
        assertTrue(preview.contains("\"next/n24-account-submission.js\""));
        assertTrue(preview.contains("path.startsWith(\"/account\")"));
        assertTrue(preview.contains("noindex,nofollow"));
    }

    @Test
    void submissionConsoleReusesExistingAccountApisAndSecurityContract() throws Exception {
        String runtime = read("resources/static/next/n24-account-submission.js");
        String api = read("java/com/xinghuo/account/web/AccountSubmissionApiController.java");

        assertTrue(runtime.contains("const LIST_API = '/api/account/submissions?size=50'"));
        assertTrue(runtime.contains("const CREATE_API = '/api/account/submissions'"));
        assertTrue(runtime.contains("request('/api/account/me')"));
        assertTrue(runtime.contains("request('/api/catalog')"));
        assertTrue(runtime.contains("/withdraw`"));
        assertTrue(runtime.contains("'X-Requested-With': 'fetch'"));
        assertTrue(runtime.contains("credentials: 'same-origin'"));
        assertTrue(runtime.contains("method: 'POST'"));

        assertTrue(api.contains("@RequestMapping(\"/api/account/submissions\")"));
        assertTrue(api.contains("@PostMapping"));
        assertTrue(api.contains("@PostMapping(\"/{id}/withdraw\")"));
        assertTrue(api.contains("return submissions.listMine(account.getId(), page, size)"));
        assertTrue(api.contains("submissions.create(account"));
        assertTrue(api.contains("submissions.withdraw(account.getId(), id, request)"));
    }

    @Test
    void nextConsoleMatchesRealCreateListWithdrawCapabilitiesWithoutFakeEditApi() throws Exception {
        String runtime = read("resources/static/next/n24-account-submission.js");
        String api = read("java/com/xinghuo/account/web/AccountSubmissionApiController.java");
        String dto = read("java/com/xinghuo/account/dto/AccountDtos.java");

        assertTrue(runtime.contains("title.maxLength = 180"));
        assertTrue(runtime.contains("summary.maxLength = 500"));
        assertTrue(runtime.contains("body: { gameCode, title, summary, contentMd, coverImage: null }"));
        assertTrue(runtime.contains("没有修改已提交投稿的更新接口"));
        assertTrue(dto.contains("@NotBlank @Size(max = 180) String title"));
        assertTrue(dto.contains("@Size(max = 500) String summary"));
        assertTrue(dto.contains("@NotBlank String contentMd"));

        assertFalse(api.contains("@PutMapping"));
        assertFalse(api.contains("@PatchMapping"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
    }

    @Test
    void publishedSubmissionStaysInNextAndLegacyEscapeIsExplicit() throws Exception {
        String runtime = read("resources/static/next/n24-account-submission.js");
        String continuity = read("resources/static/next/n14-route-continuity.js");

        assertTrue(runtime.contains("`/next/guides/${encodeURIComponent(text(item.publishedSlug))}`"));
        assertTrue(runtime.contains("legacy.href = '/account/submit?legacy=1'"));
        assertTrue(runtime.contains("legacy.dataset.n24Legacy = '1'"));
        assertTrue(runtime.contains("link.setAttribute('href', ROUTE)"));
        assertTrue(runtime.contains("delete link.dataset.n14Legacy"));
        assertTrue(runtime.contains("delete link.dataset.n21Legacy"));
        assertTrue(continuity.contains("link.dataset.n24Legacy === '1'"));
        assertTrue(continuity.contains("url.pathname === '/account/submit' || url.pathname === '/account/submit/'"));
        assertTrue(continuity.contains("nextUrl(url, '/next/account/submit')"));
    }

    @Test
    void guestReturnAndPreviewMetadataStayScopedToNext() throws Exception {
        String runtime = read("resources/static/next/n24-account-submission.js");

        assertTrue(runtime.contains("url.searchParams.set('next', ROUTE)"));
        assertTrue(runtime.contains("canonical.href = 'https://wydlgf.cn/account/submit'"));
        assertTrue(runtime.contains("robots.content = 'noindex,nofollow'"));
        assertTrue(runtime.contains("document.title = `攻略投稿控制台 · ${brand}`"));
        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));
    }
}
