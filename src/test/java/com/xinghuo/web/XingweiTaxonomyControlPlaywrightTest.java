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
class XingweiTaxonomyControlPlaywrightTest {
    private static final Path MAIN = Path.of("src/main/resources");

    @Test
    void viewerCanPreviewVisibilityButCannotToggleAndPhoneDoesNotOverflow() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(390, 844);
            loadPage(page);
            installFetch(page, false);
            addScript(page);

            page.waitForFunction("() => document.querySelectorAll('#xtaxGameRows button[data-kind=game]').length === 1");
            page.locator("#xtaxGameRows button[data-kind='game']").click();
            page.waitForFunction("() => document.querySelector('#xtaxDialog').open && document.querySelector('#xtaxImpact').textContent.includes('目标状态')");

            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xtaxToggle').hidden"));
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xtaxReasonWrap').hidden"));
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xtaxAckWrap').hidden"));
            assertEquals("2", page.locator("#xtaxVisibleNow").textContent());
            assertEquals("0", page.locator("#xtaxVisibleAfter").textContent());
            assertTrue(page.locator("#xtaxVisibilitySummary").textContent().contains("全部隐藏"));
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
        }
    }

    @Test
    void managerMustPreviewAcknowledgeThenPutWithReasonTokenAndCsrf() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(1280, 850);
            loadPage(page);
            page.evaluate("""
                    () => {
                      document.querySelector('meta[name="_csrf"]').setAttribute('content','csrf-token');
                      document.querySelector('meta[name="_csrf_header"]').setAttribute('content','X-CSRF-TOKEN');
                    }
                    """);
            installFetch(page, true);
            addScript(page);

            page.waitForFunction("() => document.querySelectorAll('#xtaxGameRows button[data-kind=game]').length === 1");
            page.locator("#xtaxGameRows button[data-kind='game']").click();
            page.waitForFunction("() => document.querySelector('#xtaxDialog').open && document.querySelector('#xtaxToggle').hidden === false");
            assertTrue(page.locator("#xtaxImpactServices").textContent().contains("3"));
            assertTrue(page.locator("#xtaxImpactPublicServices").textContent().contains("2"));
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xtaxToggle').disabled"));

            page.locator("#xtaxReason").fill("赛季结束暂停接单");
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xtaxToggle').disabled"));
            page.locator("#xtaxAcknowledge").check();
            assertTrue((Boolean) page.evaluate("() => !document.querySelector('#xtaxToggle').disabled"));
            page.locator("#xtaxToggle").click();
            page.waitForFunction("() => window.__writes.length === 1");

            assertEquals("PUT", page.evaluate("() => window.__writes[0].method"));
            assertEquals("csrf-token", page.evaluate("() => window.__writes[0].headers['X-CSRF-TOKEN']"));
            assertEquals(false, page.evaluate("() => window.__writes[0].body.active"));
            assertEquals("赛季结束暂停接单", page.evaluate("() => window.__writes[0].body.reason"));
            assertEquals("impact-token-1", page.evaluate("() => window.__writes[0].body.impactToken"));
            assertTrue(page.locator("#xtaxMessage").textContent().contains("影响快照已校验"));
        }
    }

    private static void installFetch(Page page, boolean manager) {
        page.evaluate("""
                (manager) => {
                  window.__writes=[];
                  const snapshot={
                    counts:{services:3,activeServices:2,games:1,categories:1,platforms:1},
                    games:[{code:'MW4',name:'使命召唤现代战争4',businessStatus:'ACTIVE',active:true,categoryCount:1,serviceCount:3,platformCodes:['PC']}],
                    categories:[{id:9,code:'LEVELING',name:'升级',gameCode:'MW4',active:true,serviceCount:3}],platforms:[],services:[]
                  };
                  const impact={
                    type:'GAME',key:'MW4',name:'使命召唤现代战争4',active:true,
                    categoryCount:1,activeCategoryCount:1,serviceCount:3,publicCandidateServiceCount:2,
                    currentPublicServiceCount:2,targetPublicServiceCount:0,parentActive:true,
                    visibilitySummary:'停用后，前台该游戏入口、分类入口以及当前 2 个公开服务将全部隐藏。',
                    warnings:['将隐藏 2 个当前公开服务'],impactToken:'impact-token-1',
                    effect:'停用后，该游戏及其分类下的公开候选服务会从公开目录隐藏。'
                  };
                  window.fetch=async(url,options={})=>{
                    const value=String(url);
                    if(value==='/api/admin/xingwei/access')return {ok:true,json:async()=>({allowAll:false,permissionCodes:manager?['service:view','service:manage']:['service:view'],mode:'SHADOW_RBAC'})};
                    if(value==='/api/admin/xingwei/catalog/snapshot')return {ok:true,json:async()=>snapshot};
                    if(value==='/api/admin/xingwei/catalog/taxonomy/games/MW4/impact')return {ok:true,json:async()=>impact};
                    if(value==='/api/admin/xingwei/catalog/taxonomy/games/MW4/active'&&options.method==='PUT'){
                      const body=JSON.parse(options.body);
                      window.__writes.push({url:value,method:options.method,headers:options.headers,body});
                      snapshot.games[0]={...snapshot.games[0],active:body.active};
                      return {ok:true,json:async()=>snapshot.games[0]};
                    }
                    return {ok:false,status:404,json:async()=>({error:'not found'})};
                  };
                }
                """, manager);
    }

    private static void loadPage(Page page) throws Exception {
        page.setContent(Files.readString(MAIN.resolve("templates/admin-xingwei-taxonomy.html")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(MAIN.resolve("static/assets/admin-xingwei.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(MAIN.resolve("static/assets/admin-xingwei-taxonomy.css")));
        page.addStyleTag(new Page.AddStyleTagOptions().setPath(MAIN.resolve("static/assets/admin-xingwei-taxonomy-v3i.css")));
    }

    private static void addScript(Page page) {
        page.addScriptTag(new Page.AddScriptTagOptions().setPath(MAIN.resolve("static/assets/admin-xingwei-taxonomy.js")));
    }
}
