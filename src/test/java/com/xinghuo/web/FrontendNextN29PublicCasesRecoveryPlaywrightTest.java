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

class FrontendNextN29PublicCasesRecoveryPlaywrightTest {
    private static final Path RUNTIME =
            Path.of("src/main/resources/static/next/n27-support-trust.js");

    @Test
    void errorAndEmptyAreDistinctStatesWithExplicitRetry() throws Exception {
        String js = Files.readString(RUNTIME);
        assertTrue(js.contains("publicCasesStatus === 'error'"));
        assertTrue(js.contains("publicCasesStatus === 'ready'"));
        assertTrue(js.contains("publicCasesStatus === 'loading'"));
        assertTrue(js.contains("公开案例暂时读取失败，不能据此判断当前没有案例。"));
        assertTrue(js.contains("当前没有公开完成案例。"));
        assertTrue(js.contains("retry.addEventListener('click', () => loadPublicCases())"));
        assertTrue(js.contains("caseGrid.setAttribute('aria-live', 'polite')"));
        assertTrue(js.contains("if (!Array.isArray(body)) throw new Error"));
        assertTrue(js.contains("if (request !== publicCasesRequest) return"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
    void chromiumRecoversFromNetworkFailureToGenuinelyEmptyResult() throws Exception {
        String runtime = Files.readString(RUNTIME);
        AtomicInteger requests = new AtomicInteger(0);
        String html = """
                <!doctype html><html lang="zh-CN"><head><meta charset="UTF-8"></head><body>
                  <div class="page" data-page="home"></div><footer class="next-footer"></footer>
                  <script>
                    window.XHFrontendShell = {
                      configurationReady: Promise.resolve(),
                      configuration: { brandName: '星火', trustTags: [], trustCases: [], trustFaq: [] }
                    };
                  </script>
                  <script src="/next/n27-support-trust.js"></script>
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
                    if (path.equals("/next/n27-support-trust.js")) {
                        route.fulfill(new Route.FulfillOptions().setContentType("application/javascript")
                                .setBody(runtime));
                    } else if (path.equals("/api/public/cases")) {
                        int attempt = requests.incrementAndGet();
                        if (attempt == 1) {
                            route.fulfill(new Route.FulfillOptions().setStatus(503)
                                    .setContentType("application/json").setBody("{}"));
                        } else {
                            route.fulfill(new Route.FulfillOptions().setContentType("application/json")
                                    .setBody("[]"));
                        }
                    } else {
                        route.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8")
                                .setBody(html));
                    }
                });
                page.navigate("https://example.test/next/trust");
                page.waitForSelector("#n27PublicCases button");
                assertTrue(page.locator("#n27PublicCases").textContent()
                        .contains("公开案例暂时读取失败"));
                assertTrue(!page.locator("#n27PublicCases").textContent().contains("当前没有公开完成案例。"));
                assertEquals("error", page.evaluate("() => XHFrontendNextN27.state.publicCasesStatus"));
                page.locator("#n27PublicCases button").click();
                page.waitForFunction("() => XHFrontendNextN27.state.publicCasesStatus === 'ready'");
                assertTrue(page.locator("#n27PublicCases").textContent().contains("当前没有公开完成案例。"));
                assertEquals(0, page.locator("#n27PublicCases button").count());
                assertEquals(2, requests.get(), "retry should perform exactly one additional GET");
            }
        }
    }
}
