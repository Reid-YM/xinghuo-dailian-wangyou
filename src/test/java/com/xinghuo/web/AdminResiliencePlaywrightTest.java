package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class AdminResiliencePlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void missingAdminPanelKeepsCurrentPanelVisibleAndShowsPersistentError() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <body>
                      <main>
                        <section data-panel="dashboard">DASHBOARD</section>
                        <section class="hidden" data-panel="users">USERS</section>
                      </main>
                      <button id="brokenNav" data-nav="missingModule">打开缺失模块</button>
                      <div id="toastArea"></div>
                    </body>
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-resilience.js")));

            page.locator("#brokenNav").click();

            assertEquals(Boolean.FALSE, page.evaluate("() => document.querySelector('[data-panel=dashboard]').classList.contains('hidden')"));
            assertEquals(1, page.locator("[data-panel=dashboard] [data-admin-panel-error]").count());
            assertTrue(page.locator("[data-admin-panel-error]").textContent().contains("missingModule"));
        }
    }

    @Test
    void errorToastIsPromotedIntoVisiblePanelInsteadOfDisappearingIntoBlankScreen() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <body>
                      <section class="hidden" data-panel="dashboard">DASHBOARD</section>
                      <section data-panel="toolStation"><div class="admin-toolbar">TOOLS</div></section>
                      <div id="toastArea"></div>
                    </body>
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-resilience.js")));
            page.evaluate("""
                    () => {
                      const toast = document.createElement('div');
                      toast.className = 'toast error';
                      toast.textContent = '工具站数据读取失败';
                      document.getElementById('toastArea').appendChild(toast);
                    }
                    """);
            page.waitForSelector("[data-panel=toolStation] [data-admin-panel-error]");

            assertTrue(page.locator("[data-admin-panel-error]").textContent().contains("工具站数据读取失败"));
            assertEquals(2, page.locator("[data-admin-panel-error] button").count());
        }
    }

    @Test
    void adminToolStationLoadsResilienceAssetAndHasExplicitEmptyState() throws Exception {
        String source = Files.readString(ASSETS.resolve("admin-tool-station.js"));
        assertTrue(source.contains("/assets/admin-resilience.js?v=20261007r001"));
        assertTrue(source.contains("当前游戏没有可编辑的武器资料"));
        assertTrue(source.contains("登录已失效，请重新登录"));
    }
}
