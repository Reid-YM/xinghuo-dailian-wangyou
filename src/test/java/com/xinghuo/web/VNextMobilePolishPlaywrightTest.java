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
class VNextMobilePolishPlaywrightTest {
    private static final Path MOBILE_V7 = Path.of("src/main/resources/static/assets/vnext-mobile-focus-v7.css");
    private static final Path MOBILE_V8 = Path.of("src/main/resources/static/assets/vnext-mobile-polish-v8.css");

    @Test
    void chromium375KeepsPrimaryChoicesTogetherAndShortensHeader() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = mobilePage(browser, 375, 812, "home");
            try {
                assertNoHorizontalOverflow(page, "Chromium 375");
                assertEquals(2, number(page,
                        "getComputedStyle(document.querySelector('.v-hero-actions')).gridTemplateColumns.split(' ').length").intValue(),
                        "375px should keep the two hero decisions visible together");
                assertTrue(number(page, "document.querySelector('.v-header-inner').getBoundingClientRect().height").doubleValue() <= 55.0,
                        "mobile header should be compact after v8");
                assertEquals("none", string(page, "getComputedStyle(document.querySelector('.v-brand-copy small')).display"),
                        "secondary brand line should not consume phone header height");
                assertTrue(number(page, "document.querySelector('.v-feature-card').getBoundingClientRect().height").doubleValue() <= 150.0,
                        "promise cards should be denser on the first phone screen");
            } finally {
                page.close();
            }
        }
    }

    @Test
    void webkit390QuoteFlowOwnsBottomThumbZone() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = mobilePage(browser, 390, 844, "quote");
            try {
                assertNoHorizontalOverflow(page, "WebKit 390 quote");
                assertEquals("none", string(page, "getComputedStyle(document.querySelector('.v-mobile-dock')).display"),
                        "global dock must move out of the quote wizard thumb zone");
                assertEquals("0px", string(page, "getComputedStyle(document.querySelector('.v-form-actions')).bottom"),
                        "quote actions should anchor to the viewport safe-area edge");
                assertTrue(number(page, "document.querySelector('.v-form-actions .v-primary').getBoundingClientRect().height").doubleValue() >= 50.0,
                        "quote primary action must remain a large touch target");
                assertTrue(number(page, "document.querySelector('.v-choice').getBoundingClientRect().height").doubleValue() >= 68.0,
                        "service choices must remain easy to tap");
            } finally {
                page.close();
            }
        }
    }

    @Test
    void chromium430ServiceToolbarRemainsReachableAndDesktopScopeStaysClean() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page mobile = mobilePage(browser, 430, 932, "services");
            try {
                assertNoHorizontalOverflow(mobile, "Chromium 430 services");
                assertTrue(number(mobile, "parseFloat(getComputedStyle(document.querySelector('.v-service-toolbar')).top)").doubleValue() >= 54.0,
                        "service search must remain sticky under the compact header");
                assertEquals(2, number(mobile,
                        "getComputedStyle(document.querySelector('.v-service-toolbar')).gridTemplateColumns.split(' ').length").intValue(),
                        "service toolbar should keep search and count in one compact row");
            } finally {
                mobile.close();
            }

            Page desktop = mobilePage(browser, 1024, 900, "home");
            try {
                assertEquals("block", string(desktop, "getComputedStyle(document.querySelector('.v-brand-copy small')).display"),
                        "v8 phone polish must not hide desktop brand metadata");
                assertEquals("grid", string(desktop, "getComputedStyle(document.querySelector('.v-mobile-dock')).display"),
                        "fixture desktop dock baseline should remain untouched by v8");
                assertEquals("68px", string(desktop, "getComputedStyle(document.querySelector('.v-form-actions')).bottom"),
                        "quote thumb-zone override must remain scoped below 768px");
            } finally {
                desktop.close();
            }
        }
    }

    private static Page mobilePage(Browser browser, int width, int height, String dataPage) throws Exception {
        Page page = browser.newPage();
        page.setViewportSize(width, height);
        page.setContent(fixture(dataPage));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(MOBILE_V7));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(MOBILE_V8));
        return page;
    }

    private static void assertNoHorizontalOverflow(Page page, String label) {
        Number scrollWidth = number(page, "document.documentElement.scrollWidth");
        Number clientWidth = number(page, "document.documentElement.clientWidth");
        assertTrue(scrollWidth.doubleValue() <= clientWidth.doubleValue() + 1,
                () -> label + " horizontal overflow: " + scrollWidth + " > " + clientWidth);
    }

    private static Number number(Page page, String expression) {
        return (Number) page.evaluate("() => " + expression);
    }

    private static String string(Page page, String expression) {
        return String.valueOf(page.evaluate("() => " + expression));
    }

    private static String fixture(String dataPage) {
        return """
                <!doctype html>
                <html><head><meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
                <style>
                  *{box-sizing:border-box}body{margin:0;background:#0b0c0f;color:#fff;font-family:Arial,sans-serif;overflow-x:hidden}
                  .v-app{min-height:100vh}.v-shell{width:100%;padding:0 16px}.v-header{min-height:66px}.v-header-inner{height:66px;display:flex;align-items:center;justify-content:space-between}
                  .v-brand{display:flex}.v-brand-mark{width:38px;height:38px}.v-brand-copy{display:block}.v-brand-copy small{display:block}.v-menu-button{width:44px;height:44px}
                  .v-hero{padding:36px 0}.v-hero-grid{display:grid}.v-hero-actions,.v-trust-row,.v-feature-grid{display:grid}.v-hero-actions{grid-template-columns:1fr}.v-button{min-height:44px}
                  .v-live-panel{min-height:330px}.v-service-preview-grid{display:grid}.v-service-card{min-height:188px}.v-service-toolbar{position:static;display:block}.v-search-box{min-height:44px}
                  .v-mobile-dock{display:grid;grid-template-columns:repeat(5,1fr);position:fixed;left:0;right:0;bottom:0}.v-mobile-dock button{min-height:56px;background:rgb(20,20,20);color:#fff;border:0}
                  .v-feature-grid{grid-template-columns:repeat(4,minmax(0,1fr))}.v-feature-card{min-height:180px}.v-card{min-width:0}.v-form-card{padding:17px}.v-form-actions{position:sticky;bottom:68px}.v-choice{min-height:76px}.v-page{padding-bottom:96px}
                </style></head>
                <body>
                  <div class="v-app" data-page="__DATA_PAGE__">
                    <header class="v-header"><div class="v-shell v-header-inner"><div class="v-brand"><span class="v-brand-mark">XH</span><span class="v-brand-copy"><strong>星火网游代练平台</strong><small>可靠手打 · 进度可查</small></span></div><button class="v-menu-button">M</button></div></header>
                    <section class="v-hero"><div class="v-shell v-hero-grid"><div class="v-hero-copy"><div class="v-kicker">CODMW4</div><h1>使命召唤 MW4 任务服务</h1><p class="v-lead">手机端专项视觉验证</p><div class="v-hero-actions"><button class="v-button v-primary">立即询价</button><button class="v-button">查看全部服务</button></div><div class="v-trust-row"><span>可靠手打</span><span>纯绿服务</span><span>进度可查</span><span>一对一咨询</span></div></div><div class="v-live-panel"><div class="v-live-panel-inner"><strong>服务在线</strong><p>业务编号查询进度</p><div class="v-flow-list"><span><b>01</b>目标</span><span><b>02</b>报价</span><span><b>03</b>进度</span></div></div><div class="v-status-strip">在线</div></div></div></section>
                    <section class="v-shell"><div class="v-feature-grid"><div class="v-card v-feature-card">A</div><div class="v-card v-feature-card">B</div><div class="v-card v-feature-card">C</div><div class="v-card v-feature-card">D</div></div><div class="v-service-preview-grid"><div class="v-service-card">服务A</div><div class="v-service-card">服务B</div></div><div class="v-service-toolbar"><div class="v-search-box">搜索</div><span>4 项服务</span></div></section>
                    <section class="v-page v-shell"><div class="v-form-card"><div class="v-choice-grid"><button class="v-choice">服务选择</button></div><div class="v-form-actions"><button class="v-button">上一步</button><button class="v-button v-primary">下一步</button></div></div></section>
                    <nav class="v-mobile-dock"><button>首页</button><button>服务</button><button>询价</button><button>进度</button><button>我的</button></nav>
                  </div>
                </body></html>
                """.replace("__DATA_PAGE__", dataPage);
    }
}
