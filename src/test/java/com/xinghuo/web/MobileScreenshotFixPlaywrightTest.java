package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class MobileScreenshotFixPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void chromium390KeepsHomeActionsAlignedAndFloatingSupportSmall() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
            page.setContent("""
                    <!doctype html>
                    <html data-frontend="mobile">
                    <body class="mobile-frontend" data-page="home">
                      <div id="announcementBar" class="announcement">MW4 正式版预约中 · 现可提前锁定档期，上线后按顺序开工</div>
                      <main class="container">
                        <section id="homeHero" class="hero hero-r3 image-controlled-background has-background">
                          <div class="hero-copy">
                            <div class="hero-signal"><div class="service-state">● MW4 正式版预约中</div><span class="hero-kicker">CODMW4 · 可靠手打 · 纯绿服务</span></div>
                            <h1>使命召唤 MW4 任务代练</h1>
                            <p class="hero-subtitle">武器升级 · 迷彩解锁 · 活动任务</p>
                            <div class="hero-promise"><span><b>可靠手打</b></span><span><b>纯绿服务</b></span><span><b>一号到底</b></span></div>
                            <div class="hero-price-row"><div class="price">¥20 起</div></div>
                            <div class="actions hero-actions">
                              <a id="primaryAction" class="btn primary" href="#">提前询价</a>
                              <a id="secondaryAction" class="btn ghost" href="#">查看服务大厅</a>
                              <a id="progressAction" class="btn ghost" href="#">查业务进度</a>
                            </div>
                          </div>
                        </section>
                      </main>
                      <a class="kefu-float" href="#"><span class="kefu-image-button"><span class="kefu-icon">●</span><span>客服</span></span></a>
                      <nav class="mobile-primary-nav"><a href="#">首页</a><a href="#">服务</a><a class="mobile-nav-quote" href="#"><span class="mobile-nav-icon-wrap">●</span><span>询价</span></a><a href="#">进度</a><a href="#">我的</a></nav>
                    </body>
                    </html>
                    """);
            addMobileStyles(page);

            assertTrue(Math.abs(number(page, "#secondaryAction", "top") - number(page, "#progressAction", "top")) <= 1.5,
                    "secondary hero actions should share one row");
            assertTrue(number(page, "#primaryAction", "width") > number(page, "#secondaryAction", "width") * 1.8,
                    "primary hero action should span both columns");
            assertBetween(number(page, ".kefu-image-button", "width"), 39, 41);
            assertBetween(number(page, ".kefu-image-button", "height"), 39, 41);
            assertTrue(number(page, "#announcementBar", "right") <= 390.5, "announcement should stay inside viewport");
            assertTrue(number(page, "#announcementBar", "left") >= 9.5, "announcement should keep a phone gutter");
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= window.innerWidth"));
        }
    }

    @Test
    void webkit360MakesServiceFilterNonStickyAndCompact() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(360, 780));
            page.setContent("""
                    <!doctype html>
                    <html data-frontend="mobile">
                    <body class="mobile-frontend" data-page="services">
                      <main class="container section page-shell services-browser">
                        <div class="page-hero page-hero-compact"><div><div class="service-state">● 服务大厅</div><h1>COD MW4 / DMZ / 战区</h1><p class="page-hero-note">可靠手打 · 纯绿服务 · 进度可查</p></div><div class="page-hero-actions"><a class="btn primary">提前询价</a></div></div>
                        <section id="serviceConsole" class="service-browser-console">
                          <div class="service-browser-search"><span class="search-icon">⌕</span><input id="serviceSearch" type="search" placeholder="搜索游戏或服务"></div>
                          <div class="browser-row"><div class="filters game-filter-rail"><button id="filterAll" class="filter-btn active">全部游戏</button><button class="filter-btn">使命召唤 MW4</button><button class="filter-btn">DMZ</button><button class="filter-btn">战区</button></div></div>
                          <div class="browser-row"><div class="filters platform-filter-rail"><button class="filter-btn active">全部平台</button><button class="filter-btn">Steam</button><button class="filter-btn">Battle.net</button><button class="filter-btn">PlayStation</button></div></div>
                          <div class="browser-row"><div class="filters service-filterbar"><button class="filter-btn active">全部需求</button><button class="filter-btn">武器升级</button><button class="filter-btn">DMZ任务</button></div></div>
                          <div class="service-browser-summary">41 个可接服务</div>
                        </section>
                        <div class="service-browser-grid"><a class="service-card card"><div class="service-cover"></div><div class="service-card-head"><span class="tag">使命召唤 MW4</span><span class="availability">可预约</span></div><h3>武器升级</h3><div class="service-card-facts"><span><b>服务</b><i>手打 · 纯绿</i></span></div></a></div>
                      </main>
                    </body>
                    </html>
                    """);
            addMobileStyles(page);

            assertEquals("static", page.evaluate("() => getComputedStyle(document.querySelector('#serviceConsole')).position"));
            // WebKit reports the 38px CSS field as a 40px border box, matching our <=360px compact field target.
            assertBetween(number(page, "#serviceSearch", "height"), 39, 41);
            assertBetween(number(page, "#filterAll", "height"), 31, 33);
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= window.innerWidth"));
        }
    }

    private static void addMobileStyles(Page page) {
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-compact.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-screenshot-fixes.css")));
    }

    private static double number(Page page, String selector, String dimension) {
        String expression = "() => document.querySelector('" + selector + "').getBoundingClientRect()." + dimension;
        return ((Number) page.evaluate(expression)).doubleValue();
    }

    private static void assertBetween(double value, double min, double max) {
        assertTrue(value >= min && value <= max, "expected " + value + " between " + min + " and " + max);
    }
}
