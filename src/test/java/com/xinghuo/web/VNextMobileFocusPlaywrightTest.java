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
class VNextMobileFocusPlaywrightTest {
    private static final Path MOBILE_CSS = Path.of("src/main/resources/static/assets/vnext-mobile-focus-v7.css");

    @Test
    void chromiumMobileFocusIsVisibleAt375And430() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            verifyMobile(browser, 375, 812, "Chromium 375");
            verifyMobile(browser, 430, 932, "Chromium 430");
        }
    }

    @Test
    void webkitMobileFocusIsVisibleAt390AndDesktopScopeStaysOff() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            verifyMobile(browser, 390, 844, "WebKit 390");
            verifyDesktopUnaffected(browser);
        }
    }

    private static void verifyMobile(Browser browser, int width, int height, String label) throws Exception {
        Page page = browser.newPage();
        try {
            page.setViewportSize(width, height);
            page.setContent(fixture());
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(MOBILE_CSS));

            assertEquals(width, number(page, "window.innerWidth").intValue(), label + " viewport width mismatch");
            assertNoHorizontalOverflow(page, label);

            assertEquals("grid", string(page, "getComputedStyle(document.querySelector('.v-trust-row')).display"));
            Number trustColumns = number(page, "getComputedStyle(document.querySelector('.v-trust-row')).gridTemplateColumns.split(' ').length");
            assertEquals(2, trustColumns.intValue(), label + " trust proof must be a two-column mobile grid");

            assertEquals("flex", string(page, "getComputedStyle(document.querySelector('.v-service-preview-grid')).display"));
            assertEquals("x mandatory", string(page, "getComputedStyle(document.querySelector('.v-service-preview-grid')).scrollSnapType"));

            String quoteBackground = string(page, "getComputedStyle(document.querySelector('.v-mobile-dock button:nth-child(3)')).backgroundColor");
            assertEquals("rgb(223, 47, 49)", quoteBackground, label + " quote action must be visually primary");
            Number quoteLift = number(page, "parseFloat(getComputedStyle(document.querySelector('.v-mobile-dock button:nth-child(3)')).marginTop)");
            assertTrue(quoteLift.doubleValue() <= -13.0, label + " quote action must be raised above the dock");

            Number toolbarTop = number(page, "parseFloat(getComputedStyle(document.querySelector('.v-service-toolbar')).top)");
            assertTrue(toolbarTop.doubleValue() >= 58.0, label + " service search should remain sticky below the mobile header");

            assertEquals("static", string(page, "getComputedStyle(document.querySelector('.auth-card')).position"),
                    label + " auth card should not remain desktop-sticky on mobile");
            Number authWidth = number(page, "document.querySelector('main.container').getBoundingClientRect().width");
            assertTrue(authWidth.doubleValue() <= width - 20.0, label + " legacy auth shell should keep mobile gutters");

            Number primaryHeight = number(page, "document.querySelector('.v-hero-actions .v-primary').getBoundingClientRect().height");
            assertTrue(primaryHeight.doubleValue() >= 48.0, label + " primary hero CTA is below touch target size");
        } finally {
            page.close();
        }
    }

    private static void verifyDesktopUnaffected(Browser browser) throws Exception {
        Page page = browser.newPage();
        try {
            page.setViewportSize(1024, 900);
            page.setContent(fixture());
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(MOBILE_CSS));

            Number desktopColumns = number(page,
                    "getComputedStyle(document.querySelector('.v-feature-grid')).gridTemplateColumns.split(' ').length");
            assertEquals(4, desktopColumns.intValue(), "mobile focus layer must not rewrite desktop feature layout");
            assertEquals("rgb(20, 20, 20)",
                    string(page, "getComputedStyle(document.querySelector('.v-mobile-dock button:nth-child(3)')).backgroundColor"),
                    "mobile dock emphasis must stay scoped below 768px");
        } finally {
            page.close();
        }
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

    private static String fixture() {
        return """
                <!doctype html>
                <html><head><meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
                <style>
                  *{box-sizing:border-box}body{margin:0;background:#0b0c0f;color:#fff;font-family:Arial,sans-serif;overflow-x:hidden}
                  .v-app{min-height:100vh}.v-shell{width:100%;padding:0 16px}.v-header{height:66px}.v-header-inner{display:flex;align-items:center;justify-content:space-between}
                  .v-brand{display:flex}.v-brand-mark{width:38px;height:38px}.v-brand-copy{display:block}.v-menu-button{width:44px;height:44px}
                  .v-hero-grid{display:grid}.v-hero-actions,.v-trust-row,.v-feature-grid{display:grid}.v-hero-actions{grid-template-columns:1fr}.v-button{min-height:44px}
                  .v-live-panel{min-height:330px}.v-service-preview-grid{display:grid}.v-service-card{min-height:188px}.v-service-toolbar{position:static}
                  .v-mobile-dock{display:grid;grid-template-columns:repeat(5,1fr);position:fixed;left:0;right:0;bottom:0}.v-mobile-dock button{min-height:56px;background:rgb(20,20,20);color:#fff;border:0}
                  .v-feature-grid{grid-template-columns:repeat(4,minmax(0,1fr))}.v-card{min-width:0}.container{width:100%}.auth-shell{display:grid}.auth-card{position:sticky;top:92px}
                </style></head>
                <body data-page="account-login">
                  <div class="v-app">
                    <header class="v-header"><div class="v-shell v-header-inner"><div class="v-brand"><span class="v-brand-mark">XH</span><span class="v-brand-copy"><strong>星火网游代练平台</strong></span></div><button class="v-menu-button">M</button></div></header>
                    <section class="v-hero"><div class="v-shell v-hero-grid"><div class="v-hero-copy"><div class="v-kicker">CODMW4</div><h1>使命召唤 MW4 任务服务</h1><p class="v-lead">手机端专项视觉验证</p><div class="v-hero-actions"><button class="v-button v-primary">立即询价</button><button class="v-button">全部服务</button></div><div class="v-trust-row"><span>可靠手打</span><span>纯绿服务</span><span>进度可查</span><span>一对一咨询</span></div></div><div class="v-live-panel"><div class="v-live-panel-inner"><strong>服务在线</strong><p>业务编号查询进度</p><div class="v-flow-list"><span><b>01</b>目标</span><span><b>02</b>报价</span><span><b>03</b>进度</span></div></div></div></div></section>
                    <section class="v-shell"><div class="v-feature-grid"><div class="v-card">A</div><div class="v-card">B</div><div class="v-card">C</div><div class="v-card">D</div></div><div class="v-service-preview-grid"><div class="v-service-card">服务A</div><div class="v-service-card">服务B</div></div><div class="v-service-toolbar"><div>搜索</div></div></section>
                    <nav class="v-mobile-dock"><button>首页</button><button>服务</button><button>询价</button><button>进度</button><button>我的</button></nav>
                  </div>
                  <main class="container auth-shell"><section>价值说明</section><section class="auth-card">登录</section></main>
                </body></html>
                """;
    }
}
