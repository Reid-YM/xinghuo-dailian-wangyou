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
class MobileFinalWidthsPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void webkit360MasteryKeepsHiddenSyncActionAndNoHorizontalOverflow() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(360, 780));
            page.setContent(masteryMarkup());
            addSharedMobileStyles(page, true);
            addMobileScript(page);

            assertEquals(5, number(page, "document.querySelectorAll('#mobilePrimaryNav > a').length").intValue());
            assertEquals("none", string(page, "getComputedStyle(document.querySelector('.camo-sync-action')).display"));
            assertTrue(string(page, "getComputedStyle(document.querySelector('.camo-tabs')).overflowX").matches("auto|scroll"));
            assertTrue(number(page, "document.querySelector('.camo-hero .btn').getBoundingClientRect().height").doubleValue() >= 44.0);
            assertTrue(bool(page, "(() => { const a=[...document.querySelectorAll('.camo-filter-actions .btn')]; return a.length===2 && a[1].getBoundingClientRect().top > a[0].getBoundingClientRect().top; })()"));
            assertTrue(bool(page, "(() => { const a=[...document.querySelectorAll('.camo-pyro-card')]; return a.length===2 && Math.abs(a[0].getBoundingClientRect().top-a[1].getBoundingClientRect().top)<1; })()"));
            assertNoHorizontalOverflow(page, "WebKit 360 mastery");
        }
    }

    @Test
    void chromium390AccountRailScrollsAndKpisStayTwoColumns() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
            page.setContent(accountMarkup());
            addSharedMobileStyles(page, false);
            addMobileScript(page);

            assertEquals(5, number(page, "document.querySelectorAll('#mobilePrimaryNav > a').length").intValue());
            assertTrue(string(page, "getComputedStyle(document.querySelector('.account-center-nav')).overflowX").matches("auto|scroll"));
            assertTrue(bool(page, "document.querySelector('.account-center-nav').scrollWidth > document.querySelector('.account-center-nav').clientWidth"));
            assertTrue(bool(page, "(() => { const a=[...document.querySelectorAll('.account-center-kpi')]; return a.length===4 && Math.abs(a[0].getBoundingClientRect().top-a[1].getBoundingClientRect().top)<1 && a[1].getBoundingClientRect().left>a[0].getBoundingClientRect().left; })()"));
            assertTrue(number(page, "document.querySelector('.account-center-actions .btn').getBoundingClientRect().height").doubleValue() >= 44.0);
            assertNoHorizontalOverflow(page, "Chromium 390 account");
        }
    }

    @Test
    void chromium430UsesFinalGutterAndKeepsPrimaryNavInsideViewport() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(430, 860));
            page.setContent(servicesMarkup());
            addSharedMobileStyles(page, false);
            addMobileScript(page);

            double left = number(page, "document.querySelector('main.container').getBoundingClientRect().left").doubleValue();
            double right = number(page, "document.querySelector('main.container').getBoundingClientRect().right").doubleValue();
            assertTrue(left >= 13.0 && left <= 15.0, () -> "430px final gutter should be 14px, got " + left);
            assertTrue(right <= 430.5, () -> "430px main container escaped viewport: " + right);
            assertEquals("16px", string(page, "getComputedStyle(document.querySelector('input')).fontSize"));
            assertEquals(5, number(page, "document.querySelectorAll('#mobilePrimaryNav > a').length").intValue());
            assertTrue(number(page, "document.querySelector('#mobilePrimaryNav > a').getBoundingClientRect().height").doubleValue() >= 44.0);
            assertTrue(bool(page, "document.querySelector('#mobilePrimaryNav').getBoundingClientRect().right <= innerWidth + 0.5"));
            assertNoHorizontalOverflow(page, "Chromium 430 services");
        }
    }

    private static String masteryMarkup() {
        return """
                <!doctype html>
                <html data-frontend="mobile">
                <body class="mobile-frontend camo-tracker-body" data-page="mw4-camo-tracker" data-frontend="mobile">
                  <main class="container camo-shell">
                    <section class="camo-hero"><div class="camo-hero-copy"><h1>MW4 迷彩追踪器</h1><p>多人、战区、DMZ 与 Pyroclast。</p></div><a class="btn primary">带着进度询价</a></section>
                    <div class="camo-sync-bar"><span class="camo-sync-icon">☁</span><p><b>正在检查账号状态</b><span>登录后同步进度</span></p><a class="camo-sync-action hidden">登录同步</a></div>
                    <div class="camo-tabs"><button class="camo-tab active">多人</button><button class="camo-tab">战区</button><button class="camo-tab">DMZ</button><button class="camo-tab">Pyroclast</button></div>
                    <section class="camo-pyro-panel"><div class="camo-pyro-grid"><label class="camo-pyro-card"><span class="camo-pyro-media"></span><span class="camo-pyro-body"><b>Pyro 1</b></span></label><label class="camo-pyro-card"><span class="camo-pyro-media"></span><span class="camo-pyro-body"><b>Pyro 2</b></span></label></div></section>
                    <section class="camo-filters"><label><span>武器类型</span><select><option>全部</option></select></label><label><span>完成状态</span><select><option>全部</option></select></label><label class="camo-search-field"><span>搜索武器</span><input value="MW4"></label><div class="camo-filter-actions"><button class="btn ghost">重置当前进度</button><button class="btn ghost">清空收藏</button></div></section>
                    <section class="camo-class-group"><article class="weapon-tracker"><div class="weapon-tracker-head"><button class="camo-fav">★</button><button class="weapon-tracker-main"><span class="weapon-thumb is-empty"></span><span class="weapon-text"><strong>测试武器</strong><small>0 / 3</small></span><span class="weapon-progress-mini">0%</span></button></div></article></section>
                  </main>
                </body>
                </html>
                """;
    }

    private static String accountMarkup() {
        return """
                <!doctype html>
                <html data-frontend="mobile">
                <body class="mobile-frontend account-center-page" data-page="account" data-frontend="mobile">
                  <main class="account-center-shell">
                    <div class="account-center-bar"><div><strong>个人中心</strong><small>账号服务</small></div><nav><a>首页</a></nav></div>
                    <div class="account-center-layout">
                      <aside class="account-center-rail">
                        <div class="account-center-profile"><span class="account-avatar"></span><div><strong>测试用户</strong><small>已登录</small></div></div>
                        <nav class="account-center-nav"><a class="is-active"><strong>概览</strong></a><a><strong>询价</strong></a><a><strong>订单</strong></a><a><strong>进度</strong></a><a><strong>投稿</strong></a><a><strong>设置</strong></a></nav>
                      </aside>
                      <section class="account-center-main">
                        <div class="account-center-hero"><div><h1>我的星火</h1><p>统一管理订单、询价与工具进度。</p></div><div class="account-center-actions"><a class="btn primary">快速询价</a><a class="btn ghost">查看订单</a></div></div>
                        <div class="account-center-kpis"><article class="account-center-kpi"><strong>1</strong><small>询价</small></article><article class="account-center-kpi"><strong>2</strong><small>订单</small></article><article class="account-center-kpi"><strong>3</strong><small>进度</small></article><article class="account-center-kpi"><strong>4</strong><small>收藏</small></article></div>
                        <article class="account-center-card"><h2>账号状态</h2><p>手机端内容区域不能产生横向溢出。</p></article>
                      </section>
                    </div>
                  </main>
                </body>
                </html>
                """;
    }

    private static String servicesMarkup() {
        return """
                <!doctype html>
                <html data-frontend="mobile">
                <body class="mobile-frontend" data-page="services" data-frontend="mobile">
                  <header class="site-header"><div class="container topbar"><a class="brand">星火</a><nav class="nav"><a>服务</a></nav></div></header>
                  <main class="container page-shell">
                    <section class="page-hero page-hero-compact"><div><span class="eyebrow">SERVICES</span><h1>服务大厅</h1><p>选择 MW4 服务。</p></div><div class="page-hero-actions"><a class="btn primary">立即询价</a></div></section>
                    <section class="service-browser-console"><div class="service-browser-search"><input value="MW4"></div><div class="browser-row"><span class="browser-label">分类</span><div class="filters"><button class="filter-btn">全部</button><button class="filter-btn">MW4</button><button class="filter-btn">武器升级</button><button class="filter-btn">迷彩解锁</button></div></div></section>
                    <section class="service-browser-grid"><article class="card service-card"><h3>MW4 迷彩服务</h3><p>可靠手打 · 纯绿服务 · 进度可查</p><div class="service-meta"><strong>¥20 起</strong></div></article></section>
                  </main>
                </body>
                </html>
                """;
    }

    private static void addSharedMobileStyles(Page page, boolean mastery) {
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        if (mastery) {
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mw4-camo-tracker.css")));
        }
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-pages-v1.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-flows-v1.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-content-v1.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-account-v1.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-tune-v1.css")));
        if (mastery) {
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-mastery-v1.css")));
        }
    }

    private static void addMobileScript(Page page) {
        page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("mobile-app.js")));
        page.waitForTimeout(50);
    }

    private static void assertNoHorizontalOverflow(Page page, String label) {
        double scroll = number(page, "document.documentElement.scrollWidth").doubleValue();
        double client = number(page, "document.documentElement.clientWidth").doubleValue();
        assertTrue(scroll <= client + 1.0, () -> label + " overflowed horizontally: scroll=" + scroll + ", client=" + client);
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
