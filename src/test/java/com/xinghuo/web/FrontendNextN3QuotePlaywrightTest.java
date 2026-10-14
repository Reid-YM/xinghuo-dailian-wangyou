package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextN3QuotePlaywrightTest {
    private static final Path NEXT = Path.of("src/main/resources/static/next");

    @Test
    void chromiumSubmitsOnceAndShowsBusinessNumber() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            AtomicInteger calls = new AtomicInteger();
            List<String> keys = new CopyOnWriteArrayList<>();
            routeQuotePage(page, route -> {
                calls.incrementAndGet();
                keys.add(route.request().headers().getOrDefault("idempotency-key", ""));
                route.fulfill(new Route.FulfillOptions()
                        .setStatus(201)
                        .setContentType("application/json")
                        .setBody("{\"businessNo\":\"XH-Q-N3-001\",\"status\":\"NEW\"}"));
            });

            page.navigate("https://example.test/next/quote?utm_source=chatgpt");
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n3.js")));
            fillRequired(page);

            page.evaluate("""
                    () => {
                      const button=document.querySelector('#submit');
                      button.click();
                      button.click();
                    }
                    """);
            page.waitForSelector("#n3QuoteMessage:has-text('XH-Q-N3-001')");

            assertEquals(1, calls.get());
            assertEquals(1, keys.size());
            assertFalse(keys.getFirst().isBlank());
            assertEquals("/record/XH-Q-N3-001", page.locator("#n3QuoteMessage a").getAttribute("href"));
            assertEquals("true", page.locator("#submit").getAttribute("data-n3-real-submit"));
        }
    }

    @Test
    void webkitRetryReusesIdempotencyKeyAfterServerFailure() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage();
            AtomicInteger calls = new AtomicInteger();
            List<String> keys = new CopyOnWriteArrayList<>();
            routeQuotePage(page, route -> {
                int call = calls.incrementAndGet();
                keys.add(route.request().headers().getOrDefault("idempotency-key", ""));
                if (call == 1) {
                    route.fulfill(new Route.FulfillOptions()
                            .setStatus(500)
                            .setContentType("application/json")
                            .setBody("{\"message\":\"临时服务异常\"}"));
                } else {
                    route.fulfill(new Route.FulfillOptions()
                            .setStatus(201)
                            .setContentType("application/json")
                            .setBody("{\"businessNo\":\"XH-Q-N3-RETRY\",\"status\":\"NEW\"}"));
                }
            });

            page.navigate("https://example.test/next/quote");
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n3.js")));
            fillRequired(page);

            page.locator("#submit").click();
            page.waitForSelector("#submit:has-text('重试提交')");
            assertTrue(page.locator("#n3QuoteMessage").textContent().contains("临时服务异常"));
            page.locator("#submit").click();
            page.waitForSelector("#n3QuoteMessage:has-text('XH-Q-N3-RETRY')");

            assertEquals(2, calls.get());
            assertEquals(2, keys.size());
            assertFalse(keys.getFirst().isBlank());
            assertEquals(keys.getFirst(), keys.get(1));
        }
    }

    private static void routeQuotePage(Page page, java.util.function.Consumer<Route> quoteHandler) {
        page.route("**/*", route -> {
            if (route.request().url().endsWith("/api/quote")) {
                quoteHandler.accept(route);
                return;
            }
            route.fulfill(new Route.FulfillOptions()
                    .setContentType("text/html")
                    .setBody(html()));
        });
    }

    private static void fillRequired(Page page) {
        page.locator("#n3ContactMethod").selectOption("微信");
        page.locator("#n3CustomerContact").fill("test-contact");
        page.locator("#n3PrivacyConsent").check();
        page.locator("#goal").fill("测试目标");
    }

    private static String html() {
        return """
                <body>
                  <section class="page" data-page="quote">
                    <div class="head"><p class="sub">演示模式：不会向生产环境发送请求。</p></div>
                    <div class="panel">
                      <div class="formSec"><h3>选择服务</h3><div class="formGrid">
                        <div class="field"><select id="qGame"><option value="CODMW4">使命召唤 MW4</option></select></div>
                        <div class="field"><select id="qCat"><option value="LEVEL">等级</option></select></div>
                        <div class="field"><select id="qSvc"><option value="rank">多人模式排位</option></select></div>
                      </div></div>
                      <div class="formSec"><h3>填写需求</h3><div class="field"><textarea id="goal"></textarea></div></div>
                      <div class="formSec"><h3>联系方式</h3><div class="formGrid">
                        <div class="field"><input placeholder="请输入称呼"></div>
                        <div class="field"><input placeholder="微信 / QQ / 手机"></div>
                      </div></div>
                      <button id="submit" type="button">提交询价 →</button>
                    </div>
                  </section>
                  <div class="badge">FRONTEND NEXT</div>
                </body>
                """;
    }
}
