package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextQuoteAuditPlaywrightTest {
    private static final Path NEXT = Path.of("src/main/resources/static/next");
    private static final String CATALOG = """
            {"games":[{"code":"CODMW4","name":"使命召唤 MW4"}],
             "categories":[{"code":"LEVEL","gameCode":"CODMW4","name":"等级"},
                           {"code":"CAMO","gameCode":"CODMW4","name":"迷彩"}],
             "platforms":[],"gamePlatforms":[]}
            """;
    private static final String LEVEL = """
            [{"id":1,"slug":"level","gameCode":"CODMW4","category":"LEVEL",
              "name":"真实等级服务","shortDesc":"按实际进度确认"}]
            """;
    private static final String NEWER = """
            [{"id":2,"slug":"newer","gameCode":"CODMW4","category":"LEVEL",
              "name":"最新真实服务"}]
            """;
    private static final String HTML = """
            <!doctype html><html lang="zh-CN"><head><meta charset="utf-8"></head><body>
            <section class="page" data-page="quote">
              <div class="formSec"><div class="formGrid">
                <select id="qGame"><option value="">正在读取游戏</option></select>
                <select id="qCat"><option value="">正在读取分类</option></select>
                <select id="qSvc"><option value="">正在读取服务</option></select>
              </div></div>
              <div class="formSec"><textarea id="goal"></textarea></div>
              <div class="formSec"><div class="formGrid">
                <div class="field"><input placeholder="称呼"></div>
                <div class="field"><input placeholder="联系方式"></div>
              </div></div>
              <button type="button" id="submit">提交询价</button>
            </section>
            <section class="page services"><div class="side"></div><div class="serviceGrid"></div></section>
            </body></html>
            """;

    private static void json(Route route, String data) {
        route.fulfill(new Route.FulfillOptions().setStatus(200)
                .setContentType("application/json").setBody(data));
    }

    private static void installScripts(Page page, boolean quote) {
        page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n2.js")));
        if (quote) page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n3.js")));
    }

    @Test
    void bothEnginesRejectPlaceholderGameAndServiceWithoutCreatingQuote() {
        try (Playwright playwright = Playwright.create();
             Browser chromium = playwright.chromium().launch();
             Browser webkit = playwright.webkit().launch()) {
            for (Browser browser : List.of(chromium, webkit)) {
                Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
                try {
                    AtomicInteger quotePosts = new AtomicInteger();
                    page.route("**/*", route -> {
                        String path = URI.create(route.request().url()).getPath();
                        if (path.equals("/api/catalog")) json(route, CATALOG);
                        else if (path.equals("/api/services")) json(route, LEVEL);
                        else if (path.equals("/api/quote")) {
                            quotePosts.incrementAndGet();
                            json(route, "{\"businessNo\":\"XHABCDEF1234\"}");
                        } else route.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(HTML));
                    });
                    page.navigate("https://example.test/next/quote");
                    installScripts(page, true);
                    page.waitForFunction("() => document.querySelector('#qSvc')?.value === 'level'");
                    page.locator("#n3ContactMethod").selectOption("微信");
                    page.locator("#n3CustomerContact").fill("test-contact");
                    page.locator("#n3PrivacyConsent").check();

                    // An available game with an empty service category must not silently
                    // fall back to a service from a different category.
                    page.locator("#qCat").selectOption("CAMO");
                    assertEquals("", page.locator("#qSvc").inputValue());
                    assertEquals("暂无服务", page.locator("#qSvc option").first().textContent());
                    page.locator("#submit").click();
                    assertTrue(page.locator("#n3QuoteMessage").textContent().contains("请选择具体服务"));
                    assertEquals(0, quotePosts.get());

                    // A placeholder game cannot become the literal submitted game name.
                    page.locator("#qCat").selectOption("LEVEL");
                    page.locator("#qGame").evaluate("el => {el.add(new Option('请选择游戏',''),0);el.value='';}");
                    assertEquals("level", page.locator("#qSvc").inputValue());
                    page.locator("#submit").click();
                    assertTrue(page.locator("#n3QuoteMessage").textContent().contains("请选择游戏"));
                    assertEquals(0, quotePosts.get());

                    page.locator("#qGame").selectOption("CODMW4");
                    page.locator("#submit").click();
                    page.waitForSelector("#n3QuoteMessage:has-text('XHABCDEF1234')");
                    assertEquals(1, quotePosts.get(), "only a real chosen service may create a quote");
                } finally { page.close(); }
            }
        }
    }

    @Test
    void supersededCatalogReloadCannotOverwriteNewerServices() {
        try (Playwright playwright = Playwright.create();
             Browser chromium = playwright.chromium().launch();
             Browser webkit = playwright.webkit().launch()) {
            for (Browser browser : List.of(chromium, webkit)) {
                Page page = browser.newPage();
                try {
                    AtomicInteger serviceReads = new AtomicInteger();
                    List<Route> held = new ArrayList<>();
                    page.route("**/*", route -> {
                        String path = URI.create(route.request().url()).getPath();
                        if (path.equals("/api/catalog")) json(route, CATALOG);
                        else if (path.equals("/api/services")) {
                            if (serviceReads.incrementAndGet() == 1) held.add(route);
                            else json(route, NEWER);
                        } else route.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(HTML));
                    });
                    page.navigate("https://example.test/next/quote");
                    installScripts(page, false);
                    page.waitForCondition(() -> held.size() == 1);
                    page.evaluate("() => window.XHFrontendNextN2.reload()");
                    page.waitForFunction("() => window.XHFrontendNextN2.state.services?.[0]?.slug === 'newer'");
                    json(held.get(0), LEVEL);
                    page.waitForTimeout(150);
                    assertEquals("newer", page.evaluate(
                            "() => window.XHFrontendNextN2.state.services[0].slug"));
                    assertEquals("newer", page.locator("#qSvc").inputValue());
                } finally { page.close(); }
            }
        }
    }
}
