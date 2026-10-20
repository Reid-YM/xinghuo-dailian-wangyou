package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.xinghuo.service.TaxonomyService;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextGamePlaywrightTest {
    private static final String CATALOG = """
        {"games":[{"code":"CODMW4","name":"测试游戏","nameEn":"Test Game","seoSlug":"test-game",
          "description":"游戏介绍 <img src=x onerror=unsafe>","publisher":"测试发行商","genres":"动作 / 合作",
          "coverImage":"/test-cover.svg","logoImage":"javascript:unsafe","active":true,"platformCodes":["PC"]}],
          "platforms":[{"code":"PC","name":"电脑"}],"categories":[],"gamePlatforms":[]}
        """;
    private static final String SERVICES = """
        [{"id":1,"slug":"test-service","gameCode":"CODMW4","name":"测试服务","shortDesc":"按进度确认",
          "priceMode":"FIXED","priceFixed":25,"status":"AVAILABLE","durationText":"约两天"},
         {"id":2,"slug":"offline","gameCode":"CODMW4","name":"下架服务","priceMode":"QUOTE","status":"OFFLINE"},
         {"id":3,"slug":"other-game","gameCode":"OTHER","name":"其他游戏服务"}]
        """;
    @Test void gameProfileHasRealFieldsServicesSafeTextAndNativeNavigationInBothEngines() throws Exception {
        try (Playwright p = Playwright.create(); Browser chrome = p.chromium().launch(); Browser webkit = p.webkit().launch()) {
            for (Browser browser : List.of(chrome, webkit)) for (int width : new int[]{320,1280}) {
                try (Fixture f = new Fixture(browser, width)) {
                    f.page.navigate("https://example.test/next");
                    f.page.locator("#nextHomeGames a").click();
                    f.page.waitForSelector("#nextGameDetail h1");
                    assertEquals("/next/game/test-game", URI.create(f.page.url()).getPath());
                    assertTrue(f.page.locator(".next-game-hero").textContent().contains("游戏介绍 <img src=x onerror=unsafe>"));
                    assertTrue(f.page.locator(".next-game-facts").textContent().contains("测试发行商"));
                    assertTrue(f.page.locator(".next-game-facts").textContent().contains("动作 / 合作"));
                    assertTrue(f.page.locator(".next-game-facts").textContent().contains("电脑"));
                    assertEquals(0, f.page.locator(".next-game-hero img[src^='javascript:']").count());
                    assertEquals(2, f.page.locator(".next-game-card").count());
                    assertEquals(0, f.page.locator(".next-game-card[data-service-slug=other-game]").count());
                    assertTrue(f.page.locator(".next-game-card[data-service-slug=test-service]").textContent().contains("¥25"));
                    assertEquals(0, f.page.locator(".next-game-card[data-service-slug=offline] a[href*='/quote']").count());
                    assertEquals(Boolean.TRUE, f.page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
                    assertEquals(1, f.page.getByRole(AriaRole.MAIN).count());
                    assertTrue(f.page.getByRole(AriaRole.LINK,new Page.GetByRoleOptions().setName("重新打开游戏资料")).getAttribute("href").endsWith("/game/test-game"));
                    if (browser.browserType().name().equals("chromium")) {
                        f.page.evaluate("async () => { await Promise.all(document.querySelector('.page.active').getAnimations().map(a => a.finished.catch(() => {}))); }");
                        Files.createDirectories(Path.of("target/frontend-review"));
                        f.page.screenshot(new Page.ScreenshotOptions().setFullPage(true).setPath(Path.of("target/frontend-review/game-"+width+".png")));
                    }
                    f.page.locator(".next-game-card[data-service-slug=test-service]").getByRole(AriaRole.LINK,new Locator.GetByRoleOptions().setName("查看详情")).click();
                    f.page.waitForSelector("#n10ServiceDetail h1");
                    assertEquals("/next/service/test-service", URI.create(f.page.url()).getPath());
                    f.page.goBack(); f.page.waitForSelector("#nextGameDetail h1");
                    f.page.locator(".next-game-card[data-service-slug=test-service]").getByRole(AriaRole.LINK,new Locator.GetByRoleOptions().setName("询价").setExact(true)).click();
                    f.page.waitForSelector("#submit[data-n3-real-submit=true]");
                    assertEquals("service=test-service", URI.create(f.page.url()).getQuery());
                    assertEquals(List.of(), f.errors);
                }
            }
        }
    }
    @Test void failedCatalogCanRetryAndEmptyServicesKeepTheGameProfile() throws Exception {
        try (Playwright p = Playwright.create(); Browser chrome = p.chromium().launch(); Browser webkit = p.webkit().launch()) {
            for (Browser browser : List.of(chrome, webkit)) try (Fixture f = new Fixture(browser,390)) {
                f.fail.set(true);
                f.page.navigate("https://example.test/next/game/test-game");
                f.page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("重新读取").setExact(true)).last().waitFor();
                f.fail.set(false); f.empty.set(true);
                f.page.locator("#nextGameDetail").getByRole(AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("重新读取")).click();
                f.page.waitForSelector(".next-game-hero h1");
                assertTrue(f.page.locator(".next-game-services").textContent().contains("当前暂无公开服务"));
                f.empty.set(false); f.maintenance.set(true); f.page.reload();
                f.page.waitForSelector(".next-game-hero h1");
                assertTrue(f.page.locator(".next-game-card[data-service-slug=test-service]").textContent().contains("可预约"));
                assertFalse(f.page.locator(".next-game-card[data-service-slug=test-service]").textContent().contains("可接单"));
                assertEquals(List.of(), f.errors);
            }
        }
    }
    static class Fixture implements AutoCloseable {
        final Page page; final List<String> errors = new ArrayList<>();
        final AtomicBoolean fail = new AtomicBoolean(), empty = new AtomicBoolean(), maintenance = new AtomicBoolean();
        Fixture(Browser browser, int width) {
            var preview = new FrontendNextPreviewController(); var taxonomy = mock(TaxonomyService.class);
            when(taxonomy.publicGameBySeoSlug("test-game")).thenReturn(FrontendNextGameControllerTest.game());
            var mvc = MockMvcBuilders.standaloneSetup(preview,new FrontendNextGameController(preview,taxonomy)).build();
            page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width,900)); page.onPageError(errors::add);
            page.route("**/*", route -> {
                String path = URI.create(route.request().url()).getPath();
                if (path.equals("/api/catalog")) json(route,fail.get()?503:200,fail.get()?"{}":CATALOG);
                else if(path.equals("/api/services")) json(route,200,empty.get()?"[]":SERVICES);
                else if(path.equals("/api/site-config")) json(route,200,"{\"brandName\":\"测试星火\",\"quoteEnabled\":true,\"serviceStatus\":\""+(maintenance.get()?"maintenance":"open")+"\"}");
                else if(path.equals("/test-cover.svg")) route.fulfill(new Route.FulfillOptions().setContentType("image/svg+xml").setBody("<svg xmlns='http://www.w3.org/2000/svg' width='800' height='450'><rect width='800' height='450' fill='#203245'/></svg>"));
                else FrontendNextN30DocumentJourneyPlaywrightTest.fulfill(route,mvc,new HashSet<>());
            });
        }
        public void close(){page.close();}
        void json(Route route,int status,String body){route.fulfill(new Route.FulfillOptions().setStatus(status).setContentType("application/json").setBody(body));}
    }
}
