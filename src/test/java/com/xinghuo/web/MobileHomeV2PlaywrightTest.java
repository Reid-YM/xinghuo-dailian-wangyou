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
class MobileHomeV2PlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void chromium390UsesCleanImageFirstLayoutWithoutLegacyShells() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            verify(browser, 390, 844);
        }
    }

    @Test
    void webkit360UsesCleanImageFirstLayoutWithoutLegacyShells() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            verify(browser, 360, 780);
        }
    }

    @Test
    void chromium430KeepsSafeGuttersAndNoHorizontalOverflow() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            verify(browser, 430, 932);
        }
    }

    private static void verify(Browser browser, int width, int height) {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, height));
        page.setContent(markup());
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-home-v2.css")));

        assertTrue(bool(page, "document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1"));
        assertEquals(0, number(page, "document.querySelectorAll('.xh-v4,.mobile-primary-nav,.mobile-hero-backdrop').length").intValue());

        double artWidth = number(page, "document.querySelector('.mh2-artwork').getBoundingClientRect().width").doubleValue();
        double artHeight = number(page, "document.querySelector('.mh2-artwork').getBoundingClientRect().height").doubleValue();
        assertTrue(Math.abs((artWidth / artHeight) - (16.0 / 9.0)) < 0.02,
            () -> width + "px artwork is not 16:9: " + artWidth + "x" + artHeight);

        double artworkBottom = number(page, "document.querySelector('.mh2-artwork').getBoundingClientRect().bottom").doubleValue();
        double primaryTop = number(page, "document.querySelector('.mh2-hero-actions .mh2-button').getBoundingClientRect().top").doubleValue();
        double gap = primaryTop - artworkBottom;
        assertTrue(gap >= 13.0 && gap <= 15.5,
            () -> width + "px artwork-to-CTA gap should be direct and compact, got " + gap);

        assertTrue(number(page, "document.querySelector('.mh2-topbar').getBoundingClientRect().height").doubleValue() <= 74.0);
        assertTrue(number(page, "document.querySelector('.mh2-hero-actions .mh2-button').getBoundingClientRect().height").doubleValue() >= 48.0);
        assertEquals("cover", string(page, "getComputedStyle(document.querySelector('#heroBackground')).backgroundSize"));
        assertEquals("none", string(page, "getComputedStyle(document.querySelector('#heroBackground')).transform"));

        assertTrue(bool(page, "(() => { const a=[...document.querySelectorAll('.mh2-task')]; const r0=a[0].getBoundingClientRect(), r1=a[1].getBoundingClientRect(), r2=a[2].getBoundingClientRect(); return a.length===4 && Math.abs(r0.top-r1.top)<1 && r2.top>r0.top+20; })()"));
        assertEquals("16px", string(page, "getComputedStyle(document.querySelector('#home-game')).fontSize"));

        assertEquals(5, number(page, "document.querySelectorAll('.mh2-bottom-nav > a').length").intValue());
        assertTrue(number(page, "document.querySelector('.mh2-bottom-nav > a').getBoundingClientRect().height").doubleValue() >= 54.0);
        assertTrue(bool(page, "document.querySelector('.mh2-bottom-nav').getBoundingClientRect().right <= innerWidth + .5"));
        assertTrue(number(page, "document.querySelector('.mh2-support').getBoundingClientRect().width").doubleValue() <= 46.5);

        page.close();
    }

    private static String markup() {
        return """
            <!doctype html>
            <html lang="zh-CN" data-frontend="mobile" data-mobile-home="v2">
            <body data-page="home" data-frontend="mobile" data-mobile-home="v2">
              <div class="mh2-shell">
                <div id="announcementBar" class="mh2-announcement">MW4 正式版预约中</div>
                <header class="mh2-topbar">
                  <a class="mh2-brand"><img alt=""><span class="mh2-brand-copy"><strong>星火网游</strong><small>GAME SERVICE</small></span></a>
                  <nav class="mh2-top-actions"><a>客服</a><a>我的</a></nav>
                </header>
                <main>
                  <section class="mh2-hero" id="homeHero">
                    <div class="mh2-hero-intro">
                      <div class="mh2-status">MW4 正式版预约中 · 可提前询价</div>
                      <div class="mh2-kicker">CALL OF DUTY · MW4</div>
                      <h1 class="mh2-title">使命召唤 MW4 任务代练</h1>
                      <p class="mh2-subtitle">武器升级 · 迷彩解锁 · 活动任务</p>
                      <div class="mh2-meta"><div class="mh2-price"><small>参考起步</small><strong>¥20 起</strong><span>人工确认为准</span></div><div class="mh2-trust"><span>可靠手打</span><span>进度可查</span></div></div>
                    </div>
                    <div class="mh2-artwork"><div id="heroBackground" class="mh2-artwork-layer" style="background-image:linear-gradient(#777,#222);background-size:cover"></div></div>
                    <div class="mh2-hero-actions"><a class="mh2-button mh2-button--primary">提前询价</a><a class="mh2-button mh2-button--ghost">查看服务大厅</a></div>
                  </section>
                  <div class="mh2-stack">
                    <section class="mh2-section mh2-launcher"><div class="mh2-launcher-title"><strong>你想做什么？</strong><span>选择任务</span></div><div class="mh2-task-grid"><a class="mh2-task"><span class="mh2-task-icon">⚙</span><strong>武器升级</strong><small>等级 · 配件</small></a><a class="mh2-task"><span class="mh2-task-icon">◆</span><strong>迷彩任务</strong><small>多人 · 战区</small></a><a class="mh2-task"><span class="mh2-task-icon">★</span><strong>活动任务</strong><small>赛季 · 挑战</small></a><a class="mh2-task"><span class="mh2-task-icon">＋</span><strong>其他需求</strong><small>人工确认</small></a></div></section>
                    <section class="mh2-section" id="quick-actions"><div class="mh2-section-head"><div><small>QUICK QUOTE</small><h2>30 秒快速询价</h2></div></div><form class="mh2-quote-form"><div class="mh2-form-grid"><div class="mh2-field"><label>游戏</label><select id="home-game"><option>MW4</option></select></div><div class="mh2-field"><label>平台</label><select><option>Steam</option></select></div></div></form></section>
                  </div>
                </main>
              </div>
              <a class="mh2-support"></a>
              <nav class="mh2-bottom-nav"><a aria-current="page"><b>⌂</b><span>首页</span></a><a><b>▦</b><span>服务</span></a><a class="mh2-bottom-quote"><b>＋</b><span>询价</span></a><a><b>◎</b><span>进度</span></a><a><b>○</b><span>我的</span></a></nav>
            </body>
            </html>
            """;
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
