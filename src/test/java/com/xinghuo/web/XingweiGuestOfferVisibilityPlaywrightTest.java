package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Catches quote button regressions when JS bundles are updated independently. */
@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class XingweiGuestOfferVisibilityPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");
    private static final Path TEMPLATE = Path.of("src/main/resources/templates/admin-xingwei-business.html");

    @Test
    void quoteActionsStayVisibleForBothEligibleAndIneligibleRows() throws Exception {
        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            String template = Files.readString(TEMPLATE);
            assertTrue(template.contains("admin-xingwei-business.js?v=20261018offerhotfix1"));
            assertTrue(template.contains("admin-xingwei-guest-offer.js?v=20261018offerhotfix1"));
            page.setContent(template);
            page.evaluate("""
                    () => {
                      window.fetch = async (url) => {
                        const path = String(url);
                        if (path === '/api/admin/xingwei/access') {
                          return {ok:true,json:async()=>({
                            mode:'LEGACY_ADMIN_OVERRIDE',allowAll:true,
                            permissionCodes:[],dataScope:'ALL',adminUserId:1
                          })};
                        }
                        if (path === '/api/admin/xingwei/business/quotes') {
                          return {ok:true,json:async()=>({
                            dataScope:'ALL',visibleCount:3,unassignedVisible:true,unassignedCount:3,
                            items:[
                              {businessNo:'XHGUEST10001',game:'MW4',demand:'游客需求',
                               status:'NEW',customerType:'GUEST',canOffer:true,
                               canAssign:true,ownership:{assigned:false}},
                              {businessNo:'XHACCOUNT001',game:'MW4',demand:'会员需求',
                               status:'NEW',customerType:'ACCOUNT',canOffer:false,
                               canAssign:true,ownership:{assigned:false}},
                              {businessNo:'XHCLOSED0001',game:'MW4',demand:'已关闭需求',
                               status:'CLOSED',customerType:'GUEST',canOffer:false,
                               canAssign:true,ownership:{assigned:false}}
                            ]
                          })};
                        }
                        return {ok:false,status:404,json:async()=>({error:'not found'})};
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions()
                    .setPath(ASSETS.resolve("admin-xingwei-business.js")));
            page.addScriptTag(new Page.AddScriptTagOptions()
                    .setPath(ASSETS.resolve("admin-xingwei-guest-offer.js")));
            page.waitForFunction("""
                    () => document.querySelectorAll('#xwbQuoteRows button').length >= 9
                    """);
            assertEquals(3, page.locator("#xwbQuoteRows button:has-text('发布报价')").count());
            assertTrue(page.locator("#xwbQuoteRows tr").nth(0)
                    .locator("button:has-text('发布报价')").isEnabled());
            assertFalse(page.locator("#xwbQuoteRows tr").nth(1)
                    .locator("button:has-text('发布报价')").isEnabled());
            assertFalse(page.locator("#xwbQuoteRows tr").nth(2)
                    .locator("button:has-text('发布报价')").isEnabled());
            assertTrue(page.locator("#xwbQuoteRows tr").nth(1)
                    .textContent().contains("注册账号询价"));
            assertTrue(page.locator("#xwbQuoteRows tr").nth(2)
                    .textContent().contains("不能再发布游客报价"));
        }
    }

    @Test
    void staleQuoteListBundleNeverMakesOfferControlDisappear() throws Exception {
        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("<table><tbody id='xwbQuoteRows'><tr data-search='old'>"
                    + "<td><span class='xwb-primary'>XHOLD1000001</span></td>"
                    + "<td><button type='button'>原操作</button></td></tr></tbody></table>");
            page.addScriptTag(new Page.AddScriptTagOptions()
                    .setPath(ASSETS.resolve("admin-xingwei-guest-offer.js")));
            assertEquals(1, page.locator("button:has-text('发布报价')").count());
            assertFalse(page.locator("button:has-text('发布报价')").isEnabled());
            assertTrue(page.locator("#xwbQuoteRows").textContent().contains("报价权限未同步"));
        }
    }
}
