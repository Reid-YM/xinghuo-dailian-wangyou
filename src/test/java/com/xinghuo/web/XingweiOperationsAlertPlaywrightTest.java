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
class XingweiOperationsAlertPlaywrightTest {
    private static final Path ALERT_JS = Path.of("src/main/resources/static/assets/admin-xingwei-operations-alerts.js");

    private Browser chromium(Playwright playwright) {
        var options = new com.microsoft.playwright.BrowserType.LaunchOptions();
        String executable = System.getenv("XINGHUO_TEST_CHROMIUM");
        if (executable != null && !executable.isBlank()) options.setExecutablePath(Path.of(executable));
        return playwright.chromium().launch(options);
    }

    @Test
    void rendersWarningWithRecommendedActionThenClearsOnDeployViewDenial() {
        try (Playwright playwright = Playwright.create(); Browser browser = chromium(playwright)) {
            Page page = browser.newPage();
            page.setContent("""
                    <section id="xwrOperationsAlerts" data-alert-ready="false">
                      <span id="xwrOperationsAlertState"><i></i><span>正在评估</span></span>
                      <strong id="xwrOperationsSeverity">-</strong>
                      <strong id="xwrOperations5xx">-</strong>
                      <strong id="xwrOperationsHeap">-</strong>
                      <strong id="xwrOperationsDatabase">-</strong>
                      <div id="xwrOperationsAlertList"></div>
                      <span id="xwrOperationsAlertSampledAt"></span>
                      <p id="xwrOperationsAlertMessage"></p>
                    </section>
                    """);
            page.evaluate("""
                    () => {
                      let calls = 0;
                      window.fetch = async () => {
                        calls += 1;
                        window.__opsCalls = calls;
                        if (calls > 1) return {ok:false,status:403,json:async () => ({})};
                        return {ok:true,status:200,json:async () => ({
                          sampledAt:'2026-10-17T05:30:00Z',severity:'WARNING',severityLabel:'警告',activeAlertCount:1,
                          runtime:{databaseUp:true,databaseCheckMillis:18,heapPercent:62.4,recentRequests:120,recentServerErrors:4,recentServerErrorRate:0.0333,windowMinutes:5},
                          alerts:[{code:'server-errors-warning',level:'WARNING',title:'近 5 分钟 5xx 持续出现',detail:'近 5 分钟 120 次请求中出现 4 次 5xx。',recommendedAction:'检查最近 5xx 对应的应用日志与依赖状态，并在 1 分钟后复核趋势。',source:'APPLICATION'}],
                          releaseComparison:{available:true,state:'STABLE'},recheckAfterSeconds:60
                        })};
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ALERT_JS));
            page.waitForFunction("() => document.querySelector('#xwrOperationsAlerts')?.dataset.alertReady === 'true'");

            assertEquals("warning", page.locator("#xwrOperationsAlertState").getAttribute("data-alert-state"));
            assertTrue(page.locator("#xwrOperationsSeverity").textContent().contains("警告"));
            assertTrue(page.locator("#xwrOperations5xx").textContent().contains("4 / 120"));
            assertEquals(1, page.locator("#xwrOperationsAlertList .xwr-ops-alert").count());
            assertTrue(page.locator("#xwrOperationsAlertList").textContent().contains("建议复核"));
            assertTrue(page.locator("#xwrOperationsAlertSampledAt").textContent().contains("60 秒后自动复核"));

            page.evaluate("() => window.dispatchEvent(new Event('online'))");
            page.waitForFunction("() => document.querySelector('#xwrOperationsAlertState')?.dataset.alertState === 'error'");
            assertEquals(2, ((Number) page.evaluate("() => window.__opsCalls")).intValue());
            assertTrue(page.locator("#xwrOperationsAlertMessage").textContent().contains("deploy:view"));
            assertEquals("-", page.locator("#xwrOperationsSeverity").textContent());
            assertEquals(0, page.locator("#xwrOperationsAlertList .xwr-ops-alert").count());
            assertEquals("false", page.locator("#xwrOperationsAlerts").getAttribute("data-alert-ready"));
        }
    }
}
