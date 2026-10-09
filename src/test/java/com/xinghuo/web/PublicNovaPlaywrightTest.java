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
class PublicNovaPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void chromiumDesktopUsesTwoColumnHeroAndReadableControls() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = render(browser, 1280, 900);
            assertTrue(number(page, "document.querySelector('.nv-home-hero').getBoundingClientRect().width").doubleValue() > 900);
            assertTrue(bool(page, "getComputedStyle(document.querySelector('.nv-home-hero')).gridTemplateColumns.split(' ').length >= 2"));
            assertTrue(number(page, "document.querySelector('.nv-home-copy h1').getBoundingClientRect().height").doubleValue() > 80);
            assertTrue(number(page, "document.querySelector('.nv-btn--primary').getBoundingClientRect().height").doubleValue() >= 44);
            assertTrue(bool(page, "document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1"));
            page.close();
        }
    }

    @Test
    void chromium390StacksCleanlyWithFiveItemDock() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            verifyMobile(browser, 390, 844);
        }
    }

    @Test
    void webkit360HasSafeGuttersAndStableHeroMedia() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            verifyMobile(browser, 360, 780);
        }
    }

    @Test
    void chromium430KeepsTwoByTwoTaskGridAndTouchTargets() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            verifyMobile(browser, 430, 932);
        }
    }

    private static void verifyMobile(Browser browser, int width, int height) {
        Page page = render(browser, width, height);
        assertTrue(bool(page, "document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1"));
        assertEquals("none", string(page, "getComputedStyle(document.querySelector('.nv-nav')).display"));
        assertTrue(number(page, "document.querySelector('.nv-home-copy').getBoundingClientRect().width").doubleValue() <= width - 20);
        assertTrue(number(page, "document.querySelector('.nv-btn--primary').getBoundingClientRect().height").doubleValue() >= 44);
        assertEquals("16px", string(page, "getComputedStyle(document.querySelector('#home-game')).fontSize"));
        assertEquals(5, number(page, "document.querySelectorAll('.nv-mobile-dock > a').length").intValue());
        assertTrue(number(page, "document.querySelector('.nv-mobile-dock > a').getBoundingClientRect().height").doubleValue() >= 44);
        assertTrue(bool(page, "(() => { const a=[...document.querySelectorAll('.nv-task')]; const r0=a[0].getBoundingClientRect(), r1=a[1].getBoundingClientRect(), r2=a[2].getBoundingClientRect(); return a.length===4 && Math.abs(r0.top-r1.top)<1 && r2.top>r0.top+20; })()"));
        assertTrue(number(page, "document.querySelector('.nv-home-media').getBoundingClientRect().height").doubleValue() >= 180);
        page.close();
    }

    private static Page render(Browser browser, int width, int height) {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, height));
        page.setContent(markup());
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("nova-ui.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("nova-pages.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("nova-responsive.css")));
        return page;
    }

    private static String markup() {
        return """
          <!doctype html><html lang="zh-CN" data-public-ui="nova"><body>
          <header class="nv-header"><div class="nv-topbar"><a class="nv-brand"><span><strong>星火网游</strong><small>GAME SERVICE</small></span></a><nav class="nv-nav"><a>服务大厅</a><a>工具</a><a>攻略</a><a>业务记录</a></nav><button class="nv-menu-button"><span></span><span></span><span></span></button></div></header>
          <main class="nv-home">
            <section class="nv-home-hero"><div class="nv-home-copy"><div class="nv-status">预约中</div><span class="nv-eyebrow">CALL OF DUTY · MW4</span><h1>任务服务，先把目标说清楚。</h1><p>选择游戏与目标，获取业务编号。</p><div class="nv-home-actions"><a class="nv-btn nv-btn--primary">提前询价</a><a class="nv-btn nv-btn--ghost">服务大厅</a></div><div class="nv-home-proof"><span><b>人工确认</b><small>价格排期说清</small></span><span><b>业务编号</b><small>统一追踪</small></span></div></div><aside class="nv-home-media"><div class="nv-home-art" style="background-image:linear-gradient(130deg,#302016,#0c0c0e)"></div></aside></section>
            <div class="nv-home-grid"><section class="nv-panel"><div class="nv-task-grid"><a class="nv-task"><b>武器升级</b><span>等级配件</span></a><a class="nv-task"><b>迷彩任务</b><span>多人战区</span></a><a class="nv-task"><b>活动任务</b><span>赛季挑战</span></a><a class="nv-task"><b>自定义需求</b><span>人工确认</span></a></div></section><section class="nv-panel"><form><div class="field"><label>游戏</label><select id="home-game"><option>MW4</option></select></div></form></section></div>
          </main>
          <nav class="nv-mobile-dock"><a>首页</a><a>服务</a><a class="nv-dock-primary">询价</a><a>进度</a><a>我的</a></nav>
          </body></html>
          """;
    }

    private static Number number(Page page, String expression) { return (Number) page.evaluate("() => " + expression); }
    private static String string(Page page, String expression) { return String.valueOf(page.evaluate("() => " + expression)); }
    private static boolean bool(Page page, String expression) { return Boolean.TRUE.equals(page.evaluate("() => " + expression)); }
}
