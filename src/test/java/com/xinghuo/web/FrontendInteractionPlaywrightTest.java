package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ReducedMotion;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendInteractionPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");

    @Test
    void settingsNavigationUsesOneHashControllerAcrossResponsiveWidths() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            for (int width : new int[]{375, 768, 1440}) {
                Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, 900));
                page.setContent("""
                        <body data-page="account-settings">
                          <main data-settings-hub>
                            <nav>
                              <a href="#profile" data-settings-target="profile">资料</a>
                              <a href="#contact" data-settings-target="contact">回访</a>
                              <a href="#security" data-settings-target="security">安全</a>
                            </nav>
                            <div data-settings-view="profile">PROFILE</div>
                            <div data-settings-view="contact" hidden>CONTACT</div>
                            <div data-settings-view="security" hidden>
                              <section id="password">PASSWORD</section>
                            </div>
                          </main>
                        </body>
                        """);
                page.evaluate("""
                        () => {
                          window.__scrollCalls = 0;
                          Element.prototype.scrollIntoView = function () { window.__scrollCalls += 1; };
                          location.hash = '#password';
                        }
                        """);
                page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("account-center.js")));
                page.waitForTimeout(50);

                assertEquals(Boolean.FALSE, page.evaluate("() => document.querySelector('[data-settings-view=security]').hidden"));
                assertEquals(Boolean.TRUE, page.evaluate("() => document.querySelector('[data-settings-view=profile]').hidden"));
                assertEquals(1, ((Number) page.evaluate("() => window.__scrollCalls")).intValue(),
                        "hash navigation must scroll exactly once");

                page.locator("[data-settings-target=profile]").click();
                assertEquals(Boolean.FALSE, page.evaluate("() => document.querySelector('[data-settings-view=profile]').hidden"));
                assertEquals("#profile", page.evaluate("() => location.hash"));
                page.close();
            }
        }
    }

    @Test
    void profileControllerPreservesAvatarOnNicknameOnlySave() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <body data-page="account-settings">
                      <span class="account-avatar" data-profile-avatar-preview></span>
                      <strong data-profile-public-id></strong>
                      <form id="accountProfileFormV2">
                        <input name="displayName">
                        <input name="avatarFile" type="file">
                        <div data-profile-msg hidden></div>
                        <button type="submit" data-submit>保存资料</button>
                      </form>
                    </body>
                    """);
            page.evaluate("""
                    () => {
                      window.__profilePosts = 0;
                      window.__profileBody = null;
                      const profile = {displayName:'旧昵称', avatarUrl:'/avatar/existing.png', publicUserId:'U-1'};
                      window.fetch = async (url, options = {}) => {
                        const method = String(options.method || 'GET').toUpperCase();
                        if (url === '/api/account/me') {
                          return new Response(JSON.stringify(profile), {status:200, headers:{'Content-Type':'application/json'}});
                        }
                        if (url === '/api/account/profile' && method === 'POST') {
                          window.__profilePosts += 1;
                          window.__profileBody = JSON.parse(options.body);
                          const saved = {...profile, ...window.__profileBody};
                          return new Response(JSON.stringify(saved), {status:200, headers:{'Content-Type':'application/json'}});
                        }
                        throw new Error('unexpected fetch ' + url);
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("account-profile-controller.js")));
            page.waitForFunction("() => document.querySelector('[name=displayName]').value === '旧昵称'");
            page.locator("[name=displayName]").fill("新昵称");
            page.locator("#accountProfileFormV2 [type=submit]").click();
            page.waitForFunction("() => window.__profilePosts === 1");

            assertEquals(1, ((Number) page.evaluate("() => window.__profilePosts")).intValue());
            assertEquals("新昵称", page.evaluate("() => window.__profileBody.displayName"));
            assertEquals("/avatar/existing.png", page.evaluate("() => window.__profileBody.avatarUrl"));
        }
    }

    @Test
    void reducedMotionAndLayeringRemainAccessible() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.emulateMedia(new Page.EmulateMediaOptions().setReducedMotion(ReducedMotion.REDUCE));
            page.setContent("""
                    <header class="site-header">HEADER</header>
                    <div class="modal">MODAL</div>
                    <div class="toast-area">TOAST</div>
                    <div id="announcementBar" class="announcement">
                      <div class="announcement-ticker"><span>这是一条需要完整展示的长公告内容</span><span aria-hidden="true">副本</span></div>
                    </div>
                    <div class="account-settings-view" data-settings-view="security"><section class="nexus-module" id="password">P</section></div>
                    """);
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("site-surface-refine.css")));

            int headerZ = Integer.parseInt((String) page.evaluate("() => getComputedStyle(document.querySelector('.site-header')).zIndex"));
            int modalZ = Integer.parseInt((String) page.evaluate("() => getComputedStyle(document.querySelector('.modal')).zIndex"));
            int toastZ = Integer.parseInt((String) page.evaluate("() => getComputedStyle(document.querySelector('.toast-area')).zIndex"));
            assertTrue(modalZ > headerZ);
            assertTrue(toastZ > modalZ);
            assertEquals("normal", page.evaluate("() => getComputedStyle(document.querySelector('.announcement-ticker span:not([aria-hidden=true])')).whiteSpace"));
            assertEquals("none", page.evaluate("() => getComputedStyle(document.querySelector('.announcement-ticker span[aria-hidden=true]')).display"));
            assertEquals("88px", page.evaluate("() => getComputedStyle(document.querySelector('#password')).scrollMarginTop"));
        }
    }

    @Test
    void adminGuideCheckboxNeverOpensGuideAndTitleOpensOnce() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <meta name="_csrf" content="x"><meta name="_csrf_header" content="X-CSRF">
                    <span id="guideCount"></span><span id="guideSelectedCount"></span>
                    <div id="guideRows"></div><div id="guideEmpty"></div>
                    <input id="guideSelectAll" type="checkbox"><button id="batchDeleteGuides"></button>
                    <button id="archiveAllGuides"></button><button id="importGuides"></button><button id="reloadGuides"></button>
                    <button id="newGuide"></button><button id="archiveGuide"></button><button id="previewGuide"></button><button id="addSection"></button>
                    <form id="guideForm"><select name="gameCode"></select></form>
                    """);
            page.evaluate("""
                    () => {
                      window.__guideDetailFetches = 0;
                      const json = (value) => new Response(JSON.stringify(value), {status:200, headers:{'Content-Type':'application/json'}});
                      window.fetch = async (url) => {
                        if (url === '/api/admin/guides') return json([{id:1,title:'测试攻略',gameName:'MW4',status:'PUBLISHED',productCount:0}]);
                        if (url === '/api/admin/catalog') return json({games:[{code:'mw4',name:'MW4'}]});
                        if (url === '/api/admin/services') return json([]);
                        if (url === '/api/admin/guides/1') {
                          window.__guideDetailFetches += 1;
                          return new Promise(() => {});
                        }
                        throw new Error('unexpected fetch ' + url);
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-guides.js")));
            page.waitForSelector("[data-guide-check='1']");

            page.locator("[data-guide-check='1']").click();
            page.waitForTimeout(30);
            assertEquals(0, ((Number) page.evaluate("() => window.__guideDetailFetches")).intValue());

            page.locator(".guide-admin-row-main[data-guide-id='1']").click();
            page.waitForFunction("() => window.__guideDetailFetches === 1");
            assertEquals(1, ((Number) page.evaluate("() => window.__guideDetailFetches")).intValue());
        }
    }
}
