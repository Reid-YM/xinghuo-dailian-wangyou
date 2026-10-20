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
class XingweiServiceCatalogWorkbenchPlaywrightTest {
    private static final Path MAIN = Path.of("src/main/resources");

    @Test
    void viewerRendersFiltersWithoutCreateOrWriteControlsAndStaysInsidePhoneViewport() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(1440, 900);
            loadPage(page);
            page.evaluate("""
                    () => { window.fetch = async (url) => {
                      if (String(url) === '/api/admin/xingwei/access') return {ok:true, json:async () => ({allowAll:false,permissionCodes:['service:view'],mode:'SHADOW_RBAC'})};
                      if (String(url) === '/api/admin/xingwei/catalog/snapshot') return {ok:true, json:async () => ({
                        source:'EXISTING_SERVICE_CATALOG',writeScope:'EXISTING_SERVICE_CREATE_UPDATE',
                        counts:{services:2,activeServices:1,games:2,categories:2,platforms:2},
                        services:[
                          {id:1,slug:'weapon-leveling',gameCode:'MW4',name:'武器升级',category:'LEVELING',shortDesc:'手打升级',priceMode:'FIXED',priceFixed:88,durationText:'1-2天',status:'AVAILABLE',coverImage:null,hotScore:10,manualPriority:10,expiresAt:null,active:true,sortOrder:1,version:3},
                          {id:2,slug:'rank-service',gameCode:'OTHER',name:'排位服务',category:'RANK',shortDesc:'测试',priceMode:'RANGE',priceMin:100,priceMax:200,durationText:'咨询',status:'OFFLINE',coverImage:null,hotScore:0,manualPriority:0,expiresAt:null,active:false,sortOrder:2,version:1}
                        ],
                        games:[
                          {code:'MW4',name:'使命召唤现代战争4',businessStatus:'ACTIVE',active:true,categoryCount:1,serviceCount:1,platformCodes:['PC']},
                          {code:'OTHER',name:'其他游戏',businessStatus:'PLANNED',active:true,categoryCount:1,serviceCount:1,platformCodes:['PC','PS5']}
                        ],
                        categories:[{code:'LEVELING',name:'升级',gameCode:'MW4',active:true,serviceCount:1},{code:'RANK',name:'排位',gameCode:'OTHER',active:true,serviceCount:1}], platforms:[]
                      })};
                      return {ok:false,status:404,json:async()=>({error:'not found'})};
                    }; };
                    """);
            addScript(page);
            page.waitForFunction("() => document.querySelectorAll('#xcatServiceRows tr').length === 2");
            assertEquals("2", page.locator("#xcatServiceCount").textContent());
            assertTrue(page.locator("#xcatServiceRows").textContent().contains("武器升级"));
            assertTrue(page.locator("#xcatGameRows").textContent().contains("使命召唤现代战争4"));
            assertTrue(page.locator("#xcatServiceRows").innerText().contains("升级"));
            assertFalse(page.locator("#xcatServiceRows").innerText().contains("LEVELING"));
            assertFalse(page.locator("#xcatServiceRows").innerText().contains("v3"));
            assertFalse(page.locator("#xcatCategoryRows").innerText().contains("RANK"));
            assertFalse(page.locator("#xcatGameFilter").innerText().contains("MW4"));
            assertEquals(0, page.locator("button[data-action='edit']").count());
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xcatCreateOpen').hidden"));
            assertTrue(page.locator("#xcatManageMode").textContent().contains("只读"));

            page.locator("#xcatSearch").fill("排位");
            assertEquals(1, ((Number) page.evaluate("() => document.querySelectorAll('#xcatServiceRows tr').length")).intValue());
            page.locator("#xcatSearch").fill("");
            page.locator("#xcatStateFilter").selectOption("inactive");
            assertEquals(1, ((Number) page.evaluate("() => document.querySelectorAll('#xcatServiceRows tr').length")).intValue());

