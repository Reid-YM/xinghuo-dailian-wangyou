package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class AccountSubmitBundlePlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void dedicatedSubmissionBundleKeepsPreviewDraftValidationAndRequestsAcrossBrowsers() {
        try (Playwright playwright = Playwright.create()) {
            verify(playwright.chromium().launch());
            verify(playwright.webkit().launch());
        }
    }

    private void verify(Browser browser) {
        try (browser) {
            Page page = browser.newPage();
            page.setContent("""
                    <body data-page="account-submit">
                      <strong data-submit-account-name>读取中…</strong>
                      <small data-submit-account-meta>读取中…</small>
                      <strong data-draft-state>草稿等待输入</strong>
                      <strong data-submission-active>读取中…</strong>
                      <strong data-submission-published>读取中…</strong>
                      <span data-submission-total>—</span>
                      <form id="accountSubmitForm">
                        <div class="field"><select id="submitGame" name="gameCode"><option value="">加载中…</option></select></div>
                        <div class="field"><input id="submitTitle" name="title"></div>
                        <div class="field"><input id="submitSummary" name="summary"></div>
                        <div class="field"><textarea id="submitContent" name="contentMd"></textarea></div>
                        <span data-submission-count>0</span>
                        <div data-submission-preview></div>
                        <div id="accountSubmitMsg"></div>
                        <button type="button" data-clear-draft>清空本机草稿</button>
                        <button type="submit" data-submit>提交人工审核</button>
                      </form>
                      <div id="accountSubmissionList"></div>
                      <button type="button" data-account-logout>安全退出</button>
                      <a href="#editor" data-console-anchor>编辑</a>
                      <section id="editor"></section>
                    </body>
                    """);

            installBrowserMocks(page);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("account-core.js")));
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("account-submit.js")));

            page.waitForFunction("() => document.querySelector('#submitGame').options.length === 2");
            assertEquals("测试作者", page.locator("[data-submit-account-name]").textContent());
            assertEquals("使命召唤 MW4", page.locator("#submitGame option").nth(1).textContent());
            assertTrue(page.locator("#accountSubmissionList").textContent().contains("测试投稿"));

            page.locator("#submitContent").fill("## 安全标题\n<img src=x onerror=window.__pwned=1>\n- 第一步");
            page.waitForTimeout(450);
            assertEquals(0, page.locator("[data-submission-preview] img").count());
            assertEquals(null, page.evaluate("() => window.__pwned || null"));
            assertTrue(page.locator("[data-submission-preview]").textContent().contains("<img src=x onerror=window.__pwned=1>"));
            assertTrue((Boolean) page.evaluate("() => JSON.parse(window.__drafts['xinghuo:submission-draft:v1']).contentMd.includes('安全标题')"));

            page.locator("#submitGame").selectOption("CODMW4");
            page.locator("[data-submit]").click();
            assertEquals("true", page.locator("#submitTitle").getAttribute("aria-invalid"));

            page.locator("#submitTitle").fill("测试攻略标题");
            page.locator("[data-submit]").click();
            page.waitForFunction("() => window.__requests.some(r => r.url === '/api/account/submissions' && r.method === 'POST')");
            assertEquals("fetch", page.evaluate("() => window.__requests.find(r => r.url === '/api/account/submissions' && r.method === 'POST').headers['X-Requested-With']"));

            page.locator("#accountSubmissionList button").click();
            assertTrue(page.locator("dialog.account-confirm").isVisible());
            page.locator("dialog.account-confirm .btn.danger").click();
            page.waitForFunction("() => window.__requests.some(r => r.url === '/api/account/submissions/7/withdraw' && r.method === 'POST')");
            assertEquals("fetch", page.evaluate("() => window.__requests.find(r => r.url === '/api/account/submissions/7/withdraw').headers['X-Requested-With']"));
        }
    }

    private void installBrowserMocks(Page page) {
        page.evaluate("""
                () => {
                  window.__drafts = {};
                  Object.defineProperty(window, 'localStorage', {
                    configurable: true,
                    value: {
                      getItem(key) { return Object.prototype.hasOwnProperty.call(window.__drafts, key) ? window.__drafts[key] : null; },
                      setItem(key, value) { window.__drafts[key] = String(value); },
                      removeItem(key) { delete window.__drafts[key]; }
                    }
                  });
                  window.__requests = [];
                  window.fetch = async (url, init = {}) => {
                    const method = String(init.method || 'GET').toUpperCase();
                    const headers = init.headers || {};
                    window.__requests.push({ url: String(url), method, headers, body: init.body || null });
                    let payload = {};
                    if (url === '/api/catalog') payload = { games: [{ code: 'CODMW4', name: '使命召唤 MW4' }] };
                    else if (url === '/api/account/me') payload = { displayName: '测试作者', emailVerified: true };
                    else if (url === '/api/account/submissions?size=50') payload = [{
                      id: 7, gameCode: 'CODMW4', title: '测试投稿', summary: '审核中的投稿', status: 'SUBMITTED', updatedAt: new Date().toISOString()
                    }];
                    else if (url === '/api/account/submissions' && method === 'POST') payload = { id: 8 };
                    else if (url === '/api/account/submissions/7/withdraw' && method === 'POST') payload = { ok: true };
                    return {
                      ok: true,
                      status: 200,
                      headers: { get(name) { return String(name).toLowerCase() === 'content-type' ? 'application/json' : null; } },
                      async json() { return payload; },
                      async text() { return JSON.stringify(payload); }
                    };
                  };
                }
                """);
    }
}
