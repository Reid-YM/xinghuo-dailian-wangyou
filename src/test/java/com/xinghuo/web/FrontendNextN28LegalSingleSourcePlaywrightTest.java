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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextN28LegalSingleSourcePlaywrightTest {

    private Browser chromium(Playwright playwright) {
        var options = new BrowserType.LaunchOptions();
        String executable = System.getenv("XINGHUO_TEST_CHROMIUM");
        if (executable != null && !executable.isBlank()) {
            options.setExecutablePath(Path.of(executable));
        }
        return playwright.chromium().launch(options);
    }

    private Page setup(Browser browser) throws Exception {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1280, 900));
        String nextHtml = new FrontendNextPreviewController().frontendNext().getBody();
        String rules = """
            <!doctype html><html lang="zh-CN"><head>
            <title>服务规则 - 正式源</title><meta name="description" content="正式规则说明">
            <link rel="canonical" href="https://wydlgf.cn/rules"></head><body>
            <main><div class="rules-card"><span class="eyebrow">服务规则</span><h1>下单前说明</h1>
            <div class="rule-alert">正式规则唯一来源</div>
            <section><h2>报价与确认</h2><p>正式规则正文 <a href="/privacy">隐私说明</a><script>window.__unsafeLegal=1</script></p></section>
            <section><h2>联系客服</h2><p><a href="/contact">一对一咨询</a></p></section>
            </div></main></body></html>
            """;
        String privacy = """
            <!doctype html><html lang="zh-CN"><head>
            <title>隐私说明 - 正式源</title><meta name="description" content="正式隐私说明">
            <link rel="canonical" href="https://wydlgf.cn/privacy"></head><body>
            <main><div class="rules-card"><span class="eyebrow">隐私说明</span><h1>信息收集与处理说明</h1>
            <section><h2>收集范围</h2><p>正式隐私正文 <a href="/rules">服务规则</a></p></section>
            </div></main></body></html>
            """;

        page.route("**/*", route -> {
            try {
                URI uri = URI.create(route.request().url());
                String path = uri.getPath();
                if (path.endsWith(".css")) {
                    route.fulfill(new Route.FulfillOptions().setContentType("text/css")
                            .setPath(Path.of("src/main/resources/static" + path)));
                    return;
                }
                if (path.endsWith(".js")) {
                    route.fulfill(new Route.FulfillOptions().setContentType("application/javascript")
                            .setBody(Files.readString(Path.of("src/main/resources/static" + path))));
                    return;
                }
                if (path.equals("/api/public/legal/rules")) {
                    route.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setBody(rules));
                    return;
                }
                if (path.equals("/api/public/legal/privacy")) {
                    route.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setBody(privacy));
                    return;
                }
                if (path.equals("/api/site-config")) {
                    route.fulfill(new Route.FulfillOptions().setContentType("application/json")
                            .setBody("{\"brandName\":\"星火\",\"quoteEnabled\":true}"));
                    return;
                }
                if (path.equals("/api/public/cases") || path.equals("/api/services")) {
                    route.fulfill(new Route.FulfillOptions().setContentType("application/json").setBody("[]"));
                    return;
                }
                if (path.equals("/api/catalog")) {
                    route.fulfill(new Route.FulfillOptions().setContentType("application/json")
                            .setBody("{\"games\":[],\"categories\":[],\"platforms\":[],\"gamePlatforms\":[]}"));
                    return;
                }
                if (path.equals("/api/account/me")) {
                    route.fulfill(new Route.FulfillOptions().setStatus(401).setContentType("application/json")
                            .setBody("{\"message\":\"请先登录\"}"));
                    return;
                }
                if (path.startsWith("/api/")) {
                    route.fulfill(new Route.FulfillOptions().setStatus(404).setContentType("application/json").setBody("{}"));
                    return;
                }
                route.fulfill(new Route.FulfillOptions()
                        .setContentType("text/html;charset=utf-8")
                        .setHeaders(Map.of("Content-Security-Policy",
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; connect-src 'self'"))
                        .setBody(nextHtml));
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        });
        return page;
    }

    @Test
    void chromiumRebuildsFormalLegalSourceWithoutExecutingFetchedHtml() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = chromium(playwright)) {
            Page page = setup(browser);
            page.navigate("https://example.test/next/rules");
            page.waitForSelector("#n28Legal .n28-document");

            assertTrue(page.locator("#n28Legal").textContent().contains("正式规则正文"));
            assertEquals(0, page.locator("#n28Legal script").count());
            assertFalse(page.locator("#n28Legal .n28-document").textContent().contains("__unsafeLegal"));
            assertEquals(null, page.evaluate("() => window.__unsafeLegal ?? null"));
            assertEquals("/next/privacy", page.getByText("隐私说明", new Page.GetByTextOptions().setExact(true))
                    .last().getAttribute("href"));
            assertEquals("/next/contact", page.locator("#n28Legal a.n28-link[href='/next/contact']")
                    .getAttribute("href"));
            assertEquals("/rules", page.getByText("打开正式原文", new Page.GetByTextOptions().setExact(true))
                    .getAttribute("href"));
            assertEquals("https://wydlgf.cn/rules", page.locator("link[rel=canonical]").getAttribute("href"));
            assertEquals("noindex,follow", page.locator("meta[name=robots]").getAttribute("content"));

            page.navigate("https://example.test/next/privacy");
            page.waitForSelector("#n28Legal .n28-document");
            assertTrue(page.locator("#n28Legal").textContent().contains("正式隐私正文"));
            assertEquals("/next/rules", page.getByText("服务规则", new Page.GetByTextOptions().setExact(true))
                    .last().getAttribute("href"));
        }
    }
}