            page.setViewportSize(390, 844);
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
        }
    }

    @Test
    void managerCanCreateOfflineServiceWithExistingTaxonomyAndCsrf() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(1280, 850);
            loadPage(page);
            page.evaluate("""
                    () => {
                      document.querySelector('meta[name="_csrf"]').setAttribute('content','csrf-token');
                      document.querySelector('meta[name="_csrf_header"]').setAttribute('content','X-CSRF-TOKEN');
                      window.__writes=[];
                      const snapshot={
                        source:'EXISTING_SERVICE_CATALOG',writeScope:'EXISTING_SERVICE_CREATE_UPDATE',
                        counts:{services:1,activeServices:1,games:2,categories:3,platforms:1},
                        services:[{id:1,slug:'weapon-leveling',gameCode:'MW4',name:'武器升级',category:'LEVELING',shortDesc:'手打升级',priceMode:'FIXED',priceFixed:88,priceMin:null,priceMax:null,durationText:'1-2天',status:'AVAILABLE',coverImage:null,hotScore:0,manualPriority:10,expiresAt:null,active:true,sortOrder:1,version:3}],
                        games:[
                          {code:'MW4',name:'使命召唤现代战争4',businessStatus:'ACTIVE',active:true,categoryCount:2,serviceCount:1,platformCodes:['PC']},
                          {code:'DISABLED',name:'停用游戏',businessStatus:'PAUSED',active:false,categoryCount:1,serviceCount:0,platformCodes:[]}
                        ],
                        categories:[
                          {code:'LEVELING',name:'升级',gameCode:'MW4',active:true,serviceCount:1},
                          {code:'RANK',name:'排位',gameCode:'MW4',active:true,serviceCount:0},
                          {code:'OLD',name:'旧分类',gameCode:'DISABLED',active:true,serviceCount:0}
                        ],platforms:[]
                      };
                      window.fetch=async(url,options={})=>{
                        const value=String(url);
                        if(value==='/api/admin/xingwei/access')return {ok:true,json:async()=>({allowAll:false,permissionCodes:['service:view','service:manage'],mode:'SHADOW_RBAC'})};
                        if(value==='/api/admin/xingwei/catalog/snapshot')return {ok:true,json:async()=>snapshot};
                        if(value==='/api/admin/xingwei/catalog/services'&&options.method==='POST'){
                          const body=JSON.parse(options.body);
                          window.__writes.push({url:value,method:options.method,headers:options.headers,body});
                          return {ok:true,json:async()=>({id:9,...body,version:0})};
                        }
                        return {ok:false,status:404,json:async()=>({error:'not found'})};
                      };
                    }
                    """);
            addScript(page);
            page.waitForFunction("() => document.querySelector('#xcatCreateOpen').hidden === false && document.querySelectorAll('#xcatServiceRows tr').length === 1");
            page.locator("#xcatCreateOpen").click();
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xcatCreateDialog').open"));
            assertEquals(1, page.locator("#xcatCreateGame option").count());
            assertEquals(2, page.locator("#xcatCreateCategory option").count());
            assertEquals("MW4", page.locator("#xcatCreateGame").inputValue());

            assertTrue(page.locator("#xcatCreateReason").isHidden());
            page.locator("#xcatCreateSlug").fill("weapon-mastery");
            page.locator("#xcatCreateName").fill("武器精通服务");
            page.locator("#xcatCreateShortDesc").fill("按现有分类新增的离线服务");
            page.locator("#xcatCreateDuration").fill("1-2 天");

            page.locator("#xcatCreateSave").click();
            page.waitForFunction("() => window.__writes.length === 1");

            assertEquals("POST", page.evaluate("() => window.__writes[0].method"));
            assertEquals("csrf-token", page.evaluate("() => window.__writes[0].headers['X-CSRF-TOKEN']"));
            assertEquals("weapon-mastery", page.evaluate("() => window.__writes[0].body.slug"));
            assertEquals("MW4", page.evaluate("() => window.__writes[0].body.gameCode"));
            assertEquals("LEVELING", page.evaluate("() => window.__writes[0].body.category"));
            assertEquals("OFFLINE", page.evaluate("() => window.__writes[0].body.status"));
            assertTrue((Boolean) page.evaluate("() => window.__writes[0].body.active === false"));
            assertTrue((Boolean) page.evaluate("() => window.__writes[0].body.coverImage === null"));
            assertEquals(0, ((Number) page.evaluate("() => window.__writes[0].body.hotScore")).intValue());
            assertTrue((Boolean) page.evaluate("() => window.__writes[0].body.expiresAt === null"));
            assertTrue(page.locator("#xcatMessage").textContent().contains("已创建离线服务"));
        }
    }

    @Test
    void managerCanEditExistingServiceAndWriteCarriesCsrfWithoutStructureMutation() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(1280, 850);
            loadPage(page);
            page.evaluate("""
                    () => {
                      document.querySelector('meta[name="_csrf"]').setAttribute('content','csrf-token');
                      document.querySelector('meta[name="_csrf_header"]').setAttribute('content','X-CSRF-TOKEN');
                      window.__writes=[];
                      const snapshot={
                        source:'EXISTING_SERVICE_CATALOG',writeScope:'EXISTING_SERVICE_CREATE_UPDATE',
                        counts:{services:1,activeServices:1,games:1,categories:1,platforms:1},
                        services:[{id:1,slug:'weapon-leveling',gameCode:'MW4',name:'武器升级',category:'LEVELING',shortDesc:'手打升级',priceMode:'FIXED',priceFixed:88,priceMin:null,priceMax:null,durationText:'1-2天',status:'AVAILABLE',coverImage:'/cover.webp',hotScore:12.5,manualPriority:10,expiresAt:'2026-12-01T00:00:00Z',active:true,sortOrder:1,version:3}],
                        games:[{code:'MW4',name:'使命召唤现代战争4',businessStatus:'ACTIVE',active:true,categoryCount:1,serviceCount:1,platformCodes:['PC']}],
                        categories:[{code:'LEVELING',name:'升级',gameCode:'MW4',active:true,serviceCount:1}],platforms:[]
                      };
                      window.fetch=async(url,options={})=>{
                        const value=String(url);
                        if(value==='/api/admin/xingwei/access')return {ok:true,json:async()=>({allowAll:false,permissionCodes:['service:view','service:manage'],mode:'SHADOW_RBAC'})};
                        if(value==='/api/admin/xingwei/catalog/snapshot')return {ok:true,json:async()=>snapshot};
                        if(value==='/api/admin/xingwei/catalog/services/1'&&options.method==='PUT'){
                          window.__writes.push({url:value,method:options.method,headers:options.headers,body:JSON.parse(options.body)});
                          return {ok:true,json:async()=>({...snapshot.services[0],...JSON.parse(options.body),version:4})};
                        }
                        return {ok:false,status:404,json:async()=>({error:'not found'})};
                      };
                    }
                    """);
            addScript(page);
            page.waitForFunction("() => document.querySelectorAll(\"button[data-action='edit']\").length === 1");
            assertTrue(page.locator("#xcatManageMode").textContent().contains("可管理"));

            page.locator("button[data-action='edit']").click();
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xcatEditDialog').open"));
            page.locator("#xcatEditName").fill("武器升级 Pro");

            page.locator("#xcatEditSave").click();
            page.waitForFunction("() => window.__writes.length === 1");

            assertEquals("维护服务资料",page.evaluate("() => window.__writes[0].body.reason"));
            assertEquals("PUT", page.evaluate("() => window.__writes[0].method"));
            assertEquals("\"v3\"", page.evaluate("() => window.__writes[0].headers['If-Match']"));
            assertEquals("csrf-token", page.evaluate("() => window.__writes[0].headers['X-CSRF-TOKEN']"));
            assertEquals("weapon-leveling", page.evaluate("() => window.__writes[0].body.slug"));
            assertEquals("MW4", page.evaluate("() => window.__writes[0].body.gameCode"));
            assertEquals("LEVELING", page.evaluate("() => window.__writes[0].body.category"));
            assertEquals("/cover.webp", page.evaluate("() => window.__writes[0].body.coverImage"));
            assertEquals("2026-12-01T00:00:00Z", page.evaluate("() => window.__writes[0].body.expiresAt"));
            assertEquals("武器升级 Pro", page.evaluate("() => window.__writes[0].body.name"));
            assertTrue(page.locator("#xcatMessage").textContent().contains("已保存"));
        }
    }

    private static void loadPage(Page page) throws Exception {
        page.setContent(Files.readString(MAIN.resolve("templates/admin-xingwei-catalog.html")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(MAIN.resolve("static/assets/admin-xingwei.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(MAIN.resolve("static/assets/admin-xingwei-org.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(MAIN.resolve("static/assets/admin-xingwei-catalog.css")));
    }

    private static void addScript(Page page) {
        page.addScriptTag(new Page.AddScriptTagOptions().setPath(MAIN.resolve("static/assets/admin-xingwei-catalog.js")));
    }
}
