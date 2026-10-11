package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class GlobalUiEventSyncPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void mobileDrawerReappliesPersistedControlsEveryTimeItIsRendered() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
            page.setContent("""
                    <body>
                      <header class="site-header">
                        <div class="topbar">
                          <nav class="nav">
                            <a href="/services" data-global-ui="nav-services" data-global-visible="navServicesVisible">服务</a>
                            <a href="/guides" data-global-ui="nav-guides" data-global-visible="navGuidesVisible">攻略</a>
                            <a href="/mastery" data-global-ui="nav-mastery" data-global-visible="navMasteryVisible">MW4 工具站</a>
                            <a href="/records" data-global-ui="nav-records" data-global-visible="navRecordsVisible">业务记录</a>
                            <a href="/contact" data-global-ui="nav-contact" data-global-visible="navContactVisible">一对一咨询</a>
                            <a href="/account" data-account-entry data-global-visible="navAccountVisible"><span data-global-ui="nav-account">个人中心</span></a>
                            <a class="btn primary" href="/quote" data-global-visible="headerQuoteVisible">提前询价</a>
                          </nav>
                          <button type="button" class="nav-toggle"><span></span><span></span><span></span></button>
                        </div>
                      </header>
                      <footer>
                        <a data-global-ui="footer-services" data-global-visible="footerServicesVisible">服务大厅</a>
                        <a data-global-ui="footer-records" data-global-visible="footerRecordsVisible">业务记录</a>
                        <a data-global-ui="footer-console" data-global-visible="footerConsoleVisible">个人中心</a>
                        <a data-global-ui="footer-rules" data-global-visible="footerRulesVisible">服务规则</a>
                        <a data-global-ui="footer-privacy" data-global-visible="footerPrivacyVisible">隐私说明</a>
                        <a data-global-ui="footer-contact" data-global-visible="footerContactVisible">一对一咨询</a>
                      </footer>
                    </body>
                    """);
            page.evaluate("""
                    () => {
                      const json = value => new Response(JSON.stringify(value), {
                        status: 200,
                        headers: {'Content-Type': 'application/json'}
                      });
                      window.fetch = async url => {
                        if (url === '/api/account/me') return new Response('', {status: 401});
                        if (url === '/api/public/site-settings') return json({
                          navServicesLabel: '任务服务',
                          navServicesVisible: true,
                          navGuidesVisible: true,
                          navMasteryVisible: false,
                          navRecordsVisible: true,
                          navContactVisible: true,
                          navAccountVisible: true,
                          headerQuoteVisible: false,
                          footerServicesVisible: true,
                          footerRecordsVisible: true,
                          footerConsoleVisible: true,
                          footerRulesVisible: true,
                          footerPrivacyVisible: true,
                          footerContactVisible: true,
                          icpVisible: true,
                          publicSecurityVisible: true
                        });
                        if (url === '/api/site-config') return json({toolPublicEnabled: true});
                        throw new Error('unexpected fetch ' + url);
                      };
                    }
                    """);

            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("site-shell.js")));
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("global-ui-controls.js")));
            page.waitForFunction("() => document.querySelector('[data-global-ui=nav-services]')?.textContent === '任务服务'");

            assertEquals(Boolean.TRUE, page.evaluate("() => document.querySelector('a[href=\"/quote\"]').hidden"));
            assertEquals(Boolean.TRUE, page.evaluate("() => document.querySelector('#mobileNavDrawer .nav-drawer-cta').hidden"));

            page.locator(".nav-toggle").click();
            assertEquals(Boolean.FALSE, page.evaluate("() => document.querySelector('#mobileNavDrawer').hidden"));
            assertEquals("任务服务", page.locator("#mobileNavDrawer a[href='/services']").textContent());
            assertEquals(0, page.locator("#mobileNavDrawer a[href='/mastery']").count());
            assertEquals(Boolean.TRUE, page.evaluate("() => document.querySelector('#mobileNavDrawer .nav-drawer-cta').hidden"));
        }
    }
}
