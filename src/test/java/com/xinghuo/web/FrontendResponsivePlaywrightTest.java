package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendResponsivePlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");
    private static final Path SITE_FRAGMENT = Path.of("src/main/resources/templates/fragments/site.html");

    @Test
    void wideDesktopUsesVisibleBrandToWorkspaceContrast() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1920, 1080));
            renderPublicShell(page);

            Number containerWidth = number(page, "document.querySelector('main.container').getBoundingClientRect().width");
            assertTrue(containerWidth.doubleValue() >= 1380.0,
                    () -> "24-inch desktop canvas collapsed to " + containerWidth + "px");
            assertNoHorizontalOverflow(page, "Chromium 1920 desktop");
            assertEquals("none", string(page, "getComputedStyle(document.querySelector('.site-header')).backdropFilter"));
            assertEquals("rgb(241, 242, 244)", string(page, "getComputedStyle(document.body).backgroundColor"),
                    "public workspace must be visibly light instead of the legacy full-black canvas");
            assertEquals("rgb(11, 12, 15)", string(page, "getComputedStyle(document.querySelector('.hero')).backgroundColor"),
                    "home hero must retain a dark brand stage");
            assertEquals("rgb(255, 255, 255)", string(page, "getComputedStyle(document.querySelector('.service-card')).backgroundColor"),
                    "service cards must read as white business objects on the light workspace");
            assertFourColumnServiceGrid(page);
        }
    }

    @Test
    void chromiumAndroidWechatAndNarrowWindowsStayTouchSafe() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            assertMobileContract(browser, 412, 915,
                    "Mozilla/5.0 (Linux; Android 16; Pixel) AppleWebKit/537.36 Chrome/141 Mobile Safari/537.36",
                    "Android Chrome 412");
            assertMobileContract(browser, 375, 812,
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 MicroMessenger/8.0.60 Mobile Safari/537.36",
                    "Android WeChat 375");
            assertMobileContract(browser, 320, 700,
                    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/141 Mobile Safari/537.36",
                    "Android narrow 320");
        }
    }

    @Test
    void webkitIphoneAndMacSafariStayTouchSafeAndWide() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            assertMobileContract(browser, 390, 844,
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 19_0 like Mac OS X) AppleWebKit/605.1.15 Version/19.0 Mobile/15E148 Safari/604.1",
                    "iPhone Safari 390");

            Page desktop = browser.newPage(new Browser.NewPageOptions().setViewportSize(1440, 900));
            renderPublicShell(desktop);
            assertNoHorizontalOverflow(desktop, "macOS Safari 1440");
            Number width = number(desktop, "document.querySelector('main.container').getBoundingClientRect().width");
            assertTrue(width.doubleValue() > 1300.0,
                    () -> "Safari desktop collapsed into a legacy narrow canvas: " + width + "px");
            assertEquals("none", string(desktop, "getComputedStyle(document.querySelector('.site-header')).backdropFilter"));
            assertEquals("rgb(241, 242, 244)", string(desktop, "getComputedStyle(document.body).backgroundColor"));
        }
    }

    @Test
    void experienceLayerIsAuthoritativeAndRuntimeThemeControlsRemainLast() throws Exception {
        String css = Files.readString(ASSETS.resolve("site-experience-v4.css"));
        String fragment = Files.readString(SITE_FRAGMENT);

        assertTrue(css.contains("--max:1440px"));
        assertTrue(css.contains("--xh-dark:#0b0c0f"));
        assertTrue(css.contains("--xh-canvas:#f1f2f4"));
        assertTrue(css.contains("env(safe-area-inset-bottom"));
        assertTrue(css.contains("min-height:100dvh"));
        assertTrue(css.contains("input,select,textarea{font-size:16px!important}"));
        assertTrue(css.contains("@media(forced-colors:active)"));
        assertTrue(css.contains("@media(prefers-reduced-motion:reduce)"));
        assertTrue(css.contains(".service-browser-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr))"),
                "desktop service browser must keep the explicit four-column experience contract");

        assertTrue(!fragment.contains("site-shell.css"),
                "retired legacy shell must not compete with the public experience layer");
        assertTrue(!fragment.contains("site-experience-v2.css"),
                "previous conservative experience layer must be retired from the public shell");
        assertTrue(fragment.contains("data-experience-shell=\"v4\""));
        assertTrue(fragment.contains("backdrop-filter:none!important;transition:none!important"),
                "public v4 header must quarantine legacy blur transitions at the shell boundary");
        int experience = fragment.indexOf("site-experience-v4.css?v=20261007ux04");
        int runtime = fragment.indexOf("frontend-controls-runtime.js");
        assertTrue(experience >= 0, "v4 experience stylesheet must load globally");
        assertTrue(runtime > experience, "runtime frontend controls must remain able to override configured variables");
    }

    private static void assertFourColumnServiceGrid(Page page) {
        Number cardCount = number(page,
                "document.querySelectorAll('.service-browser-grid > .service-card').length");
        assertEquals(4, cardCount.intValue(), "desktop regression fixture must contain four service cards");

        Number cardsOnFirstRow = number(page, """
                (() => {
                  const cards = Array.from(document.querySelectorAll('.service-browser-grid > .service-card'));
                  const top = cards[0].getBoundingClientRect().top;
                  return cards.filter(card => Math.abs(card.getBoundingClientRect().top - top) < 1).length;
                })()
                """);
        assertEquals(4, cardsOnFirstRow.intValue(),
                "1920 desktop must render all four service cards on one row");

        Number gridWidth = number(page,
                "document.querySelector('.service-browser-grid').getBoundingClientRect().width");
        Number firstCardWidth = number(page,
                "document.querySelector('.service-browser-grid > .service-card').getBoundingClientRect().width");
        assertTrue(firstCardWidth.doubleValue() >= gridWidth.doubleValue() / 4.5,
                () -> "service cards collapsed below a real four-column width: grid=" + gridWidth
                        + "px, firstCard=" + firstCardWidth + "px");

        Number trailingGap = number(page, """
                (() => {
                  const grid = document.querySelector('.service-browser-grid').getBoundingClientRect();
                  const cards = document.querySelectorAll('.service-browser-grid > .service-card');
                  const last = cards[cards.length - 1].getBoundingClientRect();
                  return grid.right - last.right;
                })()
                """);
        assertTrue(Math.abs(trailingGap.doubleValue()) <= 2.0,
                () -> "four service cards did not occupy the full desktop grid; trailing gap=" + trailingGap + "px");
    }

    private static void assertMobileContract(Browser browser, int width, int height, String userAgent, String label) {
        Page page = browser.newPage(new Browser.NewPageOptions()
                .setViewportSize(width, height)
                .setUserAgent(userAgent));
        renderPublicShell(page);

        assertNoHorizontalOverflow(page, label);
        assertEquals("16px", string(page, "getComputedStyle(document.querySelector('input')).fontSize"),
                label + " must not trigger iOS form zoom");
        Number buttonHeight = number(page, "document.querySelector('.mobile-dock a').getBoundingClientRect().height");
        assertTrue(buttonHeight.doubleValue() >= 44.0,
                () -> label + " mobile dock touch target was only " + buttonHeight + "px");
        Number primaryHeight = number(page, "document.querySelector('.hero-actions .btn').getBoundingClientRect().height");
        assertTrue(primaryHeight.doubleValue() >= 44.0,
                () -> label + " primary CTA touch target was only " + primaryHeight + "px");
        assertEquals("grid", string(page, "getComputedStyle(document.querySelector('.mobile-dock')).display"),
                label + " mobile dock should be visible");
        assertEquals("none", string(page, "getComputedStyle(document.querySelector('.site-header')).backdropFilter"),
                label + " header must not rely on glass blur");
        assertEquals("rgb(241, 242, 244)", string(page, "getComputedStyle(document.body).backgroundColor"),
                label + " must keep the light business workspace");
        page.close();
    }

    private static void assertNoHorizontalOverflow(Page page, String label) {
        Number scrollWidth = number(page, "document.documentElement.scrollWidth");
        Number clientWidth = number(page, "document.documentElement.clientWidth");
        assertTrue(scrollWidth.doubleValue() <= clientWidth.doubleValue() + 1,
                () -> label + " overflowed horizontally: scrollWidth=" + scrollWidth + ", clientWidth=" + clientWidth);
    }

    private static Number number(Page page, String expression) {
        return (Number) page.evaluate("() => " + expression);
    }

    private static String string(Page page, String expression) {
        return String.valueOf(page.evaluate("() => " + expression));
    }

    private static void renderPublicShell(Page page) {
        page.setContent("""
                <body data-page="home">
                  <header class="site-header" data-experience-shell="v4" style="-webkit-backdrop-filter:none!important;backdrop-filter:none!important;transition:none!important">
                    <div class="container topbar">
                      <a class="brand brand-mark"><span class="brand-dot"></span><span>星火网游代练平台</span></a>
                      <nav class="nav"><a>服务</a><a>攻略</a><a>工具站</a><a class="btn primary desktop-keep">提前询价</a></nav>
                      <button class="nav-toggle"><span></span><span></span><span></span></button>
                    </div>
                  </header>
                  <main class="container">
                    <section class="hero hero-r3">
                      <div class="hero-copy">
                        <div class="hero-signal"><span class="hero-kicker">可靠手打 · 纯绿服务 · 进度可查</span></div>
                        <h1>使命召唤 MW4 任务代练</h1>
                        <div class="hero-promise"><span><b>可靠手打</b><small>人工推进</small></span><span><b>纯绿服务</b><small>不使用外挂</small></span><span><b>进度可查</b><small>统一业务编号</small></span></div>
                        <div class="hero-actions"><a class="btn primary">提前询价</a><a class="btn ghost">服务大厅</a><a class="btn ghost hero-progress-link">查业务进度</a></div>
                      </div>
                      <aside class="hero-panel hero-media-panel"><div class="hero-visual"></div></aside>
                    </section>
                    <section class="service-browser-grid grid4">
                      <article class="service-card card">服务 A</article><article class="service-card card">服务 B</article>
                      <article class="service-card card">服务 C</article><article class="service-card card">服务 D</article>
                    </section>
                    <form><label>联系方式<input type="text" value="微信号"></label><select><option>Steam</option></select></form>
                  </main>
                  <nav class="mobile-dock"><a>首页</a><a>业务记录</a><a class="primary">提前询价</a></nav>
                  <footer class="site-footer"><div class="container footer-row">星火网游</div></footer>
                </body>
                """);
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("site-experience-v4.css")));
    }
}
