package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextServicePagesPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void desktopServiceHallUsesWideFilterConsoleAndFourCards() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1920, 1080));
            page.setContent("""
                    <body data-page="services">
                      <main class="container section page-shell services-browser">
                        <div class="service-browser-hero"><h1>COD MW4 / DMZ / 战区 任务服务</h1></div>
                        <section class="service-browser-console">
                          <div class="service-browser-search"><span class="search-icon">⌕</span><input placeholder="搜索游戏或服务"></div>
                          <div class="browser-row"><span class="browser-label">游戏</span><div class="filters"><button class="filter-btn active">全部游戏</button><button class="filter-btn">MW4</button></div></div>
                          <div class="browser-row"><span class="browser-label">平台</span><div class="filters"><button class="filter-btn active">全部平台</button></div></div>
                          <div class="browser-row"><span class="browser-label">需求</span><div class="filters"><button class="filter-btn active">全部需求</button></div></div>
                          <div class="service-browser-summary">共 4 项服务</div>
                        </section>
                        <div class="grid4 section service-browser-grid">
                          <a class="service-card card"><div class="service-cover"></div><h3>武器升级</h3><p class="muted">说明</p><div class="service-card-facts"><span><b>平台</b><i>Steam</i></span></div><div class="service-meta"><strong>¥20 起</strong><span>按进度</span></div><div class="service-card-action"><span>详情</span><span>咨询 →</span></div></a>
                          <a class="service-card card"><h3>迷彩</h3></a><a class="service-card card"><h3>活动</h3></a><a class="service-card card"><h3>排位</h3></a>
                        </div>
                      </main>
                    </body>
                    """);
            loadStyles(page);

            assertNoOverflow(page);
            String columns = String.valueOf(page.evaluate("() => getComputedStyle(document.querySelector('.service-browser-console')).gridTemplateColumns"));
            assertTrue(columns.split(" ").length >= 2, "desktop filter console should have search + filter columns");
            Number firstRow = (Number) page.evaluate("""
                    () => {
                      const cards=[...document.querySelectorAll('.service-browser-grid>.service-card')];
                      const top=cards[0].getBoundingClientRect().top;
                      return cards.filter(c => Math.abs(c.getBoundingClientRect().top-top)<1).length;
                    }
                    """);
            assertEquals(4, firstRow.intValue());
        }
    }

    @Test
    void serviceHallKeepsServerCardsOnFailureThenRecoversAndClearsEmptySearch() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <body data-page="services"><main>
                      <section class="service-browser-console">
                        <input id="publicServiceSearch" type="search">
                        <div id="publicGameFilters"></div><div id="publicPlatformFilters"></div>
                        <div id="publicCategoryFilters"></div>
                        <div id="publicServiceSummary">正在读取服务…</div>
                      </section>
                      <div id="list"><a class="service-card" href="/service/server-card">服务端卡片</a></div>
                    </main></body>
                    """);
            page.evaluate("""
                    () => {
                      let calls = 0;
                      window.XH = {
                        $: selector => document.querySelector(selector),
                        loadConfig: async () => ({}),
                        loadServices: async () => {
                          if (++calls === 1) throw new Error('temporary service outage');
                          return [{ gameCode: 'CODMW4', category: 'CAMO', name: '迷彩服务', shortDesc: '可预约' }];
                        },
                        loadTaxonomy: async () => ({
                          games: [{ code: 'CODMW4', name: '使命召唤' }], categories: [],
                          platforms: [], gamePlatforms: []
                        }),
                        esc: value => String(value), gameLabel: () => '使命召唤',
                        categoryLabel: () => '迷彩',
                        serviceCard: service => `<a class="service-card" href="/service/camo">${service.name}</a>`
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("pages.js")));
            page.waitForSelector(".service-browser-retry");
            assertTrue(page.locator("#list").textContent().contains("服务端卡片"));

            page.locator(".service-browser-retry").click();
            page.waitForSelector("#list .service-card:has-text('迷彩服务')");
            assertEquals("true", page.locator("[data-game-filter='all']").getAttribute("aria-pressed"));

            page.locator("#publicServiceSearch").fill("不存在的服务");
            page.waitForSelector("[data-reset-service-filters]");
            page.locator("[data-reset-service-filters]").click();
            assertEquals("", page.locator("#publicServiceSearch").inputValue());
            assertTrue(page.locator("#list").textContent().contains("迷彩服务"));
        }
    }

    @Test
    void serviceFiltersStayShareableAcrossRefreshAndBrowserBack() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.route("**/*", route -> route.fulfill(new Route.FulfillOptions()
                    .setContentType("text/html")
                    .setBody("""
                            <body data-page="services"><main>
                              <input id="publicServiceSearch" type="search">
                              <div id="publicGameFilters"></div><div id="publicPlatformFilters"></div>
                              <div id="publicCategoryFilters"></div>
                              <div id="publicServiceSummary"></div><div id="list"></div>
                            </main></body>
                            """)));
            page.navigate("https://example.test/services?game=CODMW4&category=camo&q=%E8%BF%B7%E5%BD%A9");
            page.evaluate("""
                    () => {
                      window.XH = {
                        $: selector => document.querySelector(selector),
                        loadConfig: async () => ({}),
                        loadServices: async () => [
                          { gameCode: 'CODMW4', category: 'CAMO', name: '迷彩服务' },
                          { gameCode: 'CODMW4', category: 'LEVEL', name: '升级服务' }
                        ],
                        loadTaxonomy: async () => ({
                          games: [{ code: 'CODMW4', name: '使命召唤' }],
                          categories: [], platforms: [], gamePlatforms: []
                        }),
                        esc: value => String(value), gameLabel: () => '使命召唤',
                        categoryLabel: code => code === 'CAMO' ? '迷彩' : '升级',
                        serviceCard: service => `<a class="service-card">${service.name}</a>`
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("pages.js")));
            page.waitForSelector("#list .service-card");
            assertEquals("迷彩", page.locator("#publicServiceSearch").inputValue());
            assertEquals("迷彩服务", page.locator("#list").textContent());
            assertEquals("true", page.locator("[data-category-filter='camo']").getAttribute("aria-pressed"));

            page.locator("[data-category-filter='level']").click();
            assertTrue(page.url().contains("category=level"));
            page.evaluate("history.back()");
            page.waitForFunction("() => new URL(location.href).searchParams.get('category') === 'camo'");
            assertEquals("迷彩服务", page.locator("#list").textContent());

            page.locator("#publicServiceSearch").fill("升级");
            assertTrue(page.url().contains("q=%E5%8D%87%E7%BA%A7"));
            page.reload();
            page.evaluate("""
                    () => { window.XH = {
                      $: selector => document.querySelector(selector), loadConfig: async () => ({}),
                      loadServices: async () => [{ gameCode: 'CODMW4', category: 'CAMO', name: '迷彩服务' }],
                      loadTaxonomy: async () => ({ games: [{ code: 'CODMW4', name: '使命召唤' }], categories: [], platforms: [], gamePlatforms: [] }),
                      esc: value => String(value), gameLabel: () => '使命召唤', categoryLabel: () => '迷彩',
                      serviceCard: service => `<a class="service-card">${service.name}</a>`
                    }; }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("pages.js")));
            page.waitForSelector("[data-reset-service-filters]");
            assertEquals("升级", page.locator("#publicServiceSearch").inputValue());
        }
    }

    @Test
    void detailDesktopAndMobileKeepConversionReadable() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1440, 900));
            renderDetail(page);
            loadStyles(page);
            assertNoOverflow(page);
            String desktopColumns = String.valueOf(page.evaluate("() => getComputedStyle(document.querySelector('.detail-hero')).gridTemplateColumns"));
            assertTrue(desktopColumns.split(" ").length >= 2, "desktop detail hero should be two-column");

            page.setViewportSize(390, 844);
            assertNoOverflow(page);
            String mobileColumns = String.valueOf(page.evaluate("() => getComputedStyle(document.querySelector('.detail-hero')).gridTemplateColumns"));
            assertEquals(1, mobileColumns.split(" ").length, "mobile detail hero must stack");
            assertEquals("none", page.evaluate("() => getComputedStyle(document.querySelector('.mobile-dock a:nth-child(3)')).display"));
            assertEquals("none", page.evaluate("() => getComputedStyle(document.querySelector('.mobile-dock a:nth-child(4)')).display"));
            Number ctaHeight = (Number) page.evaluate("() => document.querySelector('.detail-hero .btn.primary').getBoundingClientRect().height");
            assertTrue(ctaHeight.doubleValue() >= 44.0);
        }
    }

    private static void renderDetail(Page page) {
        page.setContent("""
                <body data-page="service-detail">
                  <main class="container section page-shell service-detail-page">
                    <nav class="guide-breadcrumb"><a>首页</a><span>/</span><a>服务大厅</a><span>/</span><span>武器升级</span></nav>
                    <section class="service-detail-panel"><div class="detail-hero">
                      <div><div class="service-state">可预约</div><span class="eyebrow">使命召唤 MW4</span><h1>使命召唤 MW4 武器升级</h1><p>按当前进度确认报价</p><div class="detail-list"><strong>参考价格</strong><span>¥20 起</span><strong>预计时间</strong><span>按进度</span></div><div class="actions"><a class="btn primary">提前询价</a><a class="btn ghost">联系客服</a></div></div>
                      <div class="service-cover"></div>
                    </div></section>
                    <section class="card"><h2>服务说明</h2><p>说明内容</p></section>
                  </main>
                  <nav class="mobile-dock"><a>首页</a><a>服务</a><a>攻略</a><a>工具站</a><a class="primary">提前询价</a></nav>
                </body>
                """);
    }

    private static void loadStyles(Page page) {
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("site-experience-v2.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("frontend-next-foundation.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("frontend-next-services.css")));
    }

    private static void assertNoOverflow(Page page) {
        Number scroll = (Number) page.evaluate("() => document.documentElement.scrollWidth");
        Number client = (Number) page.evaluate("() => document.documentElement.clientWidth");
        assertTrue(scroll.doubleValue() <= client.doubleValue() + 1,
                () -> "horizontal overflow: " + scroll + " > " + client);
    }
}
