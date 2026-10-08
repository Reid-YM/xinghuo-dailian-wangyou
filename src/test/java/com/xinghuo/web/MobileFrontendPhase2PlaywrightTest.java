package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class MobileFrontendPhase2PlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void chromium390UsesIndependentMobileShellAndNoBlackHeroBars() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
            renderMobileFixture(page);
            assertMobilePhase2(page, 390, "Chromium 390");
        }
    }

    @Test
    void webkit360KeepsTouchTargetsAndNoHorizontalOverflow() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(360, 780));
            renderMobileFixture(page);
            assertMobilePhase2(page, 360, "WebKit 360");
        }
    }

    private static void assertMobilePhase2(Page page, int viewportWidth, String label) {
        assertEquals("mobile", string(page, "document.documentElement.dataset.frontend"));
        assertTrue(bool(page, "document.body.classList.contains('mobile-frontend')"));
        assertEquals(5, number(page, "document.querySelectorAll('#mobilePrimaryNav > a').length").intValue());
        assertEquals("fixed", string(page, "getComputedStyle(document.querySelector('#mobilePrimaryNav')).position"));
        assertEquals("none", string(page, "getComputedStyle(document.querySelector('.site-header .nav')).display"));
        assertTrue(bool(page, "!!document.querySelector('.mobile-header-cta')"));

        Number heroWidth = number(page, "document.querySelector('#homeHero').getBoundingClientRect().width");
        assertTrue(heroWidth.doubleValue() >= viewportWidth - 1,
                () -> label + " hero is not edge-to-edge: " + heroWidth + "px");
        assertEquals("0px", string(page, "getComputedStyle(document.querySelector('#homeHero')).borderRadius"));

        assertEquals("contain", string(page, "getComputedStyle(document.querySelector('#heroBackground')).backgroundSize"),
                label + " must keep the configured foreground fit");
        assertEquals("cover", string(page, "getComputedStyle(document.querySelector('.mobile-hero-backdrop')).backgroundSize"),
                label + " must fill contain-mode bars with a cover backdrop");
        assertFalse(bool(page, "document.querySelector('.mobile-hero-backdrop').hidden"));
        assertTrue(string(page, "getComputedStyle(document.querySelector('.mobile-hero-backdrop')).backgroundImage").contains("linear-gradient"));

        Number primaryWidth = number(page, "document.querySelector('.hero-actions .btn:first-child').getBoundingClientRect().width");
        Number secondaryWidth = number(page, "document.querySelector('.hero-actions .btn:nth-child(2)').getBoundingClientRect().width");
        assertTrue(primaryWidth.doubleValue() > secondaryWidth.doubleValue() * 1.75,
                () -> label + " primary CTA must occupy the full first row");

        Number navTarget = number(page, "document.querySelector('#mobilePrimaryNav > a').getBoundingClientRect().height");
        assertTrue(navTarget.doubleValue() >= 44.0,
                () -> label + " bottom navigation touch target was only " + navTarget + "px");
        assertEquals("16px", string(page, "getComputedStyle(document.querySelector('input')).fontSize"));
        assertFalse(bool(page, "document.querySelector('details[data-responsive-details]').open"),
                label + " home optional details should start compact on mobile");

        Number scrollWidth = number(page, "document.documentElement.scrollWidth");
        Number clientWidth = number(page, "document.documentElement.clientWidth");
        assertTrue(scrollWidth.doubleValue() <= clientWidth.doubleValue() + 1,
                () -> label + " overflowed horizontally: scroll=" + scrollWidth + ", client=" + clientWidth);
    }

    private static void renderMobileFixture(Page page) {
        page.setContent("""
                <!doctype html>
                <html data-frontend="mobile">
                <body data-page="home">
                  <header class="site-header">
                    <div class="container topbar">
                      <a class="brand brand-mark"><span>星火网游代练平台</span></a>
                      <nav class="nav"><a href="/services">服务</a><a href="/records">业务记录</a></nav>
                      <button class="nav-toggle" type="button"><span></span></button>
                    </div>
                  </header>
                  <main class="container">
                    <section id="homeHero" class="hero hero-r3 image-controlled-background has-background" style="--hero-mobile-background-height:560px">
                      <div id="heroBackground" class="hero-bg-layer" style="background-image:linear-gradient(120deg,#222,#666);background-size:contain;background-position:50% 44%;background-repeat:no-repeat"></div>
                      <div class="hero-copy">
                        <div class="hero-signal"><div class="service-state">● 预约中</div><span class="hero-kicker">可靠手打 · 纯绿服务 · 进度可查</span></div>
                        <h1>使命召唤 MW4 任务代练</h1>
                        <p class="hero-subtitle">武器升级 · 迷彩解锁 · 活动任务</p>
                        <div class="hero-promise"><span><b>可靠手打</b><small>人工推进</small></span><span><b>纯绿服务</b><small>不使用外挂</small></span><span><b>进度可查</b><small>业务编号</small></span></div>
                        <div class="hero-price-row"><div class="price">¥20 起</div></div>
                        <div class="actions hero-actions"><a class="btn primary">提前询价</a><a class="btn ghost">服务大厅</a><a class="btn ghost">查业务进度</a></div>
                      </div>
                    </section>
                    <section class="section"><details data-responsive-details open><summary>补充信息</summary><div>内容</div></details><input type="text" value="微信号"></section>
                  </main>
                  <footer class="site-footer"><div class="container footer-row">星火网游</div></footer>
                </body>
                </html>
                """);
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("site-experience-v2.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-app.css")));
        page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("mobile-app.js")));
        page.waitForTimeout(50);
    }

    private static Number number(Page page, String expression) {
        return (Number) page.evaluate("() => " + expression);
    }

    private static String string(Page page, String expression) {
        return String.valueOf(page.evaluate("() => " + expression));
    }

    private static boolean bool(Page page, String expression) {
        return Boolean.TRUE.equals(page.evaluate("() => " + expression));
    }
}
