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
    void chromium390RendersV4AppHomepageWithoutOverflow() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
            page.setContent(markup());
            addStyles(page);
            assertLayout(page, 390);
        }
    }

    @Test
    void webkit360RendersV4AppHomepageWithoutOverflow() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(360, 780));
            page.setContent(markup());
            addStyles(page);
            assertLayout(page, 360);
        }
    }

    private static void assertLayout(Page page, int viewportWidth) {
        assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth"));
        assertEquals("none", css(page, "body > header", "display"));
        assertTrue((Boolean) page.evaluate("() => Math.abs(document.querySelector('#homeHero').getBoundingClientRect().width - innerWidth) < 1"));
        assertTrue((Boolean) page.evaluate("() => { const h=document.querySelector('#homeHero').getBoundingClientRect().height; return h >= 530 && h <= 590; }"));
        assertTrue((Boolean) page.evaluate("() => document.querySelector('.xh-v4-appbar').getBoundingClientRect().height >= 50"));

        assertTrue((Boolean) page.evaluate("() => { const a=[...document.querySelectorAll('.xh-v4-hero-actions .btn')]; const p=a[0].getBoundingClientRect(), s=a[1].getBoundingClientRect(); return Math.abs(p.top-s.top)<1 && p.width>s.width && p.height>=46 && s.height>=46; }"));
        assertTrue((Boolean) page.evaluate("() => { const a=[...document.querySelectorAll('.xh-v4-task')]; const r0=a[0].getBoundingClientRect(),r1=a[1].getBoundingClientRect(),r2=a[2].getBoundingClientRect(); return a.length===4 && Math.abs(r0.top-r1.top)<1 && r2.top>r0.top+20; }"));
        assertTrue((Boolean) page.evaluate("() => { const s=document.querySelector('#home-game'); const h=s.getBoundingClientRect().height; return h >= 39 && h <= 41; }"));
        assertEquals("2", (String) page.evaluate("() => getComputedStyle(document.querySelector('.xh-v4-form-grid')).gridTemplateColumns.split(' ').length.toString()"));
        assertTrue((Boolean) page.evaluate("() => { const nav=document.querySelector('.xh-v4-bottom-nav'); const r=nav.getBoundingClientRect(); return getComputedStyle(nav).position==='fixed' && r.left>=0 && r.right<=innerWidth && nav.querySelectorAll('a').length===5; }"));
        assertTrue((Boolean) page.evaluate("() => [...document.querySelectorAll('.xh-v4-launcher,.xh-v4-block,.xh-v4-quote,.xh-v4-proof')].every(el => el.getBoundingClientRect().right <= innerWidth + .5)"));
        assertTrue(viewportWidth == 360 || viewportWidth == 390);
    }

    private static String markup() {
        return """
            <!doctype html><html data-frontend="mobile"><body class="mobile-frontend" data-page="home">
            <header>legacy header</header>
            <main class="xh-v4" id="mobileHomeV4">
              <section class="xh-v4-hero hero" id="homeHero">
                <div id="heroBackground" class="hero-bg-layer"></div><div class="xh-v4-hero-shade"></div>
                <nav class="xh-v4-appbar"><a class="xh-v4-brand"><span class="xh-v4-logo">火</span><span class="xh-v4-brand-text"><strong>星火网游</strong><small>GAME SERVICE</small></span></a><div class="xh-v4-appbar-actions"><a>客服</a><a>我的</a></div></nav>
                <div class="xh-v4-hero-copy hero-copy"><div class="xh-v4-status">● 可提前询价</div><div class="xh-v4-eyebrow">CALL OF DUTY · MW4</div><h1 id="mobileHeroTitle">使命召唤 MW4 任务代练</h1><p class="xh-v4-subtitle">武器升级 · 迷彩解锁 · 活动任务</p><div class="xh-v4-meta"><div class="xh-v4-price"><small>参考起步</small><strong>¥20 起</strong><span>人工确认</span></div><div class="xh-v4-trust"><span>可靠手打</span><span>纯绿服务</span><span>进度可查</span></div></div><div class="actions hero-actions xh-v4-hero-actions"><a class="btn primary xh-v4-primary">立即询价</a><a class="btn ghost xh-v4-progress">查询进度</a></div></div>
              </section>
              <section class="xh-v4-launcher"><div class="xh-v4-launcher-head"><strong>你想做什么？</strong><span>选择任务</span></div><div class="xh-v4-task-grid"><a class="xh-v4-task"><i>⚙</i><strong>武器升级</strong><small>等级 · 配件</small></a><a class="xh-v4-task"><i>◆</i><strong>迷彩任务</strong><small>多人 · 战区</small></a><a class="xh-v4-task"><i>★</i><strong>活动任务</strong><small>赛季 · 挑战</small></a><a class="xh-v4-task"><i>＋</i><strong>其他需求</strong><small>人工确认</small></a></div></section>
              <section class="xh-v4-block"><div class="xh-v4-head"><div><span>HOT NOW</span><h2>当前热门</h2></div></div></section>
              <div class="xh-v4-proof"><a><b>编号</b><span>业务可查询</span></a><a><b>手打</b><span>过程可核对</span></a><a><b>担保</b><span>平台交易</span></a><a><b>安全</b><span>不收密码</span></a></div>
              <section class="xh-v4-quote" id="quick-actions"><div class="xh-v4-head"><div><span>QUICK QUOTE</span><h2>30 秒快速询价</h2></div></div><form class="xh-v4-quote-form"><div class="xh-v4-form-grid linked-selects four-step"><div class="field"><label>游戏 *</label><select id="home-game"><option>请选择</option></select></div><div class="field"><label>平台</label><select><option>不确定</option></select></div><div class="field"><label>需求类型 *</label><select><option>武器升级</option></select></div><div class="field"><label>具体服务 *</label><select><option>等级提升</option></select></div></div></form></section>
            </main>
            <nav class="xh-v4-bottom-nav"><a aria-current="page"><b>⌂</b><span>首页</span></a><a><b>▦</b><span>服务</span></a><a class="xh-v4-nav-quote"><b>＋</b><span>询价</span></a><a><b>◎</b><span>进度</span></a><a><b>○</b><span>我的</span></a></nav>
            </body></html>
            """;
    }

    private static void addStyles(Page page) {
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-compact.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-screenshot-fixes.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-flat-overflow.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-runtime-fixes.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-home-v4.css")));
    }

    private static String css(Page page, String selector, String property) {
        return (String) page.evaluate("([selector,property]) => getComputedStyle(document.querySelector(selector))[property]", new String[]{selector, property});
    }
}
