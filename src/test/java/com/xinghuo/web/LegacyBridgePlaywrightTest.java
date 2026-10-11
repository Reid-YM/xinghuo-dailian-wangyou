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
class LegacyBridgePlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void chromiumWideLegacySurfacesUseTheDesktopCanvasWithoutOverflow() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            assertWideServiceDetail(browser);
            assertWideRecordDetail(browser);
            assertWideAccountCenter(browser);
            assertWideAccountSubmit(browser);
        }
    }

    @Test
    void webkitMobileLegacySurfacesStayOverflowFreeAndFormSafe() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            assertMobileServiceDetail(browser);
            assertMobileRecordDetail(browser);
            assertMobileAccountCenter(browser);
            assertMobileAccountSubmit(browser);
        }
    }

    private static void assertWideServiceDetail(Browser browser) {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1920, 1080));
        page.setContent("""
                <body data-page="service-detail">
                  <main class="container section page-shell service-detail-page">
                    <section class="card"><h1>服务详情</h1><p>可靠手打 · 纯绿服务 · 进度可查</p></section>
                    <section class="card"><h2>报价逻辑</h2><p>按当前进度到目标进度的差距计算。</p></section>
                  </main>
                </body>
                """);
        addStyle(page, "app.css");
        addStyle(page, "site-experience-v2.css");
        addStyle(page, "service-detail.css");

        assertBridgeBackground(page, "service-detail desktop");
        assertNoHorizontalOverflow(page, "service-detail desktop");
        Number width = number(page, "document.querySelector('main.container').getBoundingClientRect().width");
        assertTrue(width.doubleValue() >= 1650.0,
                () -> "service-detail did not use the wide desktop canvas: " + width + "px");
        page.close();
    }

    private static void assertWideRecordDetail(Browser browser) {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1920, 1080));
        page.setContent(recordDetailFixture());
        addStyle(page, "app.css");
        addStyle(page, "record-detail.css");

        assertBridgeBackground(page, "record-detail desktop");
        assertNoHorizontalOverflow(page, "record-detail desktop");
        Number width = number(page, "document.querySelector('main.container').getBoundingClientRect().width");
        assertTrue(width.doubleValue() >= 1650.0,
                () -> "record-detail did not use the wide desktop canvas: " + width + "px");
        page.close();
    }

    private static void assertWideAccountCenter(Browser browser) {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1920, 1080));
        page.setContent(accountCenterFixture());
        addStyle(page, "app.css");
        addStyle(page, "account.css");
        addStyle(page, "account-center.css");
        addStyle(page, "account-center-refine.css");

        assertBridgeBackground(page, "account center desktop");
        assertNoHorizontalOverflow(page, "account center desktop");
        Number width = number(page, "document.querySelector('.account-center-shell').getBoundingClientRect().width");
        assertTrue(width.doubleValue() >= 1650.0,
                () -> "account center did not use the wide desktop canvas: " + width + "px");
        page.close();
    }

    private static void assertWideAccountSubmit(Browser browser) {
        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1920, 1080));
        page.setContent(accountSubmitFixture());
        addStyle(page, "app.css");
        addStyle(page, "account.css");
        addStyle(page, "account-submit.css");

        assertBridgeBackground(page, "account submit desktop");
        assertNoHorizontalOverflow(page, "account submit desktop");
        Number width = number(page, "document.querySelector('.nexus').getBoundingClientRect().width");
        assertTrue(width.doubleValue() >= 1700.0,
                () -> "account submit did not use the wide desktop canvas: " + width + "px");
        page.close();
    }

    private static void assertMobileServiceDetail(Browser browser) {
        Page page = mobilePage(browser);
        page.setContent("""
                <body data-page="service-detail">
                  <main class="container section page-shell service-detail-page">
                    <section class="card"><h1>服务详情</h1><p>手机端服务说明与报价信息。</p></section>
                  </main>
                </body>
                """);
        addStyle(page, "app.css");
        addStyle(page, "site-experience-v2.css");
        addStyle(page, "service-detail.css");
        assertBridgeBackground(page, "service-detail iPhone");
        assertNoHorizontalOverflow(page, "service-detail iPhone");
        page.close();
    }

    private static void assertMobileRecordDetail(Browser browser) {
        Page page = mobilePage(browser);
        page.setContent(recordDetailFixture());
        addStyle(page, "app.css");
        addStyle(page, "record-detail.css");

        assertBridgeBackground(page, "record-detail iPhone");
        assertNoHorizontalOverflow(page, "record-detail iPhone");
        page.close();
    }

    private static void assertMobileAccountCenter(Browser browser) {
        Page page = mobilePage(browser);
        page.setContent(accountCenterFixture());
        addStyle(page, "app.css");
        addStyle(page, "account.css");
        addStyle(page, "account-center.css");
        addStyle(page, "account-center-refine.css");

        assertBridgeBackground(page, "account center iPhone");
        assertNoHorizontalOverflow(page, "account center iPhone");
        assertEquals("16px", string(page, "getComputedStyle(document.querySelector('input')).fontSize"));
        page.close();
    }

    private static void assertMobileAccountSubmit(Browser browser) {
        Page page = mobilePage(browser);
        page.setContent(accountSubmitFixture());
        addStyle(page, "app.css");
        addStyle(page, "account.css");
        addStyle(page, "account-submit.css");

        assertBridgeBackground(page, "account submit iPhone");
        assertNoHorizontalOverflow(page, "account submit iPhone");
        assertEquals("16px", string(page, "getComputedStyle(document.querySelector('textarea')).fontSize"));
        assertEquals("grid", string(page, "getComputedStyle(document.querySelector('.submission-editor-layout')).display"));
        String columns = string(page, "getComputedStyle(document.querySelector('.submission-editor-layout')).gridTemplateColumns");
        assertTrue(!columns.contains(" "),
                () -> "account-submit mobile editor must collapse to one column, got: " + columns);
        page.close();
    }

    private static Page mobilePage(Browser browser) {
        return browser.newPage(new Browser.NewPageOptions()
                .setViewportSize(390, 844)
                .setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 19_0 like Mac OS X) AppleWebKit/605.1.15 Version/19.0 Mobile/15E148 Safari/604.1"));
    }

    private static String recordDetailFixture() {
        return """
                <body data-page="record-detail">
                  <main class="container section page-shell">
                    <section class="card record-detail-card">
                      <h1>业务进度</h1>
                      <div class="record-meta-grid">
                        <div class="record-meta-item"><span>业务编号</span><strong>XH2026</strong></div>
                      </div>
                    </section>
                  </main>
                  <nav class="mobile-dock"><a>业务记录</a></nav>
                </body>
                """;
    }

    private static String accountCenterFixture() {
        return """
                <body class="account-center-page" data-page="account-center-orders">
                  <main class="account-center-shell">
                    <header class="account-center-bar"><div><strong>个人中心</strong><small>订单与资料</small></div></header>
                    <div class="account-center-layout">
                      <aside class="account-center-rail"><nav class="account-center-nav"><a class="is-active"><b>单</b><i><strong>我的订单</strong></i></a></nav></aside>
                      <div class="account-center-main">
                        <section class="account-center-hero"><div><h1>我的订单</h1><p>查看履约进度。</p></div></section>
                        <section class="account-center-card"><header><div><h2>履约记录</h2></div></header><div class="account-center-item"><div><input value="业务编号"></div></div></section>
                      </div>
                    </div>
                  </main>
                </body>
                """;
    }

    private static String accountSubmitFixture() {
        return """
                <body data-page="account-submit" class="nexus-page">
                  <main class="nexus">
                    <header class="nexus-commandbar"><div class="nexus-command-title"><span>攻略投稿控制台</span></div></header>
                    <div class="nexus-frame">
                      <aside class="nexus-rail"><nav class="nexus-nav"><a aria-current="page"><b>写</b><span>新建投稿</span></a></nav></aside>
                      <div class="nexus-stage">
                        <header class="nexus-hero"><div><h1>撰写并跟踪攻略投稿</h1></div></header>
                        <section class="nexus-module submission-workbench">
                          <div class="submission-editor-layout">
                            <div><textarea rows="8">## 示例</textarea></div>
                            <aside class="submission-preview">安全预览</aside>
                          </div>
                        </section>
                      </div>
                    </div>
                  </main>
                  <nav class="mobile-dock"><a>控制台</a><a class="primary">新建投稿</a></nav>
                </body>
                """;
    }

    private static void assertBridgeBackground(Page page, String label) {
        String background = string(page, "getComputedStyle(document.body).backgroundColor");
        assertEquals("rgb(11, 12, 15)", background, label + " must receive the expected dark surface background");
    }

    private static void assertNoHorizontalOverflow(Page page, String label) {
        Number scrollWidth = number(page, "document.documentElement.scrollWidth");
        Number clientWidth = number(page, "document.documentElement.clientWidth");
        assertTrue(scrollWidth.doubleValue() <= clientWidth.doubleValue() + 1,
                () -> label + " overflowed horizontally: scrollWidth=" + scrollWidth + ", clientWidth=" + clientWidth);
    }

    private static void addStyle(Page page, String asset) {
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve(asset)));
    }

    private static Number number(Page page, String expression) {
        return (Number) page.evaluate("() => " + expression);
    }

    private static String string(Page page, String expression) {
        return String.valueOf(page.evaluate("() => " + expression));
    }
}
