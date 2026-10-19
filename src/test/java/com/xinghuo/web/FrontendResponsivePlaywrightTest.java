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
    void wideDesktopUsesFullOperationalCanvasAndNextHomeHierarchy() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1920, 1080));
            renderPublicShell(page);

            Number containerWidth = number(page, "document.querySelector('main.container').getBoundingClientRect().width");
            assertTrue(containerWidth.doubleValue() >= 1600.0,
                    () -> "24-inch desktop canvas collapsed to " + containerWidth + "px");
            assertNoHorizontalOverflow(page, "Chromium 1920 desktop");
            assertEquals("none", string(page, "(getComputedStyle(document.querySelector('.site-header')).backdropFilter ?? getComputedStyle(document.querySelector('.site-header')).webkitBackdropFilter ?? 'none')"));
            assertTrue(number(page,
                    "document.querySelector('.hero-copy').getBoundingClientRect().width").doubleValue()
                    > number(page, "document.querySelector('.hero-media-panel').getBoundingClientRect().width").doubleValue(),
                    "next home hero must reserve more operational width for conversion copy than decorative media");
            assertEquals(2, number(page, "getComputedStyle(document.querySelector('.quick-grid-r3')).gridTemplateColumns.split(' ').length").intValue());
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
            assertTrue(width.doubleValue() > 1360.0,
                    () -> "Safari desktop collapsed into a legacy narrow canvas: " + width + "px");
            assertEquals("none", string(desktop, "(getComputedStyle(document.querySelector('.site-header')).backdropFilter ?? getComputedStyle(document.querySelector('.site-header')).webkitBackdropFilter ?? 'none')"));
        }
    }

    @Test
    void nextFoundationLoadsAfterCompatibilityAndBeforeRuntimeControls() throws Exception {
        String compatibility = Files.readString(ASSETS.resolve("site-experience-v2.css"));
        String next = Files.readString(ASSETS.resolve("frontend-next-foundation.css"));
        String fragment = Files.readString(SITE_FRAGMENT);

        assertTrue(compatibility.contains("--max:1440px"));
        assertTrue(next.contains("--xh-next-max:1720px"));
        assertTrue(next.contains("@media (min-width:1600px)"));
        assertTrue(next.contains("input,select,textarea{font-size:16px!important}"));
        assertTrue(next.contains("@media (forced-colors:active)"));
        assertTrue(next.contains("@media (prefers-reduced-motion:reduce)"));
        assertTrue(next.contains("var(--accent-rgb"));

        int compatibilityIndex = fragment.indexOf("site-experience-v2.css?v=20261012ux04");
        int nextIndex = fragment.indexOf("frontend-next-foundation.css?v=20261014nx01");
        int runtimeIndex = fragment.indexOf("frontend-controls-runtime.js");
        assertTrue(compatibilityIndex >= 0 && nextIndex > compatibilityIndex);
        assertTrue(runtimeIndex > nextIndex);
        assertTrue(fragment.contains("data-experience-shell=\"v4\""));
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
        assertEquals("none", string(page, "getComputedStyle(document.querySelector('.hero-media-panel')).display"),
                label + " decorative hero media must not crowd the mobile conversion flow");
        assertEquals("none", string(page, "(getComputedStyle(document.querySelector('.site-header')).backdropFilter ?? getComputedStyle(document.querySelector('.site-header')).webkitBackdropFilter ?? 'none')"),
                label + " header must not rely on glass blur");
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
                  <header class="site-header" data-experience-shell="v4">
                    <div class="container topbar">
                      <a class="brand brand-mark"><span class="brand-dot"></span><span>星火网游代练平台</span></a>
                      <nav class="nav"><a>服务</a><a>攻略</a><a>工具站</a><a class="btn primary desktop-keep">提前询价</a></nav>
                      <button class="nav-toggle"><span></span><span></span><span></span></button>
                    </div>
                  </header>
                  <main class="container">
                    <section class="hero hero-r3">
                      <div class="hero-copy">
                        <div class="hero-signal"><span class="service-state">MW4 服务中</span><span class="hero-kicker">可靠手打 · 纯绿服务 · 进度可查</span></div>
                        <h1>使命召唤 MW4 任务代练</h1>
                        <p class="hero-subtitle">武器升级 · 迷彩解锁 · 活动任务 · 排位冲分</p>
                        <div class="hero-promise"><span><b>可靠手打</b><small>人工推进</small></span><span><b>纯绿服务</b><small>不使用外挂</small></span><span><b>进度可查</b><small>统一业务编号</small></span></div>
                        <div class="hero-actions"><a class="btn primary">提前询价</a><a class="btn ghost">服务大厅</a><a class="btn ghost hero-progress-link">查业务进度</a></div>
                      </div>
                      <aside class="hero-panel hero-media-panel"><div class="hero-visual"></div><div class="hero-media-copy">当前推荐</div></aside>
                    </section>
                    <section class="quick-grid quick-grid-r3">
                      <article class="card quick-card quote-card-home"><div class="quote-card-head"><h2>选择需求，获取报价</h2></div><div class="linked-selects four-step"><label class="field">游戏<select><option>MW4</option></select></label><label class="field">平台<select><option>Steam</option></select></label><label class="field">类型<select><option>武器</option></select></label><label class="field">服务<select><option>升级</option></select></label></div><div class="hero-actions"><button class="btn primary">提交</button></div></article>
                      <article class="card quick-card"><h2>业务进度</h2></article>
                      <article class="card quick-contact-card"><div>联系</div><div class="quick-contact-list"><div class="quick-contact-item">微信</div></div></article>
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
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("site-experience-v2.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("frontend-next-foundation.css")));
    }
}
