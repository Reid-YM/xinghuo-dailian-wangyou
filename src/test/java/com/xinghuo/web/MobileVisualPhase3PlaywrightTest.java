package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class MobileVisualPhase3PlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void mobileRuntimeAppliesIndependentOpacityAndBlurWithoutChangingFit() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
            page.route("**/api/public/mobile-visual-config", route -> route.fulfill(
                    new Route.FulfillOptions()
                            .setStatus(200)
                            .setContentType("application/json")
                            .setBody("{\"heroBackgroundMobileOpacity\":42,\"heroBackgroundMobileBlur\":7}")));
            page.setContent("""
                    <html data-frontend="mobile"><head><base href="https://test.local/"></head><body data-page="home" class="mobile-frontend">
                      <section id="homeHero" style="--image-text-dim:.22">
                        <div class="mobile-hero-backdrop" style="background-image:url('/x.jpg');opacity:.55"></div>
                        <div id="heroBackground" style="background-image:url('/x.jpg');background-size:contain!important;background-position:61% 35%!important;opacity:.9!important;filter:brightness(1.1) blur(0px)!important"></div>
                      </section>
                    </body></html>
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("mobile-visual-runtime.js")));
            page.waitForFunction("() => document.querySelector('#heroBackground').style.opacity === '0.42'");

            assertEquals("contain", page.evaluate("() => document.querySelector('#heroBackground').style.backgroundSize"));
            assertEquals("61% 35%", page.evaluate("() => document.querySelector('#heroBackground').style.backgroundPosition"));
            assertTrue(String.valueOf(page.evaluate("() => document.querySelector('#heroBackground').style.filter")).contains("blur(7px)"));
            Number backdropOpacity = (Number) page.evaluate("() => Number(document.querySelector('.mobile-hero-backdrop').style.opacity)");
            assertTrue(backdropOpacity.doubleValue() > 0.25 && backdropOpacity.doubleValue() < 0.27);
        }
    }

    @Test
    void adminControlsLoadPreviewAndPersistDedicatedMobileValues() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1200, 900));
            AtomicReference<String> patchBody = new AtomicReference<>();
            page.route("**/api/admin/mobile-visual-config", route -> {
                if ("PATCH".equals(route.request().method())) {
                    patchBody.set(route.request().postData());
                    route.fulfill(new Route.FulfillOptions()
                            .setStatus(200)
                            .setContentType("application/json")
                            .setBody("{\"heroBackgroundMobileOpacity\":33,\"heroBackgroundMobileBlur\":11}"));
                } else {
                    route.fulfill(new Route.FulfillOptions()
                            .setStatus(200)
                            .setContentType("application/json")
                            .setBody("{\"heroBackgroundMobileOpacity\":47,\"heroBackgroundMobileBlur\":9}"));
                }
            });

            page.setContent("""
                    <html><head><base href="https://test.local/"><meta name="_csrf" content="token"><meta name="_csrf_header" content="X-CSRF-TOKEN"></head>
                    <body>
                      <input id="configSectionReason" value="phase3 test">
                      <form id="configForm">
                        <section data-config-section="visual">
                          <div class="grid2">
                            <div class="field"><label>背景显示强度（%）</label><input name="heroBackgroundOpacity" value="90"><div class="form-help">强度和模糊全端共用</div></div>
                            <div class="field"><label>背景模糊（px）</label><input name="heroBackgroundBlur" value="0"></div>
                          </div>
                          <div class="responsive-visual-grid">
                            <div><h4>电脑端背景</h4></div>
                            <div><h4>手机端背景</h4><div class="field"><input name="heroBackgroundMobileHeight" value="520"></div></div>
                          </div>
                          <button id="autoFitHeroBackground" type="button">恢复默认参数</button>
                          <div id="heroBackgroundLivePreview" data-device="mobile" style="--image-text-dim:.22">
                            <div class="admin-hero-bg-live-layer" style="opacity:.9!important;filter:blur(0px)!important"></div>
                          </div>
                        </section>
                      </form>
                    </body></html>
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-mobile-visual-config.js")));
            page.waitForFunction("() => document.querySelector('#heroBackgroundMobileOpacityIndependent')?.value === '47'");

            assertEquals("手机背景显示强度（%）", page.locator("label[for=heroBackgroundMobileOpacityIndependent]").textContent());
            page.locator("#heroBackgroundMobileOpacityIndependent").fill("33");
            page.locator("#heroBackgroundMobileBlurIndependent").fill("11");
            page.waitForFunction("() => document.querySelector('.admin-hero-bg-live-layer').style.opacity === '0.33'");
            assertTrue(String.valueOf(page.evaluate("() => document.querySelector('.admin-hero-bg-live-layer').style.filter")).contains("blur(11px)"));

            page.locator("#saveMobileBackgroundVisual").click();
            page.waitForFunction("() => document.querySelector('#mobileBackgroundVisualState')?.textContent === '手机参数已同步'");
            assertNotNull(patchBody.get());
            assertTrue(patchBody.get().contains("\"heroBackgroundMobileOpacity\":33"));
            assertTrue(patchBody.get().contains("\"heroBackgroundMobileBlur\":11"));
            assertTrue(patchBody.get().contains("phase3 test"));
        }
    }
}
