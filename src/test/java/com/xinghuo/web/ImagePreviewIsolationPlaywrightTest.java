package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class ImagePreviewIsolationPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void layoutResolverHonorsExplicitDeviceScope() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1366, 900));
            page.setContent("<body></body>");
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("site-image-controls.js")));

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) page.evaluate("""
                () => {
                  const c = {
                    heroBackgroundOpacity: 91,
                    heroBackgroundMobileOpacity: 73,
                    heroBackgroundBlur: 5,
                    heroBackgroundMobileBlur: 2,
                    heroBackgroundFit: 'contain',
                    heroBackgroundPositionX: 11,
                    heroBackgroundPositionY: 22,
                    heroBackgroundMobileFit: 'cover',
                    heroBackgroundMobilePositionX: 77,
                    heroBackgroundMobilePositionY: 33,
                    heroBackgroundMobileHeight: 610,
                    heroImageHeight: 120,
                    heroImageMobileHeight: 190,
                    heroImageFit: 'contain',
                    heroImageMobileFit: 'cover'
                  };
                  const desktop = SiteImageControls.resolveBackground(c, 'desktop');
                  const mobile = SiteImageControls.resolveBackground(c, 'mobile');
                  const mobileMain = SiteImageControls.resolveMain(c, 'mobile');
                  return {
                    desktopSize: desktop.backgroundSize,
                    desktopPosition: desktop.backgroundPosition,
                    desktopOpacity: desktop.opacity,
                    desktopBlur: desktop.blur,
                    mobileSize: mobile.backgroundSize,
                    mobilePosition: mobile.backgroundPosition,
                    mobileOpacity: mobile.opacity,
                    mobileBlur: mobile.blur,
                    mobileHeight: mobile.height,
                    mobileMainHeight: mobileMain.height,
                    mobileMainFit: mobileMain.fit
                  };
                }
                """);

            assertEquals("contain", result.get("desktopSize"));
            assertEquals("11% 22%", result.get("desktopPosition"));
            assertEquals(0.91, ((Number) result.get("desktopOpacity")).doubleValue(), 0.0001);
            assertEquals(5, ((Number) result.get("desktopBlur")).intValue());
            assertEquals("cover", result.get("mobileSize"));
            assertEquals("77% 33%", result.get("mobilePosition"));
            assertEquals(0.73, ((Number) result.get("mobileOpacity")).doubleValue(), 0.0001);
            assertEquals(2, ((Number) result.get("mobileBlur")).intValue());
            assertEquals("610px", result.get("mobileHeight"));
            assertEquals("190px", result.get("mobileMainHeight"));
            assertEquals("cover", result.get("mobileMainFit"));
        }
    }

    @Test
    void adminMobilePreviewDoesNotDependOnDesktopBrowserWidth() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1366, 900));
            page.setContent("""
                <body class="admin-control-center-v2">
                  <form id="configForm">
                    <section data-config-section="visual">
                      <div id="visualSaveState"></div>
                      <input type="hidden" name="heroBackgroundImage" value="/desktop.jpg">
                      <input type="hidden" name="heroBackgroundMobileImage" value="/mobile.jpg">
                      <input name="heroBackgroundOpacity" value="90">
                      <input name="heroBackgroundBlur" value="4">
                      <select name="heroBackgroundFit"><option value="contain" selected>contain</option></select>
                      <input name="heroBackgroundPositionX" value="12">
                      <input name="heroBackgroundPositionY" value="24">
                      <input name="heroBackgroundMobileHeight" value="610">
                      <select name="heroBackgroundMobileFit"><option value="cover" selected>cover</option></select>
                      <input name="heroBackgroundMobilePositionX" value="76">
                      <input name="heroBackgroundMobilePositionY" value="34">
                      <input name="heroImageHeight" value="120">
                      <select name="heroImageFit"><option value="contain" selected>contain</option></select>
                      <input name="heroImagePositionX" value="50">
                      <input name="heroImagePositionY" value="50">
                      <input name="heroImageMobileHeight" value="180">
                      <select name="heroImageMobileFit"><option value="cover" selected>cover</option></select>
                      <input name="heroImageMobilePositionX" value="50">
                      <input name="heroImageMobilePositionY" value="50">
                      <div id="heroImagePreview"><img alt="hero"></div>
                      <div id="heroBackgroundLivePreview" data-device="desktop">
                        <div class="admin-hero-bg-live-layer"></div>
                      </div>
                      <button type="button" data-visual-device="desktop">电脑</button>
                      <button type="button" data-visual-device="mobile">手机</button>
                    </section>
                  </form>
                </body>
                """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("site-image-controls.js")));
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-image-editor.js")));
            page.evaluate("document.dispatchEvent(new CustomEvent('site-config-loaded'))");

            assertEquals("contain", page.evaluate("getComputedStyle(document.querySelector('.admin-hero-bg-live-layer')).backgroundSize"));
            assertEquals("12% 24%", page.evaluate("getComputedStyle(document.querySelector('.admin-hero-bg-live-layer')).backgroundPosition"));

            page.locator("[data-visual-device='mobile']").click();
            assertEquals("mobile", page.locator("#heroBackgroundLivePreview").getAttribute("data-device"));
            assertEquals("cover", page.evaluate("getComputedStyle(document.querySelector('.admin-hero-bg-live-layer')).backgroundSize"));
            assertEquals("76% 34%", page.evaluate("getComputedStyle(document.querySelector('.admin-hero-bg-live-layer')).backgroundPosition"));
            assertEquals("610px", page.evaluate("getComputedStyle(document.querySelector('#heroBackgroundLivePreview')).getPropertyValue('--hero-mobile-background-height').trim()"));

            page.locator("[data-visual-device='desktop']").click();
            assertEquals("contain", page.evaluate("getComputedStyle(document.querySelector('.admin-hero-bg-live-layer')).backgroundSize"));
            assertEquals("12% 24%", page.evaluate("getComputedStyle(document.querySelector('.admin-hero-bg-live-layer')).backgroundPosition"));
        }
    }
}
