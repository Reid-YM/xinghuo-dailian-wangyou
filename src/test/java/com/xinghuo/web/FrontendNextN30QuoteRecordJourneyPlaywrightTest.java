package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextN30QuoteRecordJourneyPlaywrightTest {
    private static final Path NEXT = Path.of("src/main/resources/static/next");
    private static final String BUSINESS_NO = "XHABCDEF1234";

    @Test
    void chromiumAndWebkitCarryRealQuoteNumberThroughRecordAndSharedHistory() {
        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions();
            String executable = System.getenv("XINGHUO_TEST_CHROMIUM");
            if (executable != null && !executable.isBlank()) {
                options.setExecutablePath(Path.of(executable));
            }
            try (Browser chromium = playwright.chromium().launch(options);
                 Browser webkit = playwright.webkit().launch()) {
                assertJourney(chromium);
                assertJourney(webkit);
            }
        }
    }

    private static void assertJourney(Browser browser) {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
        AtomicInteger quoteCalls = new AtomicInteger();
        AtomicInteger recordReads = new AtomicInteger();
        page.route("**/*", route -> {
            String path = URI.create(route.request().url()).getPath();
            if (path.equals("/api/quote")) {
                quoteCalls.incrementAndGet();
                assertEquals("POST", route.request().method());
                assertFalse(route.request().headers().getOrDefault("idempotency-key", "").isBlank());
                route.fulfill(new Route.FulfillOptions().setStatus(201)
                        .setContentType("application/json")
                        .setBody("{\"businessNo\":\"" + BUSINESS_NO + "\",\"status\":\"NEW\"}"));
                return;
            }
            if (path.equals("/api/records/" + BUSINESS_NO)) {
                recordReads.incrementAndGet();
                assertEquals("GET", route.request().method());
                route.fulfill(new Route.FulfillOptions().setContentType("application/json")
                        .setBody("{\"businessNo\":\"" + BUSINESS_NO + "\",\"stage\":\"QUOTE\","
                                + "\"status\":\"NEW\",\"title\":\"MW4 任务询价\","
                                + "\"progressText\":\"等待客服确认\","
                                + "\"createdAt\":\"2026-10-18T06:00:00Z\","
                                + "\"updatedAt\":\"2026-10-18T06:00:00Z\"}"));
                return;
            }
            if (path.equals("/next/quote")) {
                route.fulfill(new Route.FulfillOptions().setContentType("text/html; charset=utf-8")
                        .setBody(quoteHtml()));
                return;
            }
            route.fulfill(new Route.FulfillOptions().setContentType("text/html; charset=utf-8")
                    .setBody("<!doctype html><html lang='zh-CN'><body>"
                            + "<section class='page active' data-page='home'></section>"
                            + "<footer class='next-footer'></footer></body></html>"));
        });
        try {
            page.navigate("https://example.test/next/quote?utm_source=n30");
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n3.js")));
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n26-record-history-bridge.js")));
            page.locator("#goal").fill("多人任务进度查询");
            page.locator("#n3ContactMethod").selectOption("微信");
            page.locator("#n3CustomerContact").fill("wechat-test");
            page.locator("#n3PrivacyConsent").check();
            page.locator("#submit").click();
            page.waitForSelector("#n3QuoteMessage a[href='/next/record/" + BUSINESS_NO + "']");
            page.waitForFunction("() => JSON.parse(localStorage.getItem('xh_client_records_v1') || '[]').length === 1");
            assertEquals(1, quoteCalls.get(), "N30 must not duplicate the quote");
            assertEquals(BUSINESS_NO, page.evaluate(
                    "() => JSON.parse(localStorage.getItem('xh_client_records_v1'))[0].businessNo"));

            page.locator("#n3QuoteMessage a").click();
            page.waitForURL("**/next/record/" + BUSINESS_NO);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n13-record-detail.js")));
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n26-record-history-bridge.js")));
            page.waitForSelector("#n13RecordDetail .n13-facts");
            page.waitForSelector("a[data-n26-records-center='1']");
            assertTrue(page.locator("#n13RecordDetail").textContent().contains(BUSINESS_NO));
            assertTrue(page.locator("#n13RecordDetail").textContent().contains("等待客服确认"));
            assertEquals("/next/records",
                    page.locator("a[data-n26-records-center='1']").getAttribute("href"));
            assertEquals("/record/" + BUSINESS_NO,
                    page.locator("a[data-n13-legacy='1']").getAttribute("href"));

            page.locator("a[data-n26-records-center='1']").click();
            page.waitForURL("**/next/records");
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n26-records-center.js")));
            page.waitForSelector("#n26ListHost a.n26-card[href='/next/record/" + BUSINESS_NO + "']");
            assertTrue(page.locator("#n26ListHost").textContent().contains("等待客服确认"));
            assertEquals(1, quoteCalls.get());
            assertTrue(recordReads.get() >= 2);
            assertEquals(1, page.evaluate(
                    "() => JSON.parse(localStorage.getItem('xh_client_records_v1') || '[]').length"));
        } finally {
            page.close();
        }
    }

    private static String quoteHtml() {
        return """
                <!doctype html><html lang="zh-CN"><head><meta charset="utf-8"></head><body>
                  <section class="page active" data-page="quote">
                    <div class="head"><p class="sub">演示模式：仅为 N30 浏览器模拟。</p></div>
                    <div class="panel">
                      <div class="formSec"><div class="formGrid">
                        <div class="field"><select id="qGame"><option value="CODMW4">使命召唤 MW4</option></select></div>
                        <div class="field"><select id="qCat"><option value="LEVEL">等级</option></select></div>
                        <div class="field"><select id="qSvc"><option value="rank">多人任务</option></select></div>
                      </div></div>
                      <div class="formSec"><textarea id="goal"></textarea></div>
                      <div class="formSec"><div class="formGrid">
                        <div class="field"><input placeholder="请输入称呼"></div>
                        <div class="field"><input placeholder="微信 / QQ / 手机"></div>
                      </div></div>
                      <button id="submit" type="button">提交询价</button>
                    </div>
                  </section>
                  <div class="badge">FRONTEND NEXT</div>
                  <footer class="next-footer"></footer>
                </body></html>
                """;
    }
}
