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
class XingweiReleaseConfirmationPlaywrightTest {
    private static final Path JS = Path.of("src/main/resources/static/assets/admin-xingwei-release-confirmation.js");

    private Browser chromium(Playwright playwright) {
        var options = new com.microsoft.playwright.BrowserType.LaunchOptions();
        String executable = System.getenv("XINGHUO_TEST_CHROMIUM");
        if (executable != null && !executable.isBlank()) options.setExecutablePath(Path.of(executable));
        return playwright.chromium().launch(options);
    }

    @Test
    void linksChangeSubmitsWithCsrfAndClearsAfterDeployViewDenial() {
        try (Playwright playwright = Playwright.create(); Browser browser = chromium(playwright)) {
            Page page = browser.newPage();
            render(page);
            page.evaluate("""
                    () => {
                      let calls = 0;
                      window.fetch = async (url, opts = {}) => {
                        calls += 1;
                        window.__confirmCalls = calls;
                        if (calls === 1) {
                          return {ok:true,status:200,json:async () => ({
                            confirmation:{
                              available:true,status:'PENDING',sampledAt:'2026-10-17T07:31:00Z',
                              release:{currentRelease:'xinghuo-v2-standard-20261017070000.jar',gitSha:'a'.repeat(40),pullRequestNumber:525,publishedAt:'2026-10-17T07:00:00Z'},
                              stableEligible:true,stableEligibility:'ELIGIBLE',latest:null,history:[]
                            },
                            canConfirm:true
                          })};
                        }
                        if (calls === 2) {
                          window.__confirmHeader = opts.headers?.['X-CSRF-TOKEN'];
                          window.__confirmBody = JSON.parse(opts.body || '{}');
                          return {ok:true,status:200,json:async () => ({
                            confirmation:{
                              available:true,status:'STABLE',sampledAt:'2026-10-17T07:32:00Z',
                              release:{currentRelease:'xinghuo-v2-standard-20261017070000.jar',gitSha:'a'.repeat(40),pullRequestNumber:525,publishedAt:'2026-10-17T07:00:00Z'},
                              stableEligible:true,stableEligibility:'ELIGIBLE',
                              latest:{decision:'STABLE',note:'观察正常',confirmedBy:'admin',confirmedAt:'2026-10-17T07:32:00Z',observationSeverity:'OK',coverageState:'COVERED',window30State:'COMPLETE',window30ServerErrors:0,window30TotalRequests:240},
                              history:[{decision:'STABLE',note:'观察正常',confirmedBy:'admin',confirmedAt:'2026-10-17T07:32:00Z',observationSeverity:'OK',coverageState:'COVERED',window30State:'COMPLETE',window30ServerErrors:0,window30TotalRequests:240}]
                            },
                            canConfirm:true
                          })};
                        }
                        return {ok:false,status:403,json:async () => ({error:'forbidden'})};
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(JS));
            page.waitForFunction("() => window.xwrReleaseConfirmation?.confirmation?.stableEligible === true");

            assertTrue(page.locator("#xwrReleaseConfirmationChange a").first().textContent().contains("PR #525"));
            assertTrue(page.locator("#xwrReleaseConfirmationChange a").first().getAttribute("href").endsWith("/pull/525"));
            assertTrue(page.locator("#xwrReleaseConfirmationChange a").nth(1).getAttribute("href").endsWith("/commit/" + "a".repeat(40)));
            assertTrue(page.locator("#xwrReleaseConfirmStable").isEnabled());

            page.locator("#xwrReleaseConfirmationNote").fill("观察正常");
            page.locator("#xwrReleaseConfirmStable").click();
            page.waitForFunction("() => window.xwrReleaseConfirmation?.confirmation?.status === 'STABLE'");

            assertEquals("test-csrf", page.evaluate("() => window.__confirmHeader"));
            assertEquals("STABLE", page.evaluate("() => window.__confirmBody.decision"));
            assertEquals("xinghuo-v2-standard-20261017070000.jar", page.evaluate("() => window.__confirmBody.expectedRelease"));
            assertEquals("a".repeat(40), page.evaluate("() => window.__confirmBody.expectedGitSha"));
            assertTrue(page.locator("#xwrReleaseConfirmationResult").textContent().contains("已人工确认稳定"));
            assertEquals(1, page.locator("#xwrReleaseConfirmationHistory .xwr-confirm-history-item").count());

            page.evaluate("() => window.dispatchEvent(new Event('online'))");
            page.waitForFunction("() => document.querySelector('#xwrReleaseConfirmationState')?.dataset.confirmationState === 'error'");
            assertEquals("-", page.locator("#xwrReleaseConfirmationRelease").textContent());
            assertEquals("-", page.locator("#xwrReleaseConfirmationResult").textContent());
            assertTrue(page.locator("#xwrReleaseConfirmStable").isDisabled());
            assertTrue(page.locator("#xwrReleaseConfirmationMessage").textContent().contains("deploy:view"));
            assertEquals("false", page.locator("#xwrReleaseConfirmation").getAttribute("data-confirmation-ready"));
        }
    }

    private static void render(Page page) {
        page.setContent("""
                <meta name="_csrf" content="test-csrf"/>
                <meta name="_csrf_header" content="X-CSRF-TOKEN"/>
                <section id="xwrReleaseConfirmation" data-confirmation-ready="false" data-can-confirm="false">
                  <span id="xwrReleaseConfirmationState"><i></i><span>正在读取</span></span>
                  <strong id="xwrReleaseConfirmationRelease">-</strong>
                  <strong id="xwrReleaseConfirmationChange">-</strong>
                  <strong id="xwrReleaseConfirmationEligibility">-</strong>
                  <strong id="xwrReleaseConfirmationResult">-</strong>
                  <small id="xwrReleaseConfirmationLatest">-</small>
                  <textarea id="xwrReleaseConfirmationNote"></textarea>
                  <button id="xwrReleaseConfirmStable" disabled>确认稳定</button>
                  <button id="xwrReleaseConfirmAttention" disabled>需要继续观察</button>
                  <button id="xwrReleaseConfirmationRefresh">刷新确认状态</button>
                  <div id="xwrReleaseConfirmationHistory"></div>
                  <p id="xwrReleaseConfirmationMessage"></p>
                </section>
                """);
    }
}
