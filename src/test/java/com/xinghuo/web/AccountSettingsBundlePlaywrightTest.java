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
class AccountSettingsBundlePlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void settingsRuntimeKeepsSingleProfileOwnerAndSecureWritesAcrossBrowsers() {
        try (Playwright playwright = Playwright.create()) {
            verify(playwright.chromium().launch());
            verify(playwright.webkit().launch());
        }
    }

    private void verify(Browser browser) {
        try (browser) {
            Page page = browser.newPage();
            page.setContent("""
                    <body data-page="account-settings">
                      <main id="accountSettings" data-settings-hub>
                        <div id="settingsMsg" hidden></div>
                        <strong data-settings-name>读取中…</strong>
                        <strong data-settings-contact>—</strong>
                        <strong data-settings-created>—</strong>
                        <strong data-settings-email>—</strong>
                        <span data-settings-email-state></span>
                        <strong data-settings-quote-contact>读取中…</strong>
                        <strong data-security-contact>读取中…</strong>
                        <strong data-security-email>读取中…</strong>
                        <strong data-security-device-count>读取中…</strong>
                        <div data-security-score data-level="pending"><strong data-security-score-value>读取中…</strong><small data-security-score-next></small></div>

                        <span class="account-avatar" data-profile-avatar-preview></span>
                        <form id="accountProfileFormV2">
                          <div class="field"><input name="displayName"></div>
                          <div class="field"><input name="avatarFile" type="file"></div>
                          <div data-profile-msg hidden></div>
                          <button type="submit" data-submit>保存资料</button>
                        </form>

                        <div id="quoteContactList"></div>
                        <form id="accountQuoteContactForm">
                          <select name="contactType"><option value="WECHAT">微信号</option></select>
                          <div class="field"><input name="contactValue"></div>
                          <button type="submit" data-submit>添加</button>
                        </form>
                        <div data-quote-contact-msg hidden></div>

                        <form id="accountPasswordForm">
                          <label class="field password-field"><input name="oldPassword" type="password"><button type="button" data-toggle-password>显示</button></label>
                          <label class="field password-field"><input name="newPassword" type="password"><button type="button" data-toggle-password>显示</button></label>
                          <div data-password-meter data-score="0"><i></i><span></span></div>
                          <label class="field password-field"><input name="newPasswordConfirm" type="password"><button type="button" data-toggle-password>显示</button></label>
                          <div data-password-msg hidden></div>
                          <button type="submit" data-submit>更新密码</button>
                        </form>

                        <form id="accountEmailForm">
                          <div class="field"><input id="settingsEmail" name="email" type="email"></div>
                          <button type="submit" data-submit>发送验证码</button>
                          <button type="button" data-resend-verify>重新发送</button>
                        </form>
                        <form id="accountEmailCodeForm">
                          <div class="field"><input name="code"></div>
                          <button type="submit" data-submit>确认验证</button>
                        </form>
                        <div data-email-msg hidden></div>
                        <div data-email-verify-panel hidden><div data-email-verify-result hidden></div></div>

                        <button type="button" data-session-refresh>刷新设备</button>
                        <button type="button" data-session-revoke-all>下线其他设备</button>
                        <div data-session-msg hidden></div>
                        <div data-session-list></div>

                        <button type="button" data-security-events-refresh>刷新安全记录</button>
                        <div id="securityEventsMsg" hidden></div>
                        <div data-security-events></div>

                        <button type="button" data-account-logout>退出登录</button>
                      </main>
                    </body>
                    """);

            installBrowserMocks(page);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("account-core.js")));
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("account-settings.js")));
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("account-profile-controller.js")));

            page.waitForFunction("() => document.querySelector('[name=displayName]').value === '旧昵称'");
            page.waitForFunction("() => document.querySelector('[data-security-device-count]').textContent.includes('2 台在线')");
            assertEquals(Boolean.TRUE, page.evaluate("() => !!window.XHAccountSettings"));
            assertEquals("1 / 3", page.locator("[data-security-score-value]").textContent());
            assertTrue(page.locator("#quoteContactList").textContent().contains("wx***"));

            page.locator("[name=displayName]").fill("新昵称");
            page.locator("#accountProfileFormV2 [data-submit]").click();
            page.waitForFunction("() => window.__requests.some(r => r.url === '/api/account/profile' && r.method === 'POST')");
            assertEquals(1, ((Number) page.evaluate("() => window.__requests.filter(r => r.url === '/api/account/profile' && r.method === 'POST').length")).intValue());
            assertEquals("fetch", page.evaluate("() => window.__requests.find(r => r.url === '/api/account/profile' && r.method === 'POST').headers['x-requested-with']"));

            page.locator("#accountQuoteContactForm [name=contactValue]").fill("wx-test-001");
            page.locator("#accountQuoteContactForm [data-submit]").click();
            page.waitForFunction("() => window.__requests.some(r => r.url === '/api/account/quote-contacts' && r.method === 'POST')");
            assertEquals("fetch", page.evaluate("() => window.__requests.find(r => r.url === '/api/account/quote-contacts' && r.method === 'POST').headers['x-requested-with']"));

            page.locator("[name=newPassword]").fill("Abc12345!");
            int score = Integer.parseInt(page.locator("[data-password-meter]").getAttribute("data-score"));
            assertTrue(score >= 2);

            page.locator("[data-session-revoke-all]").click();
            assertTrue(page.locator("dialog.account-confirm").isVisible());
            page.locator("dialog.account-confirm .btn.danger").click();
            page.waitForFunction("() => window.__requests.some(r => r.url === '/api/account/sessions/revoke-all' && r.method === 'POST')");
            assertEquals("fetch", page.evaluate("() => window.__requests.find(r => r.url === '/api/account/sessions/revoke-all' && r.method === 'POST').headers['x-requested-with']"));
        }
    }

    private void installBrowserMocks(Page page) {
        page.evaluate("""
                () => {
                  window.__requests = [];
                  const profile = {
                    displayName: '旧昵称',
                    publicUserId: 'XH00000001',
                    avatarUrl: '',
                    contactType: 'EMAIL',
                    contactMasked: 'te***@mail.com',
                    contactVerified: false,
                    email: '',
                    emailVerified: false,
                    createdAt: '2026-10-01T08:00:00Z',
                    quoteContacts: [{type:'WECHAT', masked:'wx***'}]
                  };
                  window.fetch = async (url, init = {}) => {
                    const method = String(init.method || 'GET').toUpperCase();
                    const headers = Object.fromEntries(new Headers(init.headers || {}).entries());
                    window.__requests.push({url:String(url), method, headers, body:init.body || null});
                    let payload = {};
                    if (url === '/api/account/me') payload = profile;
                    else if (url === '/api/account/sessions') payload = [
                      {id:'current', current:true, deviceLabel:'当前浏览器', ip:'10.***.1', createdAt:'2026-10-12T01:00:00Z', lastSeenAt:new Date().toISOString()},
                      {id:'other', current:false, deviceLabel:'另一台设备', ip:'10.***.2', createdAt:'2026-10-11T01:00:00Z', lastSeenAt:'2026-10-12T00:00:00Z'}
                    ];
                    else if (url === '/api/account/security/events') payload = [{title:'登录成功', description:'账号已登录', deviceLabel:'当前浏览器', ipMasked:'10.***.1', createdAt:new Date().toISOString()}];
                    else if (url === '/api/account/profile' && method === 'POST') {
                      const body = JSON.parse(String(init.body || '{}'));
                      profile.displayName = body.displayName || profile.displayName;
                      profile.avatarUrl = body.avatarUrl || '';
                      payload = {...profile};
                    } else if (url === '/api/account/quote-contacts' && method === 'POST') {
                      profile.quoteContacts = [{type:'WECHAT', masked:'wx***'}, {type:'WECHAT', masked:'wx-***-001'}];
                      payload = {ok:true};
                    } else if (url === '/api/account/sessions/revoke-all' && method === 'POST') payload = {ok:true};
                    else if (url === '/api/account/password/change' && method === 'POST') payload = {ok:true};
                    else if (String(url).startsWith('/api/account/email/verify')) payload = {ok:true};
                    else if (String(url).startsWith('/api/account/quote-contacts/') && method === 'DELETE') payload = {ok:true};
                    else if (String(url).startsWith('/api/account/sessions/') && method === 'DELETE') payload = {ok:true};
                    else if (url === '/api/account/logout' && method === 'POST') payload = {ok:true};
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
