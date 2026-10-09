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
class MobileHomeRebuildPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void chromium390RendersCompactRebuiltHomepageWithoutOverflow() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
            page.setContent(markup());
            addStyles(page);
            assertLayout(page, 390);
        }
    }

    @Test
    void webkit360RendersCompactRebuiltHomepageWithoutOverflow() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(360, 780));
            page.setContent(markup());
            addStyles(page);
            assertLayout(page, 360);
        }
    }

    private static void assertLayout(Page page, int viewportWidth) {
        assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth"));
        assertTrue((Boolean) page.evaluate("() => Math.abs(document.querySelector('#homeHero').getBoundingClientRect().width - innerWidth) < 1"));
        assertTrue((Boolean) page.evaluate("() => { const h=document.querySelector('#homeHero').getBoundingClientRect().height; return h >= 480 && h <= 560; }"));
        assertEquals("none", css(page, ".mobile-hero-backdrop", "display"));
        assertEquals("none", backdrop(page, ".xh-mh-hero-content"));

        assertTrue((Boolean) page.evaluate("() => { const a=[...document.querySelectorAll('.xh-mh-hero-actions .btn')]; const r1=a[0].getBoundingClientRect(),r2=a[1].getBoundingClientRect(),r3=a[2].getBoundingClientRect(); return r1.width > innerWidth*.8 && Math.abs(r2.top-r3.top)<1 && r2.width < innerWidth*.55 && r3.width < innerWidth*.55; }"));
        assertTrue((Boolean) page.evaluate("() => { const a=[...document.querySelectorAll('.xh-mh-shortcut')]; const r0=a[0].getBoundingClientRect(),r1=a[1].getBoundingClientRect(),r2=a[2].getBoundingClientRect(); return Math.abs(r0.top-r1.top)<1 && r2.top>r0.top+20; }"));
        assertTrue((Boolean) page.evaluate("() => { const s=document.querySelector('#home-game'); const h=s.getBoundingClientRect().height; return h >= 38 && h <= 42; }"));
        assertEquals("2", (String) page.evaluate("() => getComputedStyle(document.querySelector('.xh-mh-form-grid')).gridTemplateColumns.split(' ').length.toString()"));
        assertEquals("0px", css(page, ".xh-mh-block", "borderRadius"));
        assertTrue((Boolean) page.evaluate("() => [...document.querySelectorAll('.xh-mh-shortcut,.xh-mh-block,.xh-mh-quote-form')].every(el => el.getBoundingClientRect().right <= innerWidth + .5)"));
        assertTrue(viewportWidth == 360 || viewportWidth == 390);
    }

    private static String markup() {
        return """
            <!doctype html><html data-frontend="mobile"><body class="mobile-frontend" data-page="home">
            <main class="xh-mh" id="mobileHomeV3">
              <section class="xh-mh-hero hero hero-r3" id="homeHero">
                <div class="mobile-hero-backdrop"></div><div id="heroBackground" class="hero-bg-layer"></div><div class="xh-mh-hero-shade"></div>
                <div class="xh-mh-hero-content hero-copy"><div class="xh-mh-status">● 可提前询价</div><div class="xh-mh-kicker">CALL OF DUTY · MW4</div><h1 id="mobileHeroTitle">使命召唤 MW4 任务代练</h1><p class="xh-mh-subtitle">武器升级 · 迷彩解锁 · 活动任务</p><div class="xh-mh-trust"><span>可靠手打</span><span>纯绿服务</span><span>进度可查</span></div><div class="xh-mh-price"><small>参考起步</small><strong>¥20 起</strong><span>人工确认</span></div><div class="actions hero-actions xh-mh-hero-actions"><a class="btn primary xh-mh-primary">提前询价</a><a class="btn ghost">服务大厅</a><a class="btn ghost hero-progress-link">查询进度</a></div></div>
              </section>
              <section class="xh-mh-shortcuts"><a class="xh-mh-shortcut"><span class="xh-mh-shortcut-no">01</span><strong>武器升级</strong><small>等级 · 配件</small><b>→</b></a><a class="xh-mh-shortcut"><span class="xh-mh-shortcut-no">02</span><strong>迷彩任务</strong><small>多人 · 战区</small><b>→</b></a><a class="xh-mh-shortcut"><span class="xh-mh-shortcut-no">03</span><strong>活动任务</strong><small>赛季 · 挑战</small><b>→</b></a><a class="xh-mh-shortcut"><span class="xh-mh-shortcut-no">04</span><strong>其他需求</strong><small>人工确认</small><b>→</b></a></section>
              <section class="xh-mh-block xh-mh-quote" id="quick-actions"><div class="xh-mh-section-head"><div><span>30 秒询价</span><h2>选需求，拿业务编号</h2></div></div><form class="xh-mh-quote-form"><div class="xh-mh-form-grid linked-selects four-step"><div class="field"><label>游戏 *</label><select id="home-game"><option>请选择</option></select></div><div class="field"><label>平台</label><select><option>不确定</option></select></div><div class="field"><label>需求类型 *</label><select><option>武器升级</option></select></div><div class="field"><label>具体服务 *</label><select><option>等级提升</option></select></div></div></form></section>
              <section class="xh-mh-block"><div class="xh-mh-section-head"><div><span>服务保障</span><h2>能核对，不靠口号</h2></div></div></section>
            </main></body></html>
            """;
    }

    private static void addStyles(Page page) {
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-compact.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-screenshot-fixes.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-flat-overflow.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-runtime-fixes.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-home-v3.css")));
    }

    private static String css(Page page, String selector, String property) {
        return (String) page.evaluate("([selector,property]) => getComputedStyle(document.querySelector(selector))[property]", new String[]{selector, property});
    }

    private static String backdrop(Page page, String selector) {
        return (String) page.evaluate("selector => { const s=getComputedStyle(document.querySelector(selector)); return s.backdropFilter || s.webkitBackdropFilter || 'none'; }", selector);
    }
}
