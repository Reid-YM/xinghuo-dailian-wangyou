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
class XingweiRuntimeResiliencePlaywrightTest {
    private static final Path JS = Path.of("src/main/resources/static/assets/admin-xingwei.js");

    @Test
    void onlineLifecycleRecheckDetectsRuntimeDriftAfterInitialMatch() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            render(page);
            page.evaluate("""
                    () => {
                      let healthCalls = 0;
                      window.fetch = async (url) => {
                        if (String(url) === '/api/admin/xingwei/access') {
                          return {ok:true, json:async () => ({
                            mode:'LEGACY_ADMIN_OVERRIDE', allowAll:true,
                            permissionCodes:[], roleCodes:[], dataScope:'ALL'
                          })};
                        }
                        if (String(url) === '/api/admin/xingwei/health') {
                          healthCalls += 1;
                          const first = healthCalls === 1;
                          return {ok:true, json:async () => ({
                            ok:true,
                            version:first ? '20261014-v3u' : '20261014-v3l',
                            stage:first ? 'V3U · STABLE PRODUCTION' : 'V3L · STABLE PRODUCTION',
                            serverTime:'2026-10-14T06:00:00+08:00'
                          })};
                        }
                        return {ok:false, status:404, json:async () => ({})};
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(JS));
            page.waitForFunction("() => window.xwRuntimeHealth?.state === 'match'");
            assertTrue(page.locator("#xwBackendStatus").textContent().contains("已核对"));

            page.evaluate("() => window.dispatchEvent(new Event('online'))");
            page.waitForFunction("() => window.xwRuntimeHealth?.state === 'mismatch'");
            assertEquals("online", page.evaluate("() => window.xwRuntimeHealth.reason"));
            assertTrue(page.locator("#xwBackendStatus").textContent().contains("版本漂移"));
            assertTrue(page.locator("#xwServerTime").textContent().contains("页面 20261014-v3u"));
            assertTrue(page.locator("#xwServerTime").textContent().contains("运行 20261014-v3l"));
        }
    }

    @Test
    void permissionFailureRemainsFailClosed() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            render(page);
            page.evaluate("""
                    () => {
                      window.fetch = async (url) => {
                        if (String(url) === '/api/admin/xingwei/access') {
                          throw new Error('network');
                        }
                        if (String(url) === '/api/admin/xingwei/health') {
                          return {ok:true, json:async () => ({
                            ok:true, version:'20261014-v3u', stage:'V3U · STABLE PRODUCTION',
                            serverTime:'2026-10-14T06:00:00+08:00'
                          })};
                        }
                        return {ok:false, status:404, json:async () => ({})};
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(JS));
            page.waitForFunction("() => document.querySelector('#xwAccessMode')?.textContent.includes('受控模块已隐藏')");
            assertEquals("DENIED", page.evaluate("() => window.xwAccess.mode"));
            assertTrue((Boolean) page.evaluate("() => document.querySelector('.xw-module').hidden"));
        }
    }

    private static void render(Page page) {
        page.setContent("""
                <body>
                  <main class="xw-main">
                    <header><div class="xw-topbar-actions"><span class="xw-status" id="xwBackendStatus"><i></i>正在检查服务</span></div></header>
                    <section id="overview"><p class="xw-kicker">V3U · STABLE PRODUCTION</p><div class="xw-version-card"><strong>20261014-v3u</strong><small id="xwServerTime">等待</small></div></section>
                    <input id="xwModuleSearch"/>
                    <span id="xwSearchCount"></span>
                    <article class="xw-module"><div class="xw-module-index">B02</div>订单</article>
                  </main>
                </body>
                """);
    }
}
