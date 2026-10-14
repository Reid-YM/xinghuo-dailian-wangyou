package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextN4MiniLoginPlaywrightTest {
    private static final Path NEXT = Path.of("src/main/resources/static/next");

    @Test
    void chromiumGeneratesQrPollsApprovalAndCompletesWebsiteLogin() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            AtomicInteger challenges = new AtomicInteger();
            AtomicInteger polls = new AtomicInteger();
            AtomicInteger completes = new AtomicInteger();

            page.route("**/*", route -> {
                String url = route.request().url();
                if (url.endsWith("/api/account/wechat-mini/availability")) {
                    json(route, 200, "{\"enabled\":true}");
                    return;
                }
                if (url.endsWith("/api/account/wechat-mini/challenge")) {
                    challenges.incrementAndGet();
                    json(route, 200, "{\"code\":\"ABCD1234\",\"expiresIn\":300,\"binding\":false,\"qrImage\":\"data:image/png;base64,AAAA\"}");
                    return;
                }
                if (url.endsWith("/api/account/wechat-mini/poll")) {
                    polls.incrementAndGet();
                    json(route, 200, "{\"status\":\"ready\",\"identityHint\":\"4321\"}");
                    return;
                }
                if (url.endsWith("/api/account/wechat-mini/complete")) {
                    completes.incrementAndGet();
                    json(route, 200, "{\"status\":\"approved\",\"identityHint\":\"\"}");
                    return;
                }
                html(route);
            });

            page.navigate("https://example.test/next/account");
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n4-mini.js")));
            page.waitForSelector("#n4MiniStart:not([disabled])");
            page.locator("#n4MiniStart").click();
            page.waitForSelector("#n4MiniCode:has-text('ABCD 1234')");
            assertEquals("data:image/png;base64,AAAA", page.locator("#n4MiniQr").getAttribute("src"));
            assertEquals(1, challenges.get());

            page.waitForSelector("#n4MiniComplete:not([hidden])", new Page.WaitForSelectorOptions().setTimeout(6000));
            assertTrue(page.locator("#n4MiniState").textContent().contains("4321"));
            assertTrue(polls.get() >= 1);

            page.locator("#n4MiniComplete").click();
            page.waitForTimeout(150);
            assertEquals(1, completes.get());
        }
    }

    private static void json(Route route, int status, String body) {
        route.fulfill(new Route.FulfillOptions()
                .setStatus(status)
                .setContentType("application/json")
                .setBody(body));
    }

    private static void html(Route route) {
        route.fulfill(new Route.FulfillOptions()
                .setContentType("text/html")
                .setBody("""
                        <body>
                          <form id="n4LoginForm">
                            <button id="n4WechatLogin" type="button">微信扫码登录</button>
                            <div class="n4-mini-links"><a href="/account">旧版账号中心</a></div>
                          </form>
                        </body>
                        """));
    }
}
