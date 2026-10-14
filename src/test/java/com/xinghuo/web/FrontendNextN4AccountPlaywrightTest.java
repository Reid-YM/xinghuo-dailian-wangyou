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
class FrontendNextN4AccountPlaywrightTest {
    private static final Path NEXT = Path.of("src/main/resources/static/next");

    @Test
    void chromiumAnonymousAccountShowsLoginWithoutReadingPrivateCollections() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            AtomicInteger privateReads = new AtomicInteger();
            page.route("**/*", route -> {
                String url = route.request().url();
                if (url.endsWith("/api/account/wechat/availability")) {
                    json(route, 200, "{\"enabled\":true}");
                    return;
                }
                if (url.endsWith("/api/account/me")) {
                    json(route, 401, "{\"message\":\"请先登录\"}");
                    return;
                }
                if (url.contains("/api/account/orders") || url.endsWith("/api/account/sessions")
                        || url.endsWith("/api/account/security/events")) {
                    privateReads.incrementAndGet();
                    json(route, 401, "{\"message\":\"请先登录\"}");
                    return;
                }
                html(route);
            });

            page.navigate("https://example.test/next/account");
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n4.js")));
            page.waitForSelector("#n4LoginForm");

            assertEquals(0, privateReads.get());
            assertTrue(page.locator("#n4WechatLogin").isEnabled());
            assertTrue(page.locator(".n4-login-copy").textContent().contains("只显示当前账号数据"));
        }
    }

    @Test
    void webkitAuthenticatedAccountRendersOnlyServerReturnedOrdersAndProfile() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage();
            page.route("**/*", route -> {
                String url = route.request().url();
                if (url.endsWith("/api/account/wechat/availability")) {
                    json(route, 200, "{\"enabled\":true}");
                    return;
                }
                if (url.endsWith("/api/account/me")) {
                    json(route, 200, "{\"displayName\":\"N4测试用户\",\"avatarUrl\":null,\"contactType\":\"PHONE\",\"contactMasked\":\"138****0000\",\"contactVerified\":true,\"quoteContacts\":[],\"email\":null,\"emailVerified\":false,\"createdAt\":\"2026-10-14T12:00:00Z\",\"entitlements\":[]}");
                    return;
                }
                if (url.contains("/api/account/orders")) {
                    json(route, 200, "{\"items\":[{\"businessNo\":\"XH-O-N4-001\",\"status\":\"IN_PROGRESS\",\"statusText\":\"进行中\",\"progressPercent\":63,\"progressText\":\"挑战任务\",\"gameName\":\"MW4\",\"serviceName\":\"赛季服务\",\"createdAt\":\"2026-10-14T12:00:00Z\",\"updatedAt\":\"2026-10-14T13:00:00Z\"}],\"total\":7,\"page\":0,\"size\":10}");
                    return;
                }
                if (url.endsWith("/api/account/sessions")) {
                    json(route, 200, "[{\"id\":\"s1\",\"ip\":\"127.***.***.1\",\"deviceLabel\":\"Safari\",\"createdAt\":\"2026-10-14T12:00:00Z\",\"lastSeenAt\":\"2026-10-14T13:00:00Z\",\"current\":true}]");
                    return;
                }
                if (url.endsWith("/api/account/security/events")) {
                    json(route, 200, "[{\"title\":\"登录成功\",\"description\":\"账号凭据验证通过并建立登录会话。\",\"ipMasked\":\"127.***.***.1\",\"deviceLabel\":\"Safari\",\"createdAt\":\"2026-10-14T13:00:00Z\"}]");
                    return;
                }
                html(route);
            });

            page.navigate("https://example.test/next/account");
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n4.js")));
            page.waitForSelector("text=欢迎回来，N4测试用户");
            page.waitForSelector("text=XH-O-N4-001");

            assertTrue(page.locator(".n4-profile").textContent().contains("138****0000"));
            assertTrue(page.locator(".n4-table").textContent().contains("赛季服务"));
            assertTrue(page.locator(".n4-table").textContent().contains("63%"));
            assertTrue(page.locator(".n4-stats").textContent().contains("7"));
            assertTrue(page.locator(".n4-stack").textContent().contains("Safari"));
            assertTrue(page.locator(".badge").textContent().contains("N4 ACCOUNT LIVE"));
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
                          <section class="page active" data-page="account"></section>
                          <div class="badge">FRONTEND NEXT</div>
                        </body>
                        """));
    }
}
