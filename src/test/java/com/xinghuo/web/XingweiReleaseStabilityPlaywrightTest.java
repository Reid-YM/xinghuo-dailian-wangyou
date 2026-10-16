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
class XingweiReleaseStabilityPlaywrightTest {
    private static final Path STABILITY_JS = Path.of("src/main/resources/static/assets/admin-xingwei-release-stability.js");

    private Browser chromium(Playwright playwright) {
        var options = new com.microsoft.playwright.BrowserType.LaunchOptions();
        String executable = System.getenv("XINGHUO_TEST_CHROMIUM");
        if (executable != null && !executable.isBlank()) options.setExecutablePath(Path.of(executable));
        return playwright.chromium().launch(options);
    }

    @Test
    void rendersObservationBaselineAndClearsSensitiveViewAfterDeployViewDenial() {
        try (Playwright playwright = Playwright.create(); Browser browser = chromium(playwright)) {
            Page page = browser.newPage();
            render(page);
            page.evaluate("""
                    () => {
                      let calls = 0;
                      window.fetch = async () => {
                        calls += 1;
                        window.__stabilityCalls = calls;
                        if (calls > 1) return {ok:false,status:403,json:async () => ({})};
                        return {ok:true,status:200,json:async () => ({
                          sampledAt:'2026-10-17T06:20:00Z',
                          processRetentionMinutes:60,
                          current:{
                            available:true,status:'AVAILABLE',publishedAt:'2026-10-17T06:10:00Z',
                            runtimeStartedAt:'2026-10-17T06:08:00Z',ageSeconds:600,
                            coverageState:'COVERED',severity:'WATCH',
                            windows:[
                              {minutes:5,state:'COMPLETE',observedMinutes:5,totalRequests:120,clientErrors:3,serverErrors:1,serverErrorRate:0.0083,severity:'WATCH'},
                              {minutes:15,state:'IN_PROGRESS',observedMinutes:10,totalRequests:220,clientErrors:5,serverErrors:1,serverErrorRate:0.0045,severity:'WATCH'},
                              {minutes:30,state:'IN_PROGRESS',observedMinutes:10,totalRequests:220,clientErrors:5,serverErrors:1,serverErrorRate:0.0045,severity:'WATCH'}
                            ]
                          },
                          historyAvailable:true,historyStatus:'AVAILABLE',
                          baseline:{ready:true,state:'ESTABLISHED',comparableReleaseCount:2,medianPostDeployMaxMillis:52,stableReleaseCount:2,watchReleaseCount:0,latestPostDeployMaxMillis:52,latestVsMedianMillis:0},
                          history:[
                            {publishedAt:'2026-10-17T06:10:00Z',gitSha:'a'.repeat(40),currentRelease:'xinghuo-v2-standard-20261017061000.jar',previousRelease:'xinghuo-v2-standard-20261017060000.jar',healthComparable:true,beforeMaxMillis:46,afterMaxMillis:52,deltaMillis:6,ratio:1.13,state:'STABLE'},
                            {publishedAt:'2026-10-17T06:00:00Z',gitSha:'b'.repeat(40),currentRelease:'xinghuo-v2-standard-20261017060000.jar',previousRelease:'xinghuo-v2-standard-20261017050000.jar',healthComparable:true,beforeMaxMillis:48,afterMaxMillis:52,deltaMillis:4,ratio:1.08,state:'STABLE'}
                          ]
                        })};
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(STABILITY_JS));
            page.waitForFunction("() => window.xwrReleaseStability?.baseline?.ready === true");

            assertEquals("120 / 1 次", reverseCount(page.locator("#xwrReleaseWindow5").textContent()));
            assertTrue(page.locator("#xwrReleaseWindow5").textContent().contains("1 / 120"));
            assertEquals("52 ms", page.locator("#xwrReleaseBaselineMedian").textContent());
            assertTrue(page.locator("#xwrReleaseBaselineReleases").textContent().contains("2 次可比"));
            assertEquals(2, page.locator("#xwrReleaseStabilityHistory .xwr-stability-history-item").count());
            assertEquals("warning", page.locator("#xwrReleaseStabilityState").getAttribute("data-stability-state"));

            page.evaluate("() => window.dispatchEvent(new Event('online'))");
            page.waitForFunction("() => document.querySelector('#xwrReleaseStabilityState')?.dataset.stabilityState === 'error'");

            assertEquals(2, ((Number) page.evaluate("() => window.__stabilityCalls")).intValue());
            assertEquals("-", page.locator("#xwrReleaseBaselineMedian").textContent());
            assertEquals("-", page.locator("#xwrReleaseWindow5").textContent());
            assertTrue(page.locator("#xwrReleaseStabilityHistory").textContent().contains("保持隐藏"));
            assertTrue(page.locator("#xwrReleaseStabilityMessage").textContent().contains("deploy:view"));
            assertEquals("false", page.locator("#xwrReleaseStability").getAttribute("data-stability-ready"));
        }
    }

    private static String reverseCount(String value) {
        if (value == null || !value.contains(" / ")) return value;
        String[] parts = value.replace(" 次", "").split(" / ");
        return parts[1] + " / " + parts[0] + " 次";
    }

    private static void render(Page page) {
        page.setContent("""
                <section id="xwrReleaseStability" data-stability-ready="false">
                  <span id="xwrReleaseStabilityState"><i></i><span>正在读取</span></span>
                  <button id="xwrReleaseStabilityRefresh">立即复核</button>
                  <strong id="xwrReleaseObservationAge">-</strong>
                  <strong id="xwrReleaseObservationCoverage">-</strong>
                  <article class="xwr-stability-card"><strong id="xwrReleaseWindow5">-</strong><small>等待观察数据</small></article>
                  <article class="xwr-stability-card"><strong id="xwrReleaseWindow15">-</strong><small>等待观察数据</small></article>
                  <article class="xwr-stability-card"><strong id="xwrReleaseWindow30">-</strong><small>等待观察数据</small></article>
                  <strong id="xwrReleaseBaselineState">-</strong>
                  <strong id="xwrReleaseBaselineMedian">-</strong>
                  <p id="xwrReleaseBaselineReleases">-</p>
                  <div id="xwrReleaseStabilityHistory"></div>
                  <span id="xwrReleaseStabilitySampledAt">-</span>
                  <p id="xwrReleaseStabilityMessage"></p>
                </section>
                """);
    }
}
