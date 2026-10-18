package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class FrontendNextN29SubmissionCatalogRecoveryPlaywrightTest {
    private static final Path RUNTIME =
            Path.of("src/main/resources/static/next/n25-submission-workbench.js");

    @Test
    void failedCatalogHasRetryWithoutResettingDraft() throws Exception {
        String js = Files.readString(RUNTIME);
        assertTrue(js.contains("function showGameError(error)"));
        assertTrue(js.contains("state.id = 'n25GameLoadState'"));
        assertTrue(js.contains("state.setAttribute('role', 'alert')"));
        assertTrue(js.contains("重新读取游戏列表"));
        assertTrue(js.contains("await loadGames()"));
        assertTrue(js.contains("if (!Array.isArray(payload?.games)) throw new Error"));
        assertTrue(js.contains("const selected = text(select.value) || text(draftValue()?.gameCode)"));
        assertTrue(js.contains("$('#n25GameLoadState')?.remove()"));
        assertTrue(js.contains("showGameError(error)"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
    void chromiumCatalogRetryRestoresOptionsWithoutOverwritingTypedTitle() throws Exception {
        String runtime = Files.readString(RUNTIME);
        AtomicInteger catalogCalls = new AtomicInteger();
        String html = """
                <!doctype html><html lang="zh-CN"><head><meta charset="utf-8"></head><body>
                <div class="page" data-page="home"></div><footer class="next-footer"></footer>
                <script src="/next/n25-submission-workbench.js"></script>
                </body></html>
                """;
        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions();
            String executable = System.getenv("XINGHUO_TEST_CHROMIUM");
            if (executable != null && !executable.isBlank()) {
                options.setExecutablePath(Path.of(executable));
            }
            try (Browser browser = playwright.chromium().launch(options)) {
                Page page = browser.newPage();
                page.route("**/*", route -> {
                    String path = URI.create(route.request().url()).getPath();
                    if (path.equals("/next/n25-submission-workbench.js")) {
                        route.fulfill(new Route.FulfillOptions().setContentType("application/javascript")
                                .setBody(runtime));
                    } else if (path.equals("/api/catalog")) {
                        if (catalogCalls.incrementAndGet() == 1) {
                            route.fulfill(new Route.FulfillOptions().setStatus(503)
                                    .setContentType("application/json").setBody("{}"));
                        } else {
                            route.fulfill(new Route.FulfillOptions().setContentType("application/json")
                                    .setBody("{\"games\":[{\"code\":\"CODMW4\",\"name\":\"使命召唤 MW4\"}]}"));
                        }
                    } else if (path.equals("/api/account/me")) {
                        route.fulfill(new Route.FulfillOptions().setStatus(401)
                                .setContentType("application/json")
                                .setBody("{\"message\":\"请先登录\"}"));
                    } else if (path.startsWith("/api/")) {
                        route.fulfill(new Route.FulfillOptions().setStatus(404)
                                .setContentType("application/json").setBody("{}"));
                    } else {
                        route.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8")
                                .setBody(html));
                    }
                });
                page.navigate("https://example.test/next/account/submit");
                page.waitForSelector("#n25GameLoadState button");
                assertEquals(1, catalogCalls.get());
                page.locator("#n25Title").fill("保留中的投稿标题");
                page.locator("#n25GameLoadState button").click();
                page.waitForFunction("() => !!document.querySelector(\"#n25Game option[value='CODMW4']\")");
                assertEquals(2, catalogCalls.get());
                assertEquals("保留中的投稿标题", page.locator("#n25Title").inputValue());
                assertEquals(0, page.locator("#n25GameLoadState").count());
                assertTrue(page.locator("#n25Game").textContent().contains("使命召唤 MW4"));
            }
        }
    }
}
