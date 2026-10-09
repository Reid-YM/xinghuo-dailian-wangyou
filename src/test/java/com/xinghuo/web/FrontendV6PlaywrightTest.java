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
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("frontend-v6-geometry.css")));

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

    private static String css(Page page, String selector, String property) {
        return (String) page.evaluate("([selector,property]) => getComputedStyle(document.querySelector(selector))[property]", new String[]{selector, property});
    }
}
