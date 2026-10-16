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
class XingweiReleaseObservabilityPlaywrightTest {
    private static final Path TREND_JS = Path.of("src/main/resources/static/assets/admin-xingwei-runtime-trend.js");
    private static final Path RELEASE_JS = Path.of("src/main/resources/static/assets/admin-xingwei-release-attestation.js");

    private Browser chromium(Playwright playwright) {
        var options = new com.microsoft.playwright.BrowserType.LaunchOptions();
        String executable = System.getenv("XINGHUO_TEST_CHROMIUM");
        if (executable != null && !executable.isBlank()) options.setExecutablePath(Path.of(executable));
        return playwright.chromium().launch(options);
    }

    @Test
    void rendersRollingTrendAndMarksMinuteWithServerError() {
        try (Playwright playwright = Playwright.create(); Browser browser = chromium(playwright)) {
            Page page = browser.newPage();
            page.setContent("""
                    <div id="xwrRuntimeTrend">等待趋势采样</div>
                    <p id="xwrRuntimeTrendSummary"></p>
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(TREND_JS));
            page.evaluate("""
                    () => document.dispatchEvent(new CustomEvent('xwr:runtime-health', {detail:{requests:{recentMinutes:[
                      {minute:'2026-10-17T05:00:00Z',totalRequests:10,clientErrors:1,serverErrors:0},
                      {minute:'2026-10-17T05:01:00Z',totalRequests:20,clientErrors:0,serverErrors:1}
                    ]}}}))
                    """);

            assertEquals(2, page.locator("#xwrRuntimeTrend .xwr-trend-column").count());
            assertEquals("true", page.locator("#xwrRuntimeTrend .xwr-trend-column").nth(1).getAttribute("data-server-errors"));
            assertTrue(page.locator("#xwrRuntimeTrendSummary").textContent().contains("1 次 5xx"));
        }
    }

    @Test
    void rendersV2HostCredentialHealthComparisonAndFailsClosedOnDeployViewDenial() {
        try (Playwright playwright = Playwright.create(); Browser browser = chromium(playwright)) {
            Page page = browser.newPage();
            renderRelease(page);
            page.evaluate("""
                    () => {
                      let calls = 0;
                      window.fetch = async () => {
                        calls += 1;
                        window.__releaseCalls = calls;
                        if (calls > 1) return {ok:false,status:403,json:async () => ({})};
                        return {ok:true,status:200,json:async () => ({
                          available:true,status:'AVAILABLE',sampledAt:'2026-10-17T05:08:00Z',attestation:{
                            schemaVersion:2,
                            publishedAt:'2026-10-17T05:07:04Z',
                            gitSha:'acf8af76b156047ff33072cbeaf6b7789b8bb624',
                            jarSha256:'d797326b0ddbcc1601db98ad59cda36d4fd709ad01d763458b107a874dbaf4ba',
                            currentRelease:'xinghuo-v2-standard-20261017050537.jar',
                            previousRelease:'xinghuo-v2-standard-20261017040039.jar',
                            mainPort:8080,canaryPort:8081,nginxUpstreamPort:8080,canaryStopped:true,
                            checks:{predeploy18080:true,canary8081:true,production8080:true},
                            healthComparison:{
                              preDeploy8080:{passed:true,homeMillis:45,servicesMillis:60},
                              postDeploy8080:{passed:true,homeMillis:50,servicesMillis:72}
                            }
                          }
                        })};
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(RELEASE_JS));
            page.waitForFunction("() => window.xwrReleaseAttestation?.available === true");

            assertEquals("ok", page.locator("#xwrReleaseAttestationState").getAttribute("data-release-state"));
            assertTrue(page.locator("#xwrReleaseChecks").textContent().contains("全部通过"));
            assertTrue(page.locator("#xwrReleasePorts").textContent().contains("8080"));
            assertTrue(page.locator("#xwrReleaseCurrent").textContent().contains("20261017050537"));
            assertTrue(page.locator("#xwrReleaseHealthBefore").textContent().contains("45 ms"));
            assertTrue(page.locator("#xwrReleaseHealthAfter").textContent().contains("72 ms"));
            assertTrue(page.locator("#xwrReleaseHealthDelta").textContent().contains("基本稳定"));
            assertEquals("stable", page.locator("#xwrReleaseAttestation").getAttribute("data-health-comparison"));

            page.evaluate("() => window.dispatchEvent(new Event('online'))");
            page.waitForFunction("() => document.querySelector('#xwrReleaseAttestationState')?.dataset.releaseState === 'error'");
            assertEquals(2, ((Number) page.evaluate("() => window.__releaseCalls")).intValue());
            assertTrue(page.locator("#xwrReleaseAttestationMessage").textContent().contains("deploy:view"));
            assertEquals("-", page.locator("#xwrReleaseGitSha").textContent());
            assertEquals("-", page.locator("#xwrReleaseCurrent").textContent());
            assertEquals("-", page.locator("#xwrReleaseHealthBefore").textContent());
            assertEquals("-", page.locator("#xwrReleaseHealthAfter").textContent());
            assertEquals("false", page.locator("#xwrReleaseAttestation").getAttribute("data-release-ready"));
            assertEquals("unavailable", page.locator("#xwrReleaseAttestation").getAttribute("data-health-comparison"));
            assertTrue(page.locator("#xwrReleasePublishedAt").textContent().contains("不可用"));
        }
    }

    private static void renderRelease(Page page) {
        page.setContent("""
                <section id="xwrReleaseAttestation" data-release-ready="false" data-health-comparison="unavailable">
                  <span id="xwrReleaseAttestationState"><i></i><span>正在读取</span></span>
                  <strong id="xwrReleaseGitSha">-</strong>
                  <strong id="xwrReleaseJarSha">-</strong>
                  <strong id="xwrReleaseCurrent">-</strong>
                  <strong id="xwrReleasePrevious">-</strong>
                  <strong id="xwrReleasePorts">-</strong>
                  <strong id="xwrReleaseChecks">-</strong>
                  <strong id="xwrReleaseHealthBefore">-</strong>
                  <strong id="xwrReleaseHealthAfter">-</strong>
                  <strong id="xwrReleaseHealthDelta">-</strong>
                  <span id="xwrReleasePublishedAt">主机凭证时间：等待读取</span>
                  <p id="xwrReleaseAttestationMessage"></p>
                </section>
                """);
    }
}
