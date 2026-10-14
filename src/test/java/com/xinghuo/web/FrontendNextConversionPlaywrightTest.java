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
class FrontendNextConversionPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void quoteDesktopKeepsMainFormDominantAndMobileStacksSafely() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1440, 900));
            page.setContent("""
                    <body data-page="quote">
                      <main class="container section page-shell quote-page-r3">
                        <div class="page-stepbar"><span class="active"><b>1</b>选择需求</span><i></i><span><b>2</b>客服确认</span><i></i><span><b>3</b>查询进度</span></div>
                        <div class="quote-layout">
                          <div class="card order-card quote-main-card quote-card">
                            <div class="quote-card-head"><div class="service-state">可预约</div><span class="eyebrow">快速询价</span><h1>帮我报价</h1></div>
                            <div data-step-panel="1"><div class="linked-selects four-step quote-page-steps"><label class="field">游戏<select><option>MW4</option></select></label><label class="field">平台<select><option>Steam</option></select></label><label class="field">需求<select><option>武器</option></select></label><label class="field">服务<select><option>升级</option></select></label></div><div class="quote-extras-page"><label class="field">进度<select><option>进行中</option></select></label><label class="field">时间<select><option>尽快</option></select></label></div><div class="detail-list"><strong>参考价格</strong><span>¥20 起</span><strong>预计时间</strong><span>按进度</span></div><div class="actions"><button class="btn primary">下一步</button></div></div>
                          </div>
                          <aside class="card consult-card"><h2>需要人工确认？</h2><div class="consult-channel"><span>微信</span><strong>AAC9878</strong><button class="btn">复制</button></div><div class="quote-safe-panel"><strong>安全提交提醒</strong><p>不要提交密码验证码。</p></div></aside>
                        </div>
                      </main>
                      <nav class="mobile-dock"><a>首页</a><a>业务记录</a><a class="primary">联系客服</a></nav>
                    </body>
                    """);
            loadStyles(page);
            assertNoOverflow(page);
            Number mainWidth = (Number) page.evaluate("() => document.querySelector('.quote-main-card').getBoundingClientRect().width");
            Number asideWidth = (Number) page.evaluate("() => document.querySelector('.consult-card').getBoundingClientRect().width");
            assertTrue(mainWidth.doubleValue() > asideWidth.doubleValue());
            assertEquals(2, ((Number) page.evaluate("() => getComputedStyle(document.querySelector('.quote-page-steps')).gridTemplateColumns.split(' ').length")).intValue());

            page.setViewportSize(390, 844);
            assertNoOverflow(page);
            assertEquals(1, ((Number) page.evaluate("() => getComputedStyle(document.querySelector('.quote-layout')).gridTemplateColumns.split(' ').length")).intValue());
            assertEquals(1, ((Number) page.evaluate("() => getComputedStyle(document.querySelector('.quote-page-steps')).gridTemplateColumns.split(' ').length")).intValue());
            Number nextHeight = (Number) page.evaluate("() => document.querySelector('.quote-main-card .btn').getBoundingClientRect().height");
            assertTrue(nextHeight.doubleValue() >= 44.0);
        }
    }

    @Test
    void recordsDesktopUsesCardsAndMobileFallsBackToSingleColumn() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1920, 1080));
            page.setContent("""
                    <body data-page="records">
                      <main class="container section records-page page-shell">
                        <div class="record-search-card card"><form><label class="field">业务编号<input value="XH7KM4Q2AZ8P"></label><button class="btn primary">查询并加入记录</button><div data-message></div></form></div>
                        <div class="record-list section"><a class="card record-card"><div class="record-card-head"><span class="tag">询价</span><span>已报价</span></div><strong class="mono">XH0001</strong><h3>武器升级</h3><div class="muted">刚刚更新</div></a><a class="card record-card"><strong class="mono">XH0002</strong><h3>迷彩</h3></a><a class="card record-card"><strong class="mono">XH0003</strong><h3>活动</h3></a></div>
                      </main>
                    </body>
                    """);
            loadStyles(page);
            assertNoOverflow(page);
            assertEquals(3, ((Number) page.evaluate("() => getComputedStyle(document.querySelector('.record-list')).gridTemplateColumns.split(' ').length")).intValue());
            Number searchButtonHeight = (Number) page.evaluate("() => document.querySelector('.record-search-card .btn').getBoundingClientRect().height");
            assertTrue(searchButtonHeight.doubleValue() >= 48.0);

            page.setViewportSize(390, 844);
            assertNoOverflow(page);
            assertEquals(1, ((Number) page.evaluate("() => getComputedStyle(document.querySelector('.record-list')).gridTemplateColumns.split(' ').length")).intValue());
            assertEquals(1, ((Number) page.evaluate("() => getComputedStyle(document.querySelector('.record-search-card form')).gridTemplateColumns.split(' ').length")).intValue());
        }
    }

    private static void loadStyles(Page page) {
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("site-experience-v2.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("frontend-next-foundation.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("frontend-next-conversion.css")));
    }

    private static void assertNoOverflow(Page page) {
        Number scroll = (Number) page.evaluate("() => document.documentElement.scrollWidth");
        Number client = (Number) page.evaluate("() => document.documentElement.clientWidth");
        assertTrue(scroll.doubleValue() <= client.doubleValue() + 1,
                () -> "horizontal overflow: " + scroll + " > " + client);
    }
}
