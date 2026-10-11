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
class PageOwnershipPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void homeRecommendationsRenderFromPagesWithoutUiFixes() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <body data-page="home">
                      <section class="hero-media-panel"><div class="hero-media-copy"></div></section>
                      <div id="serviceGrid"></div>
                    </body>
                    """);
            page.evaluate("""
                    () => {
                      const service = {slug:'mw4-test', name:'MW4 测试服务', shortDesc:'用于页面职责回归', priceMode:'fixed', priceFixed:88};
                      window.XH = {
                        $: selector => document.querySelector(selector),
                        esc: value => String(value ?? '').replace(/[&<>'\"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','\"':'&quot;'}[c])),
                        loadConfig: async () => ({hotServices:[service], homeServiceLimit:4}),
                        loadServices: async () => [],
                        loadTaxonomy: async () => ({games:[]}),
                        updateDynamicStartingPrice: () => {},
                        bindUploadFields: () => {},
                        serviceCard: () => '',
                        track: () => {},
                        initLinkedQuoteForm: async () => {},
                        submitQuoteForm: async () => {},
                        normalizeRecordNo: value => value,
                        validRecordNo: () => true,
                        showMessage: () => {}
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("pages.js")));
            page.waitForSelector(".hero-hot-item");

            assertEquals("MW4 测试服务", page.locator(".hero-hot-item b").textContent());
            assertEquals("¥88", page.locator(".hero-hot-item em").textContent());
            assertTrue(page.locator(".hero-hot-item").getAttribute("href").endsWith("/service/mw4-test"));
        }
    }

    @Test
    void contactNormalizationAndQrPlacementLiveInPages() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <body data-page="contact">
                      <main>
                        <div class="contact-console">
                          <div id="contactChannelGrid"><article><span class="tag">主客服</span></article></div>
                          <div class="contact-safety">安全提示</div>
                        </div>
                        <div id="wechatQr">QR</div>
                      </main>
                    </body>
                    """);
            page.evaluate("""
                    () => {
                      window.XH = {
                        $: selector => document.querySelector(selector),
                        loadConfig: async () => ({}),
                        track: () => {},
                        esc: value => String(value ?? ''),
                        normalizeRecordNo: value => value,
                        validRecordNo: () => true,
                        showMessage: () => {}
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("pages.js")));
            page.waitForFunction("() => document.querySelector('#contactChannelGrid .tag')?.textContent === '微信'");

            assertEquals("微信", page.locator("#contactChannelGrid .tag").textContent());
            assertEquals("wechatQr", page.evaluate("() => document.querySelector('.contact-safety').previousElementSibling.id"));

            page.evaluate("() => document.querySelector('#contactChannelGrid').innerHTML = '<span class=\"tag\">默认客服</span>'");
            page.waitForFunction("() => document.querySelector('#contactChannelGrid .tag')?.textContent === '微信'");
            assertEquals("微信", page.locator("#contactChannelGrid .tag").textContent());
        }
    }
}
