package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextN30GuideCommunityPlaywrightTest {
    @org.junit.jupiter.api.RepeatedTest(3) void guideDiscussionAndFavoritesUseExistingApisAndRetainFailedComment() {
        var preview = new FrontendNextPreviewController();
        var mvc = MockMvcBuilders.standaloneSetup(preview).build();
        try (Playwright playwright = Playwright.create(); Browser chromium = playwright.chromium().launch();
             Browser webkit = playwright.webkit().launch()) {
            for (Browser browser : List.of(chromium, webkit)) {
                Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
                page.context().tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
                boolean verified = false;
                AtomicInteger posts = new AtomicInteger();
                AtomicInteger favorites = new AtomicInteger();
                AtomicInteger reports = new AtomicInteger();
                AtomicBoolean logged = new AtomicBoolean(true);
                AtomicReference<String> posted = new AtomicReference<>();
                page.route("**/*", route -> {
                    String path = URI.create(route.request().url()).getPath();
                    if (path.equals("/api/account/me")) {
                        json(route, logged.get() ? 200 : 401, "{\"displayName\":\"测试读者\"}");
                    } else if (path.equals("/api/account/favorites")) {
                        json(route, 200, "[]");
                    } else if (path.equals("/api/account/favorites/toggle")) {
                        assertEquals("fetch", route.request().headers().get("x-requested-with"));
                        assertTrue(route.request().postData().contains("test-guide"));
                        favorites.incrementAndGet(); json(route, 200, "{\"favorited\":true}");
                    } else if (path.equals("/api/account/comments") && route.request().method().equals("POST")) {
                        posted.set(route.request().postData());
                        assertEquals("fetch", route.request().headers().get("x-requested-with"));
                        json(route, posts.incrementAndGet() == 1 ? 503 : 200, "{\"message\":\"测试响应\"}");
                    } else if (path.equals("/api/account/comments")) {
                        json(route, 200, "[{\"id\":7,\"status\":\"PUBLISHED\",\"authorName\":\"其他读者\",\"content\":\"<img src=x onerror=alert(1)>\"}]");
                    } else if (path.equals("/api/account/comments/7/report")) {
                        assertEquals("POST", route.request().method());
                        assertEquals("fetch", route.request().headers().get("x-requested-with"));
                        reports.incrementAndGet(); json(route, 200, "{}");
                    } else if (path.equals("/api/account/comments/mine")) {
                        json(route, 200, posts.get() > 1
                                ? "[{\"id\":8,\"status\":\"PENDING\",\"content\":\"等待审核的补充\",\"mine\":true},{\"id\":9,\"status\":\"DELETED\",\"content\":\"已删除内容\"}]" : "[]");
                    } else {
                        FrontendNextN30DocumentJourneyPlaywrightTest.fulfill(route, mvc, new HashSet<>());
                    }
                });
                try {
                    page.navigate("https://example.test/next/guides/test-guide");
                    page.waitForSelector("#n22GuideDetail [data-comment-form]:not([hidden])");
                    page.waitForSelector(".gc-comment");
                    assertEquals(0, page.locator(".gc-comment img").count());
                    page.waitForResponse(response -> response.url().endsWith("/api/account/favorites/toggle"),
                            () -> page.locator("[data-favorite-toggle]").click());
                    page.waitForFunction("() => !document.querySelector('[data-favorite-toggle]').disabled");
                    assertEquals("已收藏", page.locator("[data-favorite-label]").textContent(),
                            "writes=" + favorites.get() + " note=" + page.locator("[data-community-note]").textContent());
                    assertEquals(1, favorites.get());
                    page.onDialog(Dialog::accept);
                    page.locator(".gc-comment-actions button").nth(1).click();
                    page.waitForFunction("() => document.querySelector('[data-community-note]').textContent.includes('举报已提交')");
                    assertEquals(1, reports.get());
                    page.locator(".gc-comment-actions button").first().click();
                    page.locator(".gc-input").fill("等待审核的补充");
                    page.locator("[data-comment-submit]").click();
                    page.waitForSelector("[data-comment-error]:not([hidden])");
                    assertEquals("等待审核的补充", page.locator(".gc-input").inputValue());
                    page.locator("[data-comment-submit]").click();
                    page.waitForFunction("() => document.querySelector('[data-comment-list]').textContent.includes('待审核')");
                    assertEquals(2, posts.get());
                    var data = new com.fasterxml.jackson.databind.ObjectMapper().readTree(posted.get());
                    assertEquals("test-guide", data.path("targetId").asText());
                    assertEquals(7, data.path("parentId").asInt());
                    assertEquals("", page.locator(".gc-input").inputValue());
                    assertFalse(page.locator("[data-comment-list]").textContent().contains("已删除内容"));
                    java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/frontend-review"));
                    page.evaluate("() => scrollTo(0, 0)");
                    page.screenshot(new Page.ScreenshotOptions().setFullPage(true).setPath(
                            java.nio.file.Path.of("target/frontend-review/guide-" + browser.browserType().name() + ".png")));
                    logged.set(false);
                    page.reload();
                    page.waitForFunction("() => document.querySelector('[data-community-note]')?.textContent.includes('登录后')");
                    page.locator("[data-favorite-toggle]").click();
                    page.waitForURL("**/next/account?next=*");
                    assertTrue(page.url().contains("%2Fnext%2Fguides%2Ftest-guide"));
                    verified = true;
                } catch (Exception e) { throw new RuntimeException(e); }
                finally {
                    try {
                        if (verified) page.context().tracing().stop();
                        else {
                            new java.io.File("target/frontend-review").mkdirs();
                            page.context().tracing().stop(new Tracing.StopOptions().setPath(java.nio.file.Path.of(
                                    "target/frontend-review/guide-failure-" + browser.browserType().name() + "-" + System.nanoTime() + ".zip")));
                        }
                    } finally { page.close(); }
                }
            }
        }
    }
    private static void json(Route route, int status, String body) {
        route.fulfill(new Route.FulfillOptions().setStatus(status).setContentType("application/json").setBody(body));
    }
}
