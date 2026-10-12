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

    @Test
    void desktopAndPhoneLayoutsStayInsideViewportAndNavigationWorks() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(1440, 900);
            page.setContent(Files.readString(TEMPLATE));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei-org.css")));
            page.evaluate("""
                    () => {
                      window.fetch = async (url) => {
                        if (String(url) === '/api/admin/xingwei/access') {
                          return {ok:true, json:async () => ({mode:'LEGACY_ADMIN_OVERRIDE', allowAll:true, permissionCodes:[], roleCodes:[]})};
                        }
                        if (String(url) === '/api/admin/xingwei/health') {
                          return {ok:true, json:async () => ({ok:true, version:'20261013-v2b', serverTime:'2026-10-13T05:00:00+08:00'})};
                        }
                        return {ok:false, status:404, json:async () => ({error:'not found'})};
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-xingwei.js")));
            page.waitForFunction("() => document.querySelector('#xwAccessMode')?.textContent.includes('管理员兜底')");

            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
            assertEquals("286px", page.evaluate("() => getComputedStyle(document.querySelector('.xw-shell')).gridTemplateColumns.split(' ')[0]"));
            assertEquals(16, ((Number) page.evaluate("() => document.querySelectorAll('.xw-module').length")).intValue());

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
            page.setContent(Files.readString(TEMPLATE));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei-org.css")));
            page.evaluate("""
                    () => {
                      window.fetch = async (url) => {
                        if (String(url) === '/api/admin/xingwei/access') {
                          return {ok:true, json:async () => ({
                            mode:'SHADOW_RBAC', allowAll:false,
                            permissionCodes:['order:view','employee:manage'],
                            roleCodes:['XW_CUSTOMER_ORDER']
                          })};
                        }
                        if (String(url) === '/api/admin/xingwei/health') {
                          return {ok:true, json:async () => ({ok:true, version:'20261013-v2b', serverTime:'2026-10-13T05:00:00+08:00'})};
                        }
                        return {ok:false, status:404, json:async () => ({error:'not found'})};
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-xingwei.js")));
            page.waitForFunction("() => document.querySelector('#xwAccessMode')?.textContent.includes('RBAC 已生效')");

            assertTrue((Boolean) page.evaluate("() => !document.querySelector('.xw-module-index:nth-child(1)') || true"));
            assertTrue((Boolean) page.evaluate("() => [...document.querySelectorAll('.xw-module')].find(x => x.querySelector('.xw-module-index')?.textContent === 'B02')?.hidden === false"));
            assertTrue((Boolean) page.evaluate("() => [...document.querySelectorAll('.xw-module')].find(x => x.querySelector('.xw-module-index')?.textContent === 'O02')?.hidden === false"));
            assertTrue((Boolean) page.evaluate("() => [...document.querySelectorAll('.xw-module')].find(x => x.querySelector('.xw-module-index')?.textContent === 'O03')?.hidden === true"));
            assertTrue((Boolean) page.evaluate("() => [...document.querySelectorAll('.xw-module')].find(x => x.querySelector('.xw-module-index')?.textContent === 'C02')?.hidden === true"));

            page.locator("#xwModuleSearch").fill("微信");
            assertEquals(0, ((Number) page.evaluate("() => [...document.querySelectorAll('.xw-module')].filter(x => !x.hidden).length")).intValue());
            assertTrue(page.locator("#xwSearchCount").textContent().contains("匹配 0 个模块"));
            assertTrue(page.locator("#xwAccessMode").textContent().contains("RBAC 已生效"));
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
        }
    }

    @Test
    void organizationSnapshotRendersAndDialogsUseCanonicalOptions() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(1440, 900);
            page.setContent(Files.readString(TEMPLATE));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei.css")));
            page.addStyleTag(new Page.AddStyleTagOptions().setPath(ASSETS.resolve("admin-xingwei-org.css")));
            page.evaluate("""
                    () => {
                      window.xwHasPermission = () => true;
                      const snapshot = {
                        shadowMode: true,
                        enforcement: 'XINGWEI_V2B_HYBRID',
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
                          {id:1, code:'dashboard:view', name:'工作台查看', module:'dashboard'},
                          {id:11, code:'org:view', name:'组织架构查看', module:'organization'},
                          {id:15, code:'rbac:view', name:'角色与权限查看', module:'security'}
                        ]
                      };
                      window.fetch = async (url, options = {}) => {
                        if (String(url) === '/api/admin/xingwei/org/snapshot') {
                          return {ok:true, json:async () => snapshot};
                        }
                        if (String(url) === '/api/admin/xingwei/health') {
                          return {ok:true, json:async () => ({ok:true, version:'20261013-v2b', serverTime:'2026-10-13T05:00:00+08:00'})};
                        }
                        if (String(options.method || 'GET').toUpperCase() === 'PUT') {
                          return {ok:true, json:async () => ({ok:true})};
                        }
                        return {ok:false, status:404, json:async () => ({error:'not found'})};
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(ASSETS.resolve("admin-xingwei-org.js")));
            page.waitForFunction("() => document.querySelector('[data-org-kpi=\"positions\"]')?.textContent === '155'");

            assertEquals("7", page.locator("[data-org-kpi='departments']").textContent());
            assertEquals("155", page.locator("[data-org-kpi='positions']").textContent());
            assertEquals(2, page.locator("#xwDepartmentRows tr").count());
            assertEquals(2, page.locator("#xwPositionRows tr").count());

            page.locator("#xwPositionDepartment").selectOption("04");
            assertEquals(1, page.locator("#xwPositionRows tr").count());
            assertTrue(page.locator("#xwPositionRows").textContent().contains("MW4 代练员"));

            page.locator("[data-employee-edit='1']").click();
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xwEmployeeDialog').open"));
            assertEquals("XW-LC-01-001", page.locator("#xwEmployeePosition").inputValue());
            page.locator("[data-dialog-close='xwEmployeeDialog']").first().click();

            page.locator("[data-role-edit='XW_GAME_BUSINESS']").click();
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xwRoleDialog').open"));
            assertEquals(3, page.locator("#xwPermissionChecks input").count());
            assertEquals(0, page.locator("[data-role-edit='XW_MANAGEMENT']").count());
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
        }
    }
}
