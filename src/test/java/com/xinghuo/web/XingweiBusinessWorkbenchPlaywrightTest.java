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

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class XingweiBusinessWorkbenchPlaywrightTest {
    private Browser chromium(Playwright p){var options=new com.microsoft.playwright.BrowserType.LaunchOptions();String executable=System.getenv("XINGHUO_TEST_CHROMIUM");if(executable!=null&&!executable.isBlank())options.setExecutablePath(Path.of(executable));return p.chromium().launch(options);}

    private static final Path ASSETS = Path.of("src/main/resources/static/assets");
    private static final Path TEMPLATE = Path.of("src/main/resources/templates/admin-xingwei-business.html");

    @Test
    void allScopeRendersQuotesSearchesAndAssignsWithCsrf() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = chromium(playwright)) {
            Page page = browser.newPage();
            page.setViewportSize(1440, 900);
            page.setContent(Files.readString(TEMPLATE));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei-org.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei-business.css")));
            page.evaluate("""
                    () => {
                      document.querySelector('meta[name="_csrf"]').setAttribute('content', 'csrf-test-token');
                      document.querySelector('meta[name="_csrf_header"]').setAttribute('content', 'X-CSRF-TOKEN');
                      const quoteSnapshot = {
                        enforcement:'XINGWEI_V2D_BUSINESS_SCOPE', dataScope:'ALL', departmentCode:'01', adminUserId:1,
                        visibleCount:2, unassignedVisible:true, unassignedCount:1,
                        items:[
                          {businessNo:'XH1001',game:'MW4',demand:'武器升级',currentProgress:'当前 20 级',platform:'PC',contactMethod:'WECHAT',customerContact:'wx-test',status:'NEW',createdAt:'2026-10-13T05:00:00Z',canAssign:true,ownership:{assigned:false}},
                          {businessNo:'XH1002',game:'MW4',demand:'排位陪练',platform:'PS5',contactMethod:'QQ',customerContact:'123456',status:'QUOTED',createdAt:'2026-10-13T04:00:00Z',canAssign:true,ownership:{assigned:true,ownerAdminUserId:2,ownerUsername:'agent',ownerDisplayName:'客服甲',departmentCode:'03',departmentName:'03 客服与订单中心'}}
                        ]
                      };
                      const orderSnapshot = {enforcement:'XINGWEI_V2D_BUSINESS_SCOPE',dataScope:'ALL',departmentCode:'01',adminUserId:1,visibleCount:1,unassignedVisible:true,unassignedCount:0,items:[{businessNo:'XH1002',orderNo:'XH1002',status:'IN_PROGRESS',progressPercent:35,progressText:'执行中',internalNote:'优先处理',updatedAt:'2026-10-13T05:10:00Z',canAssign:true,ownership:{assigned:true,ownerAdminUserId:2,ownerUsername:'agent',ownerDisplayName:'客服甲',departmentCode:'03',departmentName:'03 客服与订单中心'}}]};
                      const candidates = [
                        {adminUserId:2,username:'agent',displayName:'客服甲',departmentCode:'03',departmentName:'03 客服与订单中心',positionName:'售前客服'},
                        {adminUserId:3,username:'game',displayName:'代练甲',departmentCode:'04',departmentName:'04 游戏业务中心',positionName:'MW4 代练员'}
                      ];
                      window.fetch = async (url, options = {}) => {
                        const value = String(url);
                        if (value === '/api/admin/xingwei/access') return {ok:true,json:async()=>({mode:'LEGACY_ADMIN_OVERRIDE',allowAll:true,permissionCodes:[],roleCodes:['XW_MANAGEMENT'],dataScope:'ALL',departmentCode:'01',adminUserId:1})};
                        if (value === '/api/admin/xingwei/business/quotes') return {ok:true,json:async()=>quoteSnapshot};
                        if (value === '/api/admin/xingwei/business/orders') return {ok:true,json:async()=>orderSnapshot};
                        if (value === '/api/admin/xingwei/business/orders/exceptions') return {ok:true,json:async()=>({
                          ...orderSnapshot, visibleCount:1,
                          items:[{businessNo:'XH0999',orderNo:'XH0999',status:'EXCEPTION',progressText:'客户争议处理中',customerNote:'售后问题：进度不符',internalNote:'客服已联系',updatedAt:'2026-10-12T05:10:00Z',canAssign:true,ownership:{assigned:true,ownerAdminUserId:2,ownerUsername:'agent',departmentCode:'03'}}]
                        })};
                        if (value.startsWith('/api/admin/xingwei/business/orders/after-sales?')) {
                          const params = new URLSearchParams(value.split('?')[1]);
                          const page = Number(params.get('page'));
                          const query = params.get('q') || '';
                          const category = params.get('category') || '';
                          (window.__afterSalesRequests ||= []).push({page, query, category});
                          const first = {businessNo:'XH0888',orderNo:'XH0888',status:'IN_PROGRESS',progressText:'核对中',customerNote:'[售后问题 · 交付或进度 · 2026-10-14T05:00:00Z]\\n未按约定完成 <script>alert(1)</script>',internalNote:'客服跟进中',updatedAt:'2026-10-14T05:10:00Z',canAssign:true,ownership:{assigned:true,ownerAdminUserId:2,ownerUsername:'agent',departmentCode:'03'}};
                          const older = {businessNo:'XH0777',orderNo:'XH0777',status:'IN_PROGRESS',progressText:'待回访',customerNote:'[售后问题 · 旧单]\\n旧单追踪',updatedAt:'2026-09-01T05:10:00Z',canAssign:true,ownership:{assigned:true,ownerAdminUserId:2,ownerUsername:'agent',departmentCode:'03'}};
                          const refund = {businessNo:'XH0666',orderNo:'XH0666',status:'IN_PROGRESS',customerNote:'[售后问题 · 退款核对 · 2026-10-14T06:00:00Z]\\n请核对',updatedAt:'2026-10-14T06:10:00Z',canAssign:true,ownership:{assigned:true,ownerAdminUserId:2,ownerUsername:'agent',departmentCode:'03'}};
                          const items = category === 'REFUND_REVIEW' ? [refund] : query ? (query === '旧单' ? [older] : []) : page === 0 ? [first] : [older];
                          return {ok:true,json:async()=>({...orderSnapshot, visibleCount:items.length, totalCount:category || query ? items.length : 51, page, pageSize:50, hasNext:!category && !query && page === 0, items})};
                        }
                        if (value === '/api/admin/xingwei/business/ownership-candidates') return {ok:true,json:async()=>candidates};
                        if (value.includes('/ownership') && String(options.method).toUpperCase() === 'PUT') {
                          window.__lastOwnershipBody = JSON.parse(options.body);
                          window.__lastCsrf = options.headers['X-CSRF-TOKEN'];
                          return {ok:true,json:async()=>({assigned:true})};
                        }
                        return {ok:false,status:404,json:async()=>({error:'not found'})};
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-xingwei-business.js")));
            page.waitForFunction("() => document.querySelector('#xwbQuoteRows')?.textContent.includes('XH1001')");

            assertEquals(2, page.locator("#xwbQuoteRows tr[data-search]").count());
            assertTrue(page.locator("#xwbAccessBadge").textContent().contains("管理员兜底"));
            assertEquals("2", page.locator("#xwbKpiVisible").textContent());
            assertEquals("1", page.locator("#xwbKpiUnassigned").textContent());

            page.locator("#xwbSearch").fill("排位陪练");
            assertEquals("1", page.locator("#xwbKpiFiltered").textContent());
            assertTrue((Boolean) page.evaluate("() => [...document.querySelectorAll('#xwbQuoteRows tr[data-search]')].filter(x => !x.hidden).length === 1"));
            page.locator("#xwbSearch").fill("");

            page.locator("#xwbQuoteRows .xw-mini-button").first().click();
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xwbAssignDialog').open"));
            page.locator("#xwbAssignTarget").selectOption("owner:2");
            page.locator("#xwbAssignForm button[type='submit']").click();
            page.waitForFunction("() => window.__lastOwnershipBody?.ownerAdminUserId === 2");
            assertEquals("csrf-test-token", page.evaluate("() => window.__lastCsrf"));
            assertFalse((Boolean) page.evaluate("() => document.querySelector('#xwbAssignDialog').open"));

            page.locator("[data-tab='orders']").click();
            page.waitForFunction("() => document.querySelector('#xwbOrderRows')?.textContent.includes('XH1002')");
            assertEquals("35%", page.locator("#xwbOrderRows .xwb-progress .xwb-primary").textContent());

            page.locator("[data-tab='exceptions']").click();
            page.waitForFunction("() => document.querySelector('#xwbOrderRows')?.textContent.includes('XH0999')");
            assertEquals("异常订单", page.locator("#xwbOrderHeading").textContent());
            assertEquals(1, page.locator("#xwbOrderRows tr[data-search]").count());
            assertFalse(page.locator("#xwbOrderRows").textContent().contains("XH1002"));
            assertTrue(page.locator("#xwbOrderRows").textContent().contains("客户说明：售后问题：进度不符"));
            assertTrue(page.locator("#xwbOrderRows").textContent().contains("内部备注：客服已联系"));
            page.locator("#xwbSearch").fill("客户争议");
            assertEquals("1", page.locator("#xwbKpiFiltered").textContent());

            page.locator("#xwbSearch").fill("");
            page.locator("[data-tab='after-sales']").click();
            page.waitForFunction("() => document.querySelector('#xwbOrderRows')?.textContent.includes('XH0888')");
            assertEquals("售后问题记录", page.locator("#xwbOrderHeading").textContent());
            assertEquals(1, page.locator("#xwbOrderRows tr[data-search]").count());
            assertTrue(page.locator("#xwbOrderDescription").textContent().contains("不代表退款申请或处理状态"));
            assertTrue(page.locator("#xwbOrderRows").textContent().contains("未按约定完成 <script>alert(1)</script>"));
            assertEquals(0, page.locator("#xwbOrderRows script").count());
            page.locator("#xwbOrderRows .xw-mini-button").first().click();
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xwbAfterSalesDetailDialog').open"));
            assertTrue(page.locator("#xwbAfterSalesDetailMeta").textContent().contains("XH0888"));
            assertTrue(page.locator("#xwbAfterSalesDetailNote").textContent().contains("未按约定完成 <script>alert(1)</script>"));
            assertEquals(0, page.locator("#xwbAfterSalesDetailDialog script").count());
            page.locator("#xwbAfterSalesDetailClose").click();
            assertFalse((Boolean) page.evaluate("() => document.querySelector('#xwbAfterSalesDetailDialog').open"));
            assertEquals("51", page.locator("#xwbKpiVisible").textContent());
            assertEquals("第 1 页 · 共 51 条", page.locator("#xwbAfterSalesPageText").textContent());
            page.locator("#xwbAfterSalesNext").click();
            page.waitForFunction("() => document.querySelector('#xwbOrderRows')?.textContent.includes('XH0777')");
            assertEquals("第 2 页 · 共 51 条", page.locator("#xwbAfterSalesPageText").textContent());
            page.locator("#xwbSearch").fill("旧单");
            page.waitForFunction("() => document.querySelector('#xwbAfterSalesPageText')?.textContent.includes('共 1 条')");
            assertEquals("1", page.locator("#xwbKpiVisible").textContent());
            assertEquals("1", page.locator("#xwbKpiFiltered").textContent());
            assertTrue((Boolean) page.evaluate("() => window.__afterSalesRequests.some(x => x.page === 1 && !x.query) && window.__afterSalesRequests.some(x => x.page === 0 && x.query === '旧单')"));

            page.locator("#xwbSearch").fill("");
            page.waitForFunction("() => document.querySelector('#xwbAfterSalesPageText')?.textContent.includes('共 51 条')");
            page.locator("#xwbAfterSalesCategory").selectOption("REFUND_REVIEW");
            page.waitForFunction("() => document.querySelector('#xwbOrderRows')?.textContent.includes('XH0666')");
            assertEquals("第 1 页 · 共 1 条", page.locator("#xwbAfterSalesPageText").textContent());
            assertTrue((Boolean) page.evaluate("() => window.__afterSalesRequests.some(x => x.page === 0 && x.category === 'REFUND_REVIEW' && !x.query)"));
            assertFalse(page.locator("#xwbAfterSalesCategoryLabel").isHidden());

            page.setViewportSize(390, 844);
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xwbOrdersPanel .xw-table-wrap').scrollWidth > document.querySelector('#xwbOrdersPanel .xw-table-wrap').clientWidth"));
            page.locator("#xwbOrderRows .xw-mini-button").first().click();
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xwbAfterSalesDetailDialog').getBoundingClientRect().right <= innerWidth + 1"));
            assertTrue(page.locator("#xwbAfterSalesDetailNote").textContent().contains("请核对"));
        }
    }

    @Test
    void departmentReadOnlyOperatorCannotSeeQuoteTabOrAssignmentAction() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = chromium(playwright)) {
            Page page = browser.newPage();
            page.setViewportSize(1280, 820);
            page.setContent(Files.readString(TEMPLATE));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei-org.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei-business.css")));
            page.evaluate("""
                    () => {
                      window.fetch = async (url) => {
                        const value = String(url);
                        if (value === '/api/admin/xingwei/access') return {ok:true,json:async()=>({mode:'SHADOW_RBAC',allowAll:false,permissionCodes:['order:view'],roleCodes:['XW_CUSTOMER_ORDER'],dataScope:'DEPARTMENT',departmentCode:'03',adminUserId:9})};
                        if (value === '/api/admin/xingwei/business/orders') return {ok:true,json:async()=>({dataScope:'DEPARTMENT',departmentCode:'03',adminUserId:9,visibleCount:1,unassignedVisible:false,unassignedCount:0,items:[{businessNo:'XH2001',orderNo:'XH2001',status:'IN_PROGRESS',progressPercent:10,progressText:'已接单',updatedAt:'2026-10-13T05:00:00Z',canAssign:false,ownership:{assigned:true,ownerAdminUserId:8,ownerUsername:'peer',ownerDisplayName:'同中心员工',departmentCode:'03',departmentName:'03 客服与订单中心'}}]})};
                        if (value.startsWith('/api/admin/xingwei/business/orders/after-sales?')) return {ok:true,json:async()=>({dataScope:'DEPARTMENT',departmentCode:'03',adminUserId:9,visibleCount:1,totalCount:1,page:0,pageSize:50,hasNext:false,unassignedVisible:false,unassignedCount:0,items:[{businessNo:'XH2001',orderNo:'XH2001',status:'IN_PROGRESS',customerNote:'[售后问题 · 其他问题] 需要回访',updatedAt:'2026-10-13T05:00:00Z',canAssign:false,ownership:{assigned:true,departmentCode:'03'}}]})};
                        return {ok:false,status:404,json:async()=>({error:'not found'})};
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-xingwei-business.js")));
            page.waitForFunction("() => document.querySelector('#xwbOrderRows')?.textContent.includes('XH2001')");

            assertTrue((Boolean) page.evaluate("() => document.querySelector('[data-tab=quotes]').hidden"));
            assertFalse((Boolean) page.evaluate("() => document.querySelector('[data-tab=orders]').hidden"));
            assertFalse((Boolean) page.evaluate("() => document.querySelector('[data-tab=exceptions]').hidden"));
            assertFalse((Boolean) page.evaluate("() => document.querySelector('[data-tab=after-sales]').hidden"));
            assertFalse((Boolean) page.evaluate("() => document.querySelector('#xwbOrdersPanel').hidden"));
            assertEquals(0, page.locator("#xwbOrderRows .xw-mini-button").count());
            page.locator("[data-tab='after-sales']").click();
            page.waitForFunction("() => document.querySelector('#xwbOrderRows')?.textContent.includes('需要回访')");
            assertEquals(1, page.locator("#xwbOrderRows .xw-mini-button").count());
            page.locator("#xwbOrderRows .xw-mini-button").click();
            assertTrue(page.locator("#xwbAfterSalesDetailNote").textContent().contains("需要回访"));
            assertEquals(0, page.locator("#xwbAssignDialog[open]").count());
            assertTrue(page.locator("#xwbScopeText").textContent().contains("本中心"));
            assertEquals("不可见", page.locator("#xwbKpiUnassigned").textContent());
        }
    }
}
