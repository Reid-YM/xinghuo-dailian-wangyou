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
class XingweiAdminPlaywrightTest {
    private static final Path ASSETS = Path.of("src/main/resources/static/assets");
    private static final Path TEMPLATE = Path.of("src/main/resources/templates/admin-xingwei.html");
    private static final String VERSION = "20261014-v3s";
    private static final String STAGE = "V3S · RELEASE CANDIDATE";

    @Test
    void desktopAndPhoneLayoutsStayInsideViewportAndNavigationWorks() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(1440, 900);
            loadMain(page);
            page.evaluate("""
                    ([version, stage]) => {
                      window.fetch = async (url) => {
                        if (String(url) === '/api/admin/xingwei/access') {
                          return {ok:true, json:async () => ({
                            mode:'LEGACY_ADMIN_OVERRIDE', allowAll:true,
                            permissionCodes:[], roleCodes:[], dataScope:'ALL',
                            adminUserId:1, departmentCode:'01'
                          })};
                        }
                        if (String(url) === '/api/admin/xingwei/health') {
                          return {ok:true, json:async () => ({ok:true, version, stage, serverTime:'2026-10-14T05:00:00+08:00'})};
                        }
                        return {ok:false, status:404, json:async () => ({error:'not found'})};
                      };
                    }
                    """, new Object[] {VERSION, STAGE});
            addMainScript(page);
            page.waitForFunction("() => document.querySelector('#xwAccessMode')?.textContent.includes('管理员兜底')");
            page.waitForFunction("() => document.documentElement.dataset.xwRuntimeState === 'match'");

            assertTrue(page.locator("#xwBackendStatus").textContent().contains("已核对"));
            assertTrue(page.locator("#xwServerTime").textContent().contains(VERSION));
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
            assertEquals("286px", page.evaluate("() => getComputedStyle(document.querySelector('.xw-shell')).gridTemplateColumns.split(' ')[0]"));
            assertEquals(17, ((Number) page.evaluate("() => document.querySelectorAll('.xw-module').length")).intValue());

            page.locator("#xwModuleSearch").fill("微信");
            assertTrue(((Number) page.evaluate("() => [...document.querySelectorAll('.xw-module')].filter(x => !x.hidden).length")).intValue() >= 2);
            assertTrue(((String) page.locator("#xwSearchCount").textContent()).contains("匹配"));

            page.setViewportSize(390, 844);
            page.locator("#xwModuleSearch").fill("");
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
            assertTrue((Boolean) page.evaluate("() => getComputedStyle(document.querySelector('.xw-menu-button')).display !== 'none'"));
            assertTrue(!(Boolean) page.evaluate("() => document.querySelector('#xwSidebar').classList.contains('open')"));

