package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class MobileV6VisualPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void chromiumPhoneWidthsLoadTheV6VisualSystemWithoutOverflow() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            for (int width : List.of(320, 360, 390, 430)) {
                Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, 844));
                renderFixture(page);
                assertV6(page, width);
                page.close();
            }
        }
    }

    @Test
    void webkit390LoadsTheSameV6Hierarchy() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
            renderFixture(page);
            assertV6(page, 390);
        }
    }

    private static void assertV6(Page page, int width) {
        assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= window.innerWidth"),
                "mobile V6 must not overflow at " + width + "px");

        double titleSize = number(page, "#mobileHeroTitle", "fontSize");
        assertTrue(titleSize >= (width <= 360 ? 38 : 40), "V6 hero title should be visibly larger at " + width + "px");

        double heroHeight = number(page, "#homeHero", "height");
        assertTrue(heroHeight >= 630, "V6 hero should dominate the first viewport at " + width + "px");

        String taskColumns = string(page, ".xh-v4-task-grid", "gridTemplateColumns");
        int expectedTaskColumns = width <= 360 ? 2 : 4;
        assertEquals(expectedTaskColumns, taskColumns.trim().split("\\s+").length,
                "task launcher column count at " + width + "px");

        String proofColumns = string(page, ".xh-v4-proof", "gridTemplateColumns");
        assertEquals(2, proofColumns.trim().split("\\s+").length, "trust panel should be 2x2");

        double submitHeight = number(page, "#submit", "height");
        assertTrue(submitHeight >= 54, "V6 quote CTA should keep a strong mobile touch target");
    }

    private static void renderFixture(Page page) {
        page.setContent("""
                <!doctype html>
                <html data-frontend="mobile">
                <body class="mobile-frontend" data-page="home">
                  <main class="xh-v4 container">
                    <section class="xh-v4-hero hero" id="homeHero">
                      <div class="xh-v4-hero-copy hero-copy">
                        <div class="xh-v4-status">● MW4 正式版预约中</div>
                        <div class="xh-v4-eyebrow">CALL OF DUTY · MW4</div>
                        <h1 id="mobileHeroTitle">使命召唤 MW4 任务代练</h1>
                        <p class="xh-v4-subtitle">武器升级 · 迷彩解锁 · 活动任务 · 排位冲分</p>
                        <div class="xh-v4-meta"><div class="xh-v4-price"><strong>¥20 起</strong></div></div>
                        <div class="xh-v4-hero-actions"><a class="btn xh-v4-primary">立即询价</a><a class="btn xh-v4-progress">查询进度</a></div>
                      </div>
                    </section>
                    <section class="xh-v4-launcher">
                      <div class="xh-v4-task-grid">
                        <a class="xh-v4-task"><i>⚙</i><strong>武器升级</strong><small>等级 · 配件</small></a>
                        <a class="xh-v4-task"><i>◆</i><strong>迷彩任务</strong><small>多人 · 战区</small></a>
                        <a class="xh-v4-task"><i>★</i><strong>活动任务</strong><small>赛季 · 挑战</small></a>
                        <a class="xh-v4-task"><i>＋</i><strong>其他需求</strong><small>人工确认</small></a>
                      </div>
                    </section>
                    <div class="xh-v4-proof">
                      <a><b>编号</b><span>业务可查询</span></a><a><b>手打</b><span>过程可核对</span></a>
                      <a><b>担保</b><span>平台规则交易</span></a><a><b>安全</b><span>网页不收密码</span></a>
                    </div>
                    <section id="quick-actions" class="xh-v4-quote">
                      <form class="xh-v4-quote-form"><div class="field"><label>需求</label><select><option>MW4</option></select></div><button id="submit" class="btn primary xh-v4-submit" type="button">提交需求并获取报价</button></form>
                    </section>
                  </main>
                </body>
                </html>
                """);
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-app.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-home-v4.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("mobile-v6.css")));
    }

    private static double number(Page page, String selector, String property) {
        Object value = page.evaluate("() => parseFloat(getComputedStyle(document.querySelector('" + selector + "'))." + property + ")");
        return ((Number) value).doubleValue();
    }

    private static String string(Page page, String selector, String property) {
        return (String) page.evaluate("() => getComputedStyle(document.querySelector('" + selector + "'))." + property);
    }
}
