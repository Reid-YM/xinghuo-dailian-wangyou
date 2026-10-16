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
class XingweiRuntimeHealthDashboardPlaywrightTest {
    private static final Path JS = Path.of("src/main/resources/static/assets/admin-xingwei-runtime-health.js");

    private Browser chromium(Playwright p) {
        var options = new com.microsoft.playwright.BrowserType.LaunchOptions();
        String executable = System.getenv("XINGHUO_TEST_CHROMIUM");
        if (executable != null && !executable.isBlank()) options.setExecutablePath(Path.of(executable));
        return p.chromium().launch(options);
    }

    @Test
    void rendersRuntimeMetricsAndManualRefreshClearsHistoricalWarning() {
        try (Playwright playwright = Playwright.create(); Browser browser = chromium(playwright)) {
            Page page = browser.newPage();
            render(page);
            page.evaluate("""
                    () => {
                      let calls = 0;
                      window.fetch = async (url) => {
                        if (String(url) !== '/api/admin/xingwei/runtime-health') return {ok:false,status:404};
                        calls += 1;
                        window.__healthCalls = calls;
                        const withError = calls === 1;
                        return {ok:true,status:200,json:async () => ({
                          sampledAt:'2026-10-17T04:10:00+08:00',
                          startedAt:'2026-10-17T02:00:00+08:00',
                          uptimeMillis:7800000,
                          database:{up:true,checkMillis:4},
                          jvm:{usedHeapBytes:268435456,maxHeapBytes:1073741824,threadCount:36},
                          requests:{totalRequests:1284,clientErrors:12,serverErrors:withError ? 1 : 0,lastServerError:withError ? {at:'2026-10-17T03:58:00+08:00',status:500} : null}
                        })};
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(JS));
            page.waitForFunction("() => window.xwrRuntimeHealth?.requests?.serverErrors === 1");

            assertEquals("数据库正常", page.locator("#xwrRuntimeDatabase").textContent());
            assertEquals("1284", page.locator("#xwrRuntimeRequests").textContent());
            assertEquals("12", page.locator("#xwrRuntime4xx").textContent());
            assertEquals("1", page.locator("#xwrRuntime5xx").textContent());
            assertTrue(page.locator("#xwrRuntimeLastServerError").textContent().contains("500"));
            assertEquals("warning", page.locator("#xwrRuntimeHealthState").getAttribute("data-health-state"));

            page.locator("#xwrRuntimeHealthRefresh").click();
            page.waitForFunction("() => window.__healthCalls === 2 && window.xwrRuntimeHealth?.requests?.serverErrors === 0");
            assertEquals("0", page.locator("#xwrRuntime5xx").textContent());
            assertEquals("ok", page.locator("#xwrRuntimeHealthState").getAttribute("data-health-state"));
            assertTrue(page.locator("#xwrRuntimeLastServerError").textContent().contains("尚无记录"));
        }
    }

    @Test
    void deployViewDenialFailsClosedWithoutInventingHealthData() {
        try (Playwright playwright = Playwright.create(); Browser browser = chromium(playwright)) {
            Page page = browser.newPage();
            render(page);
            page.evaluate("""
                    () => {
                      window.fetch = async () => {
                        window.__healthCalls = (window.__healthCalls || 0) + 1;
                        return {ok:false,status:403,json:async () => ({})};
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(JS));
            page.waitForFunction("() => document.querySelector('#xwrRuntimeHealthMessage')?.textContent.includes('deploy:view')");

            assertEquals("error", page.locator("#xwrRuntimeHealthState").getAttribute("data-health-state"));
            assertTrue(page.locator("#xwrRuntimeHealthState").textContent().contains("无运行诊断权限"));
            assertEquals("读取中", page.locator("#xwrRuntimeDatabase").textContent());
            assertEquals(1, ((Number) page.evaluate("() => window.__healthCalls")).intValue());
        }
    }

    private static void render(Page page) {
        page.setContent("""
                <main>
                  <section id="xwrRuntimeHealth" data-health-ready="false">
                    <div>
                      <span class="xw-status" id="xwrRuntimeHealthState"><i></i><span>正在读取</span></span>
                      <button id="xwrRuntimeHealthRefresh" type="button">立即复核</button>
                    </div>
                    <article class="xwr-health-card" data-tone=""><strong id="xwrRuntimeDatabase">读取中</strong><small>db</small></article>
                    <article class="xwr-health-card" data-tone=""><strong id="xwrRuntimeUptime">-</strong><small>uptime</small></article>
                    <article class="xwr-health-card" data-tone=""><strong id="xwrRuntimeHeap">-</strong><small>heap</small></article>
                    <article class="xwr-health-card" data-tone=""><strong id="xwrRuntimeRequests">-</strong><small>requests</small></article>
                    <article class="xwr-health-card" data-tone=""><strong id="xwrRuntime4xx">-</strong><small>4xx</small></article>
                    <article class="xwr-health-card" data-tone=""><strong id="xwrRuntime5xx">-</strong><small>5xx</small></article>
                    <span id="xwrRuntimeLastServerError">最近 5xx：等待采样</span>
                    <span id="xwrRuntimeSampledAt">采样时间：等待读取</span>
                    <p id="xwrRuntimeHealthMessage"></p>
                  </section>
                </main>
                """);
    }
}