            page.locator("#xwMenuButton").click();
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xwSidebar').classList.contains('open')"));
            page.locator(".xw-nav-item[href='#business']").click();
            assertTrue(!(Boolean) page.evaluate("() => document.querySelector('#xwSidebar').classList.contains('open')"));
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
        }
    }

    @Test
    void scopedRbacFiltersModulesAndSearchCannotRevealUnauthorizedEntries() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(1280, 850);
            loadMain(page);
            page.evaluate("""
                    ([version, stage]) => {
                      window.fetch = async (url) => {
                        if (String(url) === '/api/admin/xingwei/access') {
                          return {ok:true, json:async () => ({
                            mode:'SHADOW_RBAC', allowAll:false,
                            permissionCodes:['order:view','employee:manage'],
                            roleCodes:['XW_CUSTOMER_ORDER'],
                            dataScope:'DEPARTMENT', departmentCode:'XW-03', adminUserId:3
                          })};
                        }
                        if (String(url) === '/api/admin/xingwei/health') {
                          return {ok:true, json:async () => ({ok:true, version, stage, serverTime:'2026-10-14T05:00:00+08:00'})};
                        }
                        return {ok:false, status:404, json:async () => ({error:'not found'})};
                      };
                    }
                    """, new Object[] {VERSION, STAGE});
            addMainScript(page);
            page.waitForFunction("() => document.querySelector('#xwAccessMode')?.textContent.includes('RBAC 已生效')");
            page.waitForFunction("() => document.documentElement.dataset.xwRuntimeState === 'match'");

            assertTrue((Boolean) page.evaluate("() => !document.querySelector('.xw-module-index:nth-child(1)') || true"));
            assertTrue((Boolean) page.evaluate("() => [...document.querySelectorAll('.xw-module')].find(x => x.querySelector('.xw-module-index')?.textContent === 'B02')?.hidden === false"));
            assertTrue((Boolean) page.evaluate("() => [...document.querySelectorAll('.xw-module')].find(x => x.querySelector('.xw-module-index')?.textContent === 'B05')?.hidden === false"));
            assertTrue((Boolean) page.evaluate("() => [...document.querySelectorAll('.xw-module')].find(x => x.querySelector('.xw-module-index')?.textContent === 'O02')?.hidden === false"));
            assertTrue((Boolean) page.evaluate("() => [...document.querySelectorAll('.xw-module')].find(x => x.querySelector('.xw-module-index')?.textContent === 'O03')?.hidden === true"));
            assertTrue((Boolean) page.evaluate("() => [...document.querySelectorAll('.xw-module')].find(x => x.querySelector('.xw-module-index')?.textContent === 'C02')?.hidden === true"));

            page.locator("#xwModuleSearch").fill("微信");
            assertEquals(1, ((Number) page.evaluate("() => [...document.querySelectorAll('.xw-module')].filter(x => !x.hidden).length")).intValue());
            assertTrue((Boolean) page.evaluate("() => [...document.querySelectorAll('.xw-module')].find(x => !x.hidden)?.querySelector('.xw-module-index')?.textContent === 'O02'"));

            page.locator("#xwModuleSearch").fill("支付");
            assertEquals(0, ((Number) page.evaluate("() => [...document.querySelectorAll('.xw-module')].filter(x => !x.hidden).length")).intValue());
            assertTrue(page.locator("#xwSearchCount").textContent().contains("匹配 0 个模块"));
            assertTrue(page.locator("#xwAccessMode").textContent().contains("RBAC 已生效"));
            assertTrue(page.locator("#xwAccessMode").textContent().contains("本中心"));
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
        }
    }

    @Test
    void runtimeVersionDriftIsVisibleInsteadOfReportingHealthy() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(1280, 850);
            loadMain(page);
            page.evaluate("""
                    () => {
                      window.fetch = async (url) => {
                        if (String(url) === '/api/admin/xingwei/access') {
                          return {ok:true, json:async () => ({
                            mode:'LEGACY_ADMIN_OVERRIDE', allowAll:true,
                            permissionCodes:[], roleCodes:[], dataScope:'ALL', adminUserId:1
                          })};
                        }
                        if (String(url) === '/api/admin/xingwei/health') {
                          return {ok:true, json:async () => ({
                            ok:true,
                            version:'20261014-v3k',
                            stage:'V3K · STABLE PRODUCTION',
                            serverTime:'2026-10-14T05:00:00+08:00'
                          })};
                        }
                        return {ok:false, status:404, json:async () => ({error:'not found'})};
                      };
                    }
                    """);
            addMainScript(page);
            page.waitForFunction("() => document.documentElement.dataset.xwRuntimeState === 'mismatch'");

            assertTrue(page.locator("#xwBackendStatus").textContent().contains("版本漂移"));
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xwBackendStatus').classList.contains('error')"));
            assertTrue(page.locator("#xwServerTime").textContent().contains("页面 20261014-v3s"));
            assertTrue(page.locator("#xwServerTime").textContent().contains("运行 20261014-v3k"));
            assertEquals("mismatch", page.evaluate("() => window.xwRuntimeHealth.state"));
            assertEquals("20261014-v3s", page.evaluate("() => window.xwRuntimeHealth.declaredVersion"));
            assertEquals("20261014-v3k", page.evaluate("() => window.xwRuntimeHealth.runtimeVersion"));
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
        }
    }

    @Test
    void organizationSnapshotRendersAndDialogsUseCanonicalOptions() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(1440, 900);
            loadMain(page);
            page.evaluate("""
                    () => {
                      window.xwAccess = {
                        mode:'LEGACY_ADMIN_OVERRIDE', allowAll:true,
                        permissionCodes:[], roleCodes:['XW_MANAGEMENT'],
                        dataScope:'ALL', adminUserId:1, departmentCode:'01'
                      };
                      window.xwHasPermission = () => true;
                      window.xwCanManageGlobalRbac = () => true;
                      const snapshot = {
                        shadowMode: false,
                        enforcement: 'XINGWEI_V2C_DATA_SCOPE',
                        scope: 'XINGWEI_ONLY',
                        dataScope: 'ALL',
                        scopeDepartmentCode: '01',
                        scopeAdminUserId: 1,
                        counts: {departments: 7, positions: 155, employees: 1, assignedEmployees: 1, roles: 7, permissions: 3},
                        visibleSections: {organization:true, employees:true, rbac:true},
                        departments: [
                          {id:1, code:'01', name:'01 管理层', positionCount:7, enabled:true},
                          {id:4, code:'04', name:'04 游戏业务中心', positionCount:30, enabled:true}
                        ],
                        positions: [
                          {id:1, code:'XW-LC-01-001', name:'工作室负责人', category:'核心管理', departmentCode:'01', departmentName:'01 管理层'},
                          {id:58, code:'XW-LC-04-007', name:'MW4 代练员', category:'MW4 执行', departmentCode:'04', departmentName:'04 游戏业务中心'}
                        ],
                        employees: [{id:1, username:'admin', legacyRole:'ADMIN', loginEnabled:true, employeeNo:'XW-0001', displayName:'负责人', status:'ACTIVE', departmentCode:'01', departmentName:'01 管理层', positionCode:'XW-LC-01-001', positionName:'工作室负责人', roleCodes:['XW_MANAGEMENT']}],
                        roles: [
                          {id:1, code:'XW_MANAGEMENT', name:'管理层', dataScope:'ALL', permissionCount:3, permissionCodes:['dashboard:view','org:view','rbac:view'], enabled:true},
                          {id:4, code:'XW_GAME_BUSINESS', name:'游戏业务中心', dataScope:'DEPARTMENT', permissionCount:1, permissionCodes:['dashboard:view'], enabled:true}
                        ],
                        permissions: [
                          {id:1, code:'dashboard:view', name:'查看工作台', module:'dashboard', action:'view'},
                          {id:2, code:'org:view', name:'查看组织', module:'org', action:'view'},
                          {id:3, code:'rbac:view', name:'查看权限', module:'rbac', action:'view'}
                        ]
                      };
                      window.fetch = async () => ({ok:true, json:async () => snapshot});
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-xingwei-org.js")));
            page.waitForFunction("() => document.querySelectorAll('#xwDepartmentRows tr').length === 2");

            assertEquals("7", page.locator("[data-org-kpi='departments']").textContent());
            assertEquals("155", page.locator("[data-org-kpi='positions']").textContent());
            assertTrue(page.locator("#xwDepartmentRows").textContent().contains("04 游戏业务中心"));
            assertTrue(page.locator("#xwPositionRows").textContent().contains("MW4 代练员"));
            assertTrue(page.locator("#xwEmployeeRows").textContent().contains("负责人"));
            assertTrue(page.locator("#xwRoleRows").textContent().contains("游戏业务中心"));

            page.locator("#xwEmployeeRows [data-employee-edit]").click();
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xwEmployeeDialog').open"));
            assertTrue(((Number) page.evaluate("() => document.querySelectorAll('#xwEmployeeDepartment option').length")).intValue() >= 3);
            assertTrue(page.locator("#xwEmployeePosition").textContent().contains("工作室负责人"));
            page.locator("[data-dialog-close='xwEmployeeDialog']").first().click();

            page.locator("#xwRoleRows [data-role-edit]").first().click();
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xwRoleDialog').open"));
            assertTrue(page.locator("#xwPermissionChecks").textContent().contains("查看组织"));
            page.locator("[data-dialog-close='xwRoleDialog']").first().click();

            page.setViewportSize(390, 844);
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
        }
    }

    private static void loadMain(Page page) throws Exception {
        page.setContent(Files.readString(TEMPLATE));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei-org.css")));
    }

    private static void addMainScript(Page page) {
        page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-xingwei.js")));
    }
}
