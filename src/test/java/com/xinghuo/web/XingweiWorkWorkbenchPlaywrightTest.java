package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class XingweiWorkWorkbenchPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");
    private static final Path TEMPLATE = Path.of("src/main/resources/templates/admin-xingwei-work.html");

    @Test
    void departmentQueueClaimAndOwnFulfillmentWorkInChromiumAndWebKit() throws Exception {
        try (Playwright playwright = Playwright.create()) {
            runDepartmentScenario(playwright.chromium());
            runDepartmentScenario(playwright.webkit());
        }
    }

    private void runDepartmentScenario(BrowserType browserType) throws Exception {
        try (Browser browser = browserType.launch()) {
            Page page = browser.newPage();
            page.setViewportSize(1440, 900);
            page.setContent(Files.readString(TEMPLATE));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei-org.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei-business.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei-work.css")));
            page.evaluate("""
                    () => {
                      document.querySelector('meta[name="_csrf"]').setAttribute('content', 'csrf-v2f-token');
                      document.querySelector('meta[name="_csrf_header"]').setAttribute('content', 'X-CSRF-TOKEN');
                      window.__claimed = false;
                      window.__workSnapshot = () => ({
                        enforcement:'XINGWEI_V2F_WORKBENCH', dataScope:'DEPARTMENT', departmentCode:'03', adminUserId:9,
                        visibleCount:2, mineCount:window.__claimed ? 2 : 1, queueCount:window.__claimed ? 0 : 1, unassignedCount:0,
                        items:[
                          {businessNo:'XHWORK01',orderNo:'XHWORK01',game:'MW4',demand:'武器升级',expectedTime:'今晚',status:'IN_PROGRESS',progressPercent:35,progressText:'执行中',customerNote:'客户备注',internalNote:'内部备注',updatedAt:'2026-10-13T07:00:00Z',version:3,bucket:'MINE',canClaim:false,canUpdate:true,ownership:{assigned:true,ownerAdminUserId:9,departmentCode:'03',departmentName:'03 客服与订单中心'}},
                          {businessNo:'XHQUEUE01',orderNo:'XHQUEUE01',game:'MW4',demand:'排位陪练',expectedTime:'今天',status:'WAITING_CONFIRMATION',progressPercent:null,progressText:'待领取',updatedAt:'2026-10-13T06:00:00Z',version:0,bucket:window.__claimed?'MINE':'DEPARTMENT_QUEUE',canClaim:!window.__claimed,canUpdate:window.__claimed,ownership:{assigned:true,ownerAdminUserId:window.__claimed?9:null,departmentCode:'03',departmentName:'03 客服与订单中心'}}
                        ]
                      });
                      window.fetch = async (url, options = {}) => {
                        const value = String(url);
                        if (value === '/api/admin/xingwei/work' && (!options.method || options.method === 'GET')) {
                          return {ok:true,json:async()=>window.__workSnapshot()};
                        }
                        if (value === '/api/admin/xingwei/work/orders/XHQUEUE01/claim' && String(options.method).toUpperCase() === 'PUT') {
                          window.__claimCsrf = options.headers['X-CSRF-TOKEN'];
                          window.__claimed = true;
                          return {ok:true,json:async()=>({assigned:true,ownerAdminUserId:9,departmentCode:'03'})};
                        }
                        if (value === '/api/admin/xingwei/work/orders/XHWORK01' && String(options.method).toUpperCase() === 'PATCH') {
                          window.__updateCsrf = options.headers['X-CSRF-TOKEN'];
                          window.__updateBody = JSON.parse(options.body);
                          return {ok:true,json:async()=>({businessNo:'XHWORK01',status:'IN_PROGRESS',progressPercent:60,progressText:'已完成 60%',version:4})};
                        }
                        return {ok:false,status:404,json:async()=>({error:'not found'})};
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-xingwei-work.js")));
            page.waitForFunction("() => document.querySelector('#xwwTaskRows')?.textContent.includes('XHWORK01')");

            assertEquals(2, page.locator("#xwwTaskRows tr[data-bucket]").count());
            assertEquals("1", page.locator("#xwwKpiMine").textContent());
            assertEquals("1", page.locator("#xwwKpiQueue").textContent());
            assertEquals("不可见", page.locator("#xwwKpiUnassigned").textContent());
            assertTrue((Boolean) page.evaluate("() => document.querySelector('[data-filter=UNASSIGNED]').hidden"));

            page.locator("[data-filter='DEPARTMENT_QUEUE']").click();
            assertEquals(1, page.locator("#xwwTaskRows tr[data-bucket]:visible").count());
            page.locator("#xwwTaskRows .xw-mini-button").filter(new com.microsoft.playwright.Locator.FilterOptions().setHasText("领取任务")).click();
            page.waitForFunction("() => window.__claimed === true && document.querySelector('#xwwKpiMine')?.textContent === '2'");
            assertEquals("csrf-v2f-token", page.evaluate("() => window.__claimCsrf"));

            page.locator("[data-filter='MINE']").click();
            page.locator("#xwwSearch").fill("武器升级");
            assertEquals(1, page.locator("#xwwTaskRows tr[data-bucket]:visible").count());
            page.locator("#xwwTaskRows tr[data-bucket]:visible .xw-mini-button").filter(new com.microsoft.playwright.Locator.FilterOptions().setHasText("更新履约")).click();
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xwwEditDialog').open"));
            assertFalse(page.locator("#xwwEditDialog").innerText().contains("当前版本"));
            assertEquals(0,page.locator("#xwwEditInternalNote").count());
            page.locator("#xwwEditPercent").fill("60");
            page.locator("#xwwEditProgress").fill("已完成 60%");
            page.locator("#xwwEditReason").fill("更新当前代练进度");
            page.locator("#xwwEditForm button[type='submit']").click();
            page.waitForFunction("() => window.__updateBody?.progressPercent === 60");
            assertEquals("csrf-v2f-token", page.evaluate("() => window.__updateCsrf"));
            assertEquals(3, ((Number) page.evaluate("() => window.__updateBody.version")).intValue());
            assertEquals("IN_PROGRESS", page.evaluate("() => window.__updateBody.status"));
            assertFalse((Boolean) page.evaluate("() => document.querySelector('#xwwEditDialog').open"));

            page.locator("#xwwSearch").fill("");
            page.setViewportSize(390, 844);
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xwwTaskPanel .xw-table-wrap').scrollWidth > document.querySelector('#xwwTaskPanel .xw-table-wrap').clientWidth"));
        }
    }
}
