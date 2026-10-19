package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextN30SubmissionDraftPlaywrightTest {
    @Test void draftSurvivesImmediateExitAndFailedPostWithoutReappearingAfterSuccess() {
        try (Playwright playwright = Playwright.create(); Browser chromium = playwright.chromium().launch();
             Browser webkit = playwright.webkit().launch()) {
            for (Browser browser : List.of(chromium, webkit)) verifyDraft(browser);
        }
    }

    private void verifyDraft(Browser browser) {
        Page page = browser.newPage();
        AtomicInteger accountReads = new AtomicInteger();
        AtomicInteger writes = new AtomicInteger();
        page.route("**/*", route -> {
            String path = URI.create(route.request().url()).getPath();
            if (path.equals("/next/n25-submission-workbench.js")) {
                route.fulfill(new Route.FulfillOptions().setContentType("application/javascript")
                        .setPath(Path.of("src/main/resources/static/next/n25-submission-workbench.js")));
            } else if (path.equals("/api/catalog")) {
                json(route, 200, "{\"games\":[{\"code\":\"CODMW4\",\"name\":\"使命召唤\"}]}");
            } else if (path.equals("/api/account/me")) {
                json(route, accountReads.incrementAndGet() == 1 ? 503 : 200, "{\"displayName\":\"测试账号\"}");
            } else if (path.equals("/api/account/submissions") && route.request().method().equals("POST")) {
                assertEquals("fetch", route.request().headers().get("x-requested-with"));
                json(route, writes.incrementAndGet() == 1 ? 503 : 200, "{\"message\":\"测试响应\"}");
            } else if (path.startsWith("/api/")) {
                json(route, 200, "[]");
            } else {
                route.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setBody("""
                    <html><head><meta charset="utf-8"></head><body>
                    <section class="page active" data-page="home"></section><footer class="next-footer"></footer>
                    <script src="/next/n25-submission-workbench.js"></script></body></html>
                    """));
            }
        });
        try {
            page.navigate("https://example.test/next/account/submit");
            page.waitForSelector("#n25Auth[data-state=error]");
            assertTrue(page.locator("#n25Submit").isDisabled());
            page.locator("#n25Title").fill("离页前的标题");
            page.locator("#n25Content").fill("离页前的正文");
            // Flush before the 350ms debounce expires, as mobile tab suspension does.
            page.evaluate("() => window.dispatchEvent(new Event('pagehide'))");
            assertEquals("离页前的正文", page.evaluate("() => JSON.parse(localStorage.getItem('xinghuo:submission-draft:v1')).contentMd"));
            page.locator("#n25AuthAction").click();
            page.waitForSelector("#n25Auth[data-state=ok]");
            assertEquals("离页前的标题", page.locator("#n25Title").inputValue());
            page.reload();
            page.waitForSelector("#n25Auth[data-state=ok]");
            assertEquals("离页前的正文", page.locator("#n25Content").inputValue());
            page.locator("#n25Game").selectOption("CODMW4");
            page.locator("#n25Submit").click();
            page.waitForSelector("#n25Message[data-kind=error]");
            assertEquals(1, writes.get());
            assertEquals("离页前的正文", page.locator("#n25Content").inputValue());
            assertFalse(page.locator("#n25Submit").isDisabled());
            page.locator("#n25Submit").click();
            page.waitForSelector("#n25Message[data-kind=ok]");
            page.evaluate("() => new Promise(resolve => setTimeout(resolve, 500))");
            assertEquals(2, writes.get());
            assertNull(page.evaluate("() => localStorage.getItem('xinghuo:submission-draft:v1')"));
            page.reload();
            page.waitForSelector("#n25Auth[data-state=ok]");
            assertEquals("", page.locator("#n25Content").inputValue());
        } finally { page.close(); }
    }
    private static void json(Route route, int status, String body) {
        route.fulfill(new Route.FulfillOptions().setStatus(status).setContentType("application/json").setBody(body));
    }
}
