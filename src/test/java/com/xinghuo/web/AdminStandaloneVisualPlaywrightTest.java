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
class AdminStandaloneVisualPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void chromiumGuideAdminUsesCanonicalLightWorkspaceWithoutLegacyBridge() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1920, 1080));
            page.setContent(guideFixture());
            addCanonicalGuideStyles(page);

            assertEquals("rgb(244, 246, 248)", string(page, "getComputedStyle(document.body).backgroundColor"));
            assertEquals("rgb(255, 255, 255)", string(page, "getComputedStyle(document.querySelector('.guide-admin-toolbar')).backgroundColor"));
            assertEquals("rgb(255, 255, 255)", string(page, "getComputedStyle(document.querySelector('.guide-admin-list')).backgroundColor"));
            assertEquals("rgb(255, 255, 255)", string(page, "getComputedStyle(document.querySelector('.guide-editor-grid input')).backgroundColor"));
            Number shellWidth = number(page, "document.querySelector('.guide-admin-shell').getBoundingClientRect().width");
            assertTrue(shellWidth.doubleValue() >= 1400.0,
                    () -> "guide admin did not use the expected desktop canvas: " + shellWidth + "px");
            assertNoHorizontalOverflow(page, "guide admin desktop");
        }
    }

    @Test
    void webkitGuideAdminStaysOverflowFreeWithoutLegacyBridge() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions()
                    .setViewportSize(390, 844)
                    .setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 19_0 like Mac OS X) AppleWebKit/605.1.15 Version/19.0 Mobile/15E148 Safari/604.1"));
            page.setContent(guideFixture());
            addCanonicalGuideStyles(page);

            assertEquals("rgb(244, 246, 248)", string(page, "getComputedStyle(document.body).backgroundColor"));
            assertEquals("16px", string(page, "getComputedStyle(document.querySelector('.guide-editor-grid input')).fontSize"));
            assertNoHorizontalOverflow(page, "guide admin iPhone");
        }
    }

    private static void addCanonicalGuideStyles(Page page) {
        addStyle(page, "app.css");
        addStyle(page, "admin-guides-workspace.css");
        addStyle(page, "admin-light-theme.css");
        addStyle(page, "admin-brand-layout-fixes.css");
    }

    private static String guideFixture() {
        return """
                <body class="admin-solid-ui">
                  <header class="guide-admin-top">
                    <div class="container guide-admin-topbar">
                      <a class="btn ghost">返回运营后台</a>
                      <div><span class="eyebrow">内容运营</span><strong>多游戏攻略中心</strong></div>
                      <div class="row-actions"><button class="btn primary">新建攻略</button></div>
                    </div>
                  </header>
                  <main class="container guide-admin-shell">
                    <section class="card guide-admin-toolbar">
                      <div><h1>攻略内容管理</h1><p class="muted">维护攻略正文、常见问题与推荐服务。</p></div>
                      <div class="row-actions"><button class="btn">刷新列表</button></div>
                    </section>
                    <div class="guide-admin-layout">
                      <section class="card guide-admin-list">
                        <div class="guide-admin-list-head"><div><span class="eyebrow">内容列表</span><h2>已有攻略</h2></div></div>
                        <div class="guide-admin-rows">
                          <article class="guide-admin-row"><label class="guide-admin-row-check"><input type="checkbox"></label><button class="guide-admin-row-main"><b>MW4 推进攻略</b><small>已发布</small></button></article>
                        </div>
                      </section>
                      <section class="card guide-admin-editor">
                        <form id="guideForm">
                          <header class="guide-editor-head"><div><span class="eyebrow">编辑器</span><h2>编辑攻略</h2></div></header>
                          <div class="guide-editor-grid guide-editor-grid-2"><label class="field"><span>标题</span><input value="MW4 推进攻略"></label><label class="field"><span>状态</span><select><option>已发布</option></select></label></div>
                        </form>
                      </section>
                    </div>
                  </main>
                </body>
                """;
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
