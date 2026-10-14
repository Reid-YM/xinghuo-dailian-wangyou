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
class FrontendNextAccountPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void loginDesktopKeepsValuePanelAndAuthCardBalancedWhileMobilePrioritizesSignIn() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1440, 900));
            page.setContent("""
                    <body data-page="account-login">
                      <main class="container section auth-shell">
                        <section class="auth-value-panel"><span class="eyebrow">账号中心</span><h1>登录后，询价记录与交付进度集中在一处。</h1><p>账号只保存联系方式、业务编号与进度状态。</p><div class="auth-value-grid"><div><strong>记录归档</strong><span>换设备也能查。</span></div><div><strong>进度同步</strong><span>同步云端。</span></div><div><strong>设备可控</strong><span>随时下线。</span></div></div><div class="detail-list"><strong>忘记密码</strong><span>邮箱重置</span></div></section>
                        <section class="auth-card card"><div class="auth-card-head"><h2>继续查看你的业务</h2></div><div class="mini-login"><button class="btn primary">使用星火迷彩追踪器小程序登录</button><div class="mini-login-challenge"><strong data-mini-code>12345678</strong></div></div><form class="account-form"><label class="field">邮箱<input value="you@example.com"></label><label class="field password-field">密码<input type="password"><button class="password-toggle" type="button">显示</button></label><button class="btn primary auth-submit">登录</button></form></section>
                      </main>
                    </body>
                    """);
            loadStyles(page);
            assertNoOverflow(page);
            assertEquals(2, columns(page, ".auth-shell"));
            Number valueWidth = number(page, "document.querySelector('.auth-value-panel').getBoundingClientRect().width");
            Number cardWidth = number(page, "document.querySelector('.auth-card').getBoundingClientRect().width");
            assertTrue(valueWidth.doubleValue() > cardWidth.doubleValue(), "desktop login should keep explanatory area wider than the form card");
            assertTrue(number(page, "document.querySelector('.auth-submit').getBoundingClientRect().height").doubleValue() >= 48.0);

            page.setViewportSize(390, 844);
            assertNoOverflow(page);
            assertEquals(1, columns(page, ".auth-shell"));
            assertEquals("none", string(page, "getComputedStyle(document.querySelector('.auth-value-grid')).display"));
            assertEquals("16px", string(page, "getComputedStyle(document.querySelector('.account-form input')).fontSize"));
        }
    }

    @Test
    void accountCenterDesktopHasRailAndFourKpisThenCollapsesCleanlyOnMobile() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1920, 1080));
            page.setContent("""
                    <body class="account-center-page" data-page="account-center-overview">
                      <main class="account-center-shell">
                        <header class="account-center-bar"><div><strong>个人中心</strong><small>需求、订单、资料与安全各归其位</small></div><nav><a>首页</a><a>编号查询</a><button>退出</button></nav></header>
                        <div class="account-center-layout">
                          <aside class="account-center-rail"><div class="account-center-profile"><div class="account-avatar"></div><div><span>当前账号</span><strong>测试用户</strong><small>已登录</small></div></div><nav class="account-center-nav"><span>业务中心</span><a class="is-active"><b>概</b><i><strong>总览</strong><small>状态与任务</small></i></a><a><b>需</b><i><strong>我的需求</strong><small>询价</small></i></a><a><b>单</b><i><strong>我的订单</strong><small>交付</small></i></a></nav><div class="account-center-safety"><strong>安全边界</strong><p>不索取密码。</p></div></aside>
                          <div class="account-center-main"><section class="account-center-hero"><div><span class="account-center-eyebrow">OVERVIEW</span><h1>欢迎回到个人中心</h1><p>查看关键状态。</p></div><div class="account-center-actions"><a class="btn primary">提交新需求</a></div></section><section class="account-center-kpis"><article class="account-center-kpi"><span>需求</span><strong>3</strong></article><article class="account-center-kpi"><span>订单</span><strong>2</strong></article><article class="account-center-kpi"><span>进行中</span><strong>1</strong></article><article class="account-center-kpi"><span>安全</span><strong>4</strong></article></section><div class="account-center-grid"><section class="account-center-card">最近需求</section><section class="account-center-card">最近订单</section></div><section class="account-center-quick"><a>个人资料</a><a>联系方式</a><a>工具存档</a></section></div>
                        </div>
                      </main>
                    </body>
                    """);
            loadStyles(page);
            assertNoOverflow(page);
            assertEquals(2, columns(page, ".account-center-layout"));
            assertEquals(4, columns(page, ".account-center-kpis"));
            Number shellWidth = number(page, "document.querySelector('.account-center-shell').getBoundingClientRect().width");
            assertTrue(shellWidth.doubleValue() >= 1600.0, () -> "account center did not use wide desktop canvas: " + shellWidth);

            page.setViewportSize(412, 915);
            assertNoOverflow(page);
            assertEquals(1, columns(page, ".account-center-layout"));
            assertEquals(2, columns(page, ".account-center-kpis"));
            assertEquals("none", string(page, "getComputedStyle(document.querySelector('.account-center-profile')).display"));
            assertTrue(number(page, "document.querySelector('.account-center-nav').scrollWidth").doubleValue() >= number(page, "document.querySelector('.account-center-nav').clientWidth").doubleValue());

            page.setViewportSize(390, 844);
            assertNoOverflow(page);
            assertEquals(1, columns(page, ".account-center-kpis"));
        }
    }

    private static void loadStyles(Page page) {
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("site-experience-v2.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("frontend-next-foundation.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("frontend-next-account.css")));
    }

    private static int columns(Page page, String selector) {
        return ((Number) page.evaluate("selector => getComputedStyle(document.querySelector(selector)).gridTemplateColumns.split(' ').length", selector)).intValue();
    }

    private static Number number(Page page, String expression) {
        return (Number) page.evaluate("() => " + expression);
    }

    private static String string(Page page, String expression) {
        return String.valueOf(page.evaluate("() => " + expression));
    }

    private static void assertNoOverflow(Page page) {
        Number scroll = number(page, "document.documentElement.scrollWidth");
        Number client = number(page, "document.documentElement.clientWidth");
        assertTrue(scroll.doubleValue() <= client.doubleValue() + 1,
                () -> "horizontal overflow: " + scroll + " > " + client);
    }
}
