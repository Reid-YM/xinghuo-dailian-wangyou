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
class FrontendV6PlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void chromiumDesktopRendersNewV6LayoutSystem() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1440, 900));
            page.setContent(desktopMarkup());
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("frontend-v6.css")));

            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth"));
            assertEquals("sticky", css(page, ".site-header", "position"));
            assertTrue((Boolean) page.evaluate("() => { const h=document.querySelector('.site-header').getBoundingClientRect().height; return h>=68 && h<=78; }"));
            assertEquals("30px", css(page, "#homeHero", "borderRadius"));
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#homeHero').getBoundingClientRect().width <= 1241"));
            assertTrue((Boolean) page.evaluate("() => parseFloat(getComputedStyle(document.querySelector('#homeHero h1')).fontSize) >= 48"));
            assertTrue((Boolean) page.evaluate("() => document.querySelector('.btn.primary').getBoundingClientRect().height >= 44"));
            assertTrue(css(page, ".btn.primary", "backgroundImage").contains("linear-gradient"));
            assertEquals("18px", css(page, ".card", "borderRadius"));
            assertTrue((Boolean) page.evaluate("() => document.querySelector('input').getBoundingClientRect().height >= 48"));
            assertEquals("2", (String) page.evaluate("() => getComputedStyle(document.querySelector('#quick-actions')).gridTemplateColumns.split(' ').length.toString()"));
        }
    }

    @Test
    void webkit390RendersV6MobileHomeWithoutBlackFrameOrOverflow() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
            page.setContent(mobileMarkup());
            addMobileStyles(page);

            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth"));
            assertTrue((Boolean) page.evaluate("() => Math.abs(document.querySelector('#homeHero').getBoundingClientRect().width - innerWidth) < 1"));
            assertTrue((Boolean) page.evaluate("() => { const h=document.querySelector('#homeHero').getBoundingClientRect().height; return h>=500 && h<=550; }"));
            assertEquals("0px", css(page, "#homeHero", "borderRadius"));
            assertEquals("block", css(page, "#homeHero .mobile-hero-backdrop", "display"));
            assertEquals("cover", css(page, "#homeHero .mobile-hero-backdrop", "backgroundSize"));
            assertTrue(css(page, ".xh-v4-logo", "backgroundImage").contains("brand-logo.svg"));
            assertTrue((Boolean) page.evaluate("() => [...document.querySelectorAll('.xh-v4-hero-actions .btn')].every(x => x.getBoundingClientRect().height >= 45)"));
            assertTrue((Boolean) page.evaluate("() => [...document.querySelectorAll('.xh-v4-launcher,.xh-v4-quote')].every(x => x.getBoundingClientRect().right <= innerWidth + .5)"));
            assertEquals("16px", css(page, ".xh-v4-launcher", "borderRadius"));
        }
    }

    private static String desktopMarkup() {
        return """
            <!doctype html><html data-xh-ui="v6"><body data-page="home">
            <header class="site-header"><div class="container topbar"><a class="brand brand-mark"><img src="/assets/brand-logo.svg"><span>星火网游代练平台</span></a><nav class="nav"><a href="/services">服务</a><a href="/records">业务记录</a><a class="btn primary" href="/quote">提前询价</a></nav></div></header>
            <main class="container">
              <section class="hero" id="homeHero"><div class="hero-bg-layer"></div><div class="hero-copy"><div class="hero-signal"><div class="service-state">● MW4 可询价</div><span class="hero-kicker">CODMW4 · 可靠手打 · 进度可查</span></div><h1>使命召唤 MW4 任务代练</h1><p class="hero-subtitle">武器升级 · 迷彩解锁 · 活动任务</p><div class="hero-promise"><span><b>可靠手打</b><small>人工推进</small></span><span><b>纯绿服务</b><small>不使用外挂</small></span></div><div class="hero-price-row"><div class="price">¥20 起</div></div><div class="actions hero-actions"><a class="btn primary">立即询价</a><a class="btn ghost">查进度</a></div></div><aside class="hero-panel"><div class="hero-visual"></div></aside></section>
              <section class="section quick-grid" id="quick-actions"><div class="card"><div class="quote-card-head"><span class="eyebrow">快速询价</span><h2>选择需求，获取报价</h2></div><form><div class="field"><label>游戏</label><input value="MW4"></div><button class="btn primary">提交</button></form></div><div class="card"><h2>业务进度</h2></div></section>
            </main>
            <footer class="site-footer"><div class="container footer-row"><strong>星维网络服务工作室</strong></div></footer>
            </body></html>
            """;
    }

    private static String mobileMarkup() {
        return """
            <!doctype html><html data-frontend="mobile" data-xh-ui="v6"><body class="mobile-frontend" data-page="home">
            <header>legacy</header>
            <main class="xh-v4" id="mobileHomeV4">
              <section class="xh-v4-hero hero" id="homeHero"><div class="mobile-hero-backdrop" style="background-image:linear-gradient(#7a6b38,#1a1d22)"></div><div id="heroBackground" class="hero-bg-layer"></div><div class="xh-v4-hero-shade"></div><nav class="xh-v4-appbar"><a class="xh-v4-brand"><span class="xh-v4-logo">火</span><span class="xh-v4-brand-text"><strong>星火网游</strong><small>GAME SERVICE</small></span></a><div class="xh-v4-appbar-actions"><a>客服</a><a>我的</a></div></nav><div class="xh-v4-hero-copy hero-copy"><div class="xh-v4-status">● 可提前询价</div><div class="xh-v4-eyebrow">CALL OF DUTY · MW4</div><h1 id="mobileHeroTitle">使命召唤 MW4 任务代练</h1><p class="xh-v4-subtitle">武器升级 · 迷彩解锁 · 活动任务</p><div class="xh-v4-meta"><div class="xh-v4-price"><small>参考起步</small><strong>¥20 起</strong><span>人工确认</span></div><div class="xh-v4-trust"><span>可靠手打</span><span>纯绿服务</span><span>进度可查</span></div></div><div class="actions hero-actions xh-v4-hero-actions"><a class="btn primary xh-v4-primary">立即询价</a><a class="btn ghost xh-v4-progress">查询进度</a></div></div></section>
              <section class="xh-v4-launcher"><div class="xh-v4-launcher-head"><strong>你想做什么？</strong><span>选择任务</span></div><div class="xh-v4-task-grid"><a class="xh-v4-task"><i>⚙</i><strong>武器升级</strong><small>等级 · 配件</small></a><a class="xh-v4-task"><i>◆</i><strong>迷彩任务</strong><small>多人 · 战区</small></a><a class="xh-v4-task"><i>★</i><strong>活动任务</strong><small>赛季 · 挑战</small></a><a class="xh-v4-task"><i>＋</i><strong>其他需求</strong><small>人工确认</small></a></div></section>
              <section class="xh-v4-quote" id="quick-actions"><div class="xh-v4-head"><div><span>QUICK QUOTE</span><h2>30 秒快速询价</h2></div></div><form><div class="field"><label>游戏</label><select><option>MW4</option></select></div></form></section>
            </main>
            </body></html>
            """;
    }

    private static void addMobileStyles(Page page) {
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-compact.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-screenshot-fixes.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-flat-overflow.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-runtime-fixes.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-home-v4.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("frontend-v6.css")));
    }

    private static String css(Page page, String selector, String property) {
        return (String) page.evaluate("([selector,property]) => getComputedStyle(document.querySelector(selector))[property]", new String[]{selector, property});
    }
}
