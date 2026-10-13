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
class XingweiServicePublishReviewPlaywrightTest {
    private static final Path MAIN = Path.of("src/main/resources");

    @Test
    void managerMustPassReviewBeforeRestoreAndRestoreCarriesCsrf() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(1280, 850);
            loadPage(page);
            page.onDialog(dialog -> dialog.accept());
            page.evaluate("""
                    () => {
                      document.querySelector('meta[name="_csrf"]').setAttribute('content','csrf-token');
                      document.querySelector('meta[name="_csrf_header"]').setAttribute('content','X-CSRF-TOKEN');
                      window.__writes=[];
                      const snapshot={
                        source:'EXISTING_SERVICE_CATALOG',writeScope:'EXISTING_SERVICE_CREATE_UPDATE_WITH_PUBLISH_PREFLIGHT',
                        counts:{services:1,activeServices:0,games:1,categories:1,platforms:1},
                        services:[{id:7,slug:'weapon-mastery',gameCode:'MW4',name:'武器精通服务',category:'LEVELING',shortDesc:'手打服务说明',priceMode:'FIXED',priceFixed:88,priceMin:null,priceMax:null,durationText:'1-2 天',status:'OFFLINE',coverImage:null,hotScore:0,manualPriority:0,expiresAt:null,active:false,sortOrder:1,version:0}],
                        games:[{code:'MW4',name:'使命召唤现代战争4',businessStatus:'ACTIVE',active:true,categoryCount:1,serviceCount:1,platformCodes:['PC']}],
                        categories:[{code:'LEVELING',name:'升级',gameCode:'MW4',active:true,serviceCount:1}],platforms:[]
                      };
                      const review={serviceId:7,slug:'weapon-mastery',name:'武器精通服务',ready:true,blockers:0,warnings:1,checks:[
                        {code:'TAXONOMY_PUBLIC',label:'游戏与分类可公开',severity:'BLOCKER',passed:true,detail:'MW4 / LEVELING 当前可公开'},
                        {code:'PRICE_VALID',label:'价格配置有效',severity:'BLOCKER',passed:true,detail:'固定价 ¥88.00'},
                        {code:'COVER_RECOMMENDED',label:'服务封面',severity:'WARNING',passed:false,detail:'未配置封面；允许上架'}
                      ]};
                      window.fetch=async(url,options={})=>{
                        const value=String(url);
                        if(value==='/api/admin/xingwei/access')return {ok:true,json:async()=>({allowAll:false,permissionCodes:['service:view','service:manage'],mode:'SHADOW_RBAC'})};
                        if(value==='/api/admin/xingwei/catalog/snapshot')return {ok:true,json:async()=>snapshot};
                        if(value==='/api/admin/xingwei/catalog/services/7/publish-review')return {ok:true,json:async()=>review};
                        if(value==='/api/admin/xingwei/catalog/services/7/restore'&&options.method==='POST'){
                          window.__writes.push({url:value,method:options.method,headers:options.headers});
                          snapshot.services[0]={...snapshot.services[0],status:'AVAILABLE',active:true,version:1};
                          snapshot.counts.activeServices=1;
                          return {ok:true,json:async()=>snapshot.services[0]};
                        }
                        return {ok:false,status:404,json:async()=>({error:'not found'})};
                      };
                    }
                    """);
            addScript(page);
            page.waitForFunction("() => document.querySelectorAll(\"button[data-action='review']\").length === 1");
            assertEquals(0, page.locator("button[data-action='restore']").count());

            page.locator("button[data-action='review']").click();
            page.waitForFunction("() => document.querySelector('#xcatReviewDialog').open && document.querySelector('#xcatReviewPublish').hidden === false");
            assertTrue(page.locator("#xcatReviewSummary").textContent().contains("检查通过"));
            assertTrue(page.locator("#xcatReviewChecks").textContent().contains("服务封面"));
            assertTrue(page.locator("#xcatReviewChecks").textContent().contains("警告项"));

            page.locator("#xcatReviewPublish").click();
            page.waitForFunction("() => window.__writes.length === 1");
            assertEquals("POST", page.evaluate("() => window.__writes[0].method"));
            assertEquals("csrf-token", page.evaluate("() => window.__writes[0].headers['X-CSRF-TOKEN']"));
            assertEquals("/api/admin/xingwei/catalog/services/7/restore", page.evaluate("() => window.__writes[0].url"));
            assertTrue(page.locator("#xcatMessage").textContent().contains("正式上架"));
        }
    }

    @Test
    void blockerHidesPublishButtonAndReviewDialogStaysInsidePhoneViewport() throws Exception {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setViewportSize(390, 844);
            loadPage(page);
            page.evaluate("""
                    () => {
                      const snapshot={
                        source:'EXISTING_SERVICE_CATALOG',writeScope:'EXISTING_SERVICE_CREATE_UPDATE_WITH_PUBLISH_PREFLIGHT',
                        counts:{services:1,activeServices:0,games:1,categories:1,platforms:0},
                        services:[{id:8,slug:'expired-service',gameCode:'MW4',name:'过期服务',category:'LEVELING',shortDesc:'说明',priceMode:'QUOTE',durationText:'咨询',status:'OFFLINE',coverImage:null,hotScore:0,manualPriority:0,expiresAt:'2026-10-01T00:00:00Z',active:false,sortOrder:1,version:0}],
                        games:[{code:'MW4',name:'使命召唤现代战争4',businessStatus:'ACTIVE',active:true,categoryCount:1,serviceCount:1,platformCodes:[]}],
                        categories:[{code:'LEVELING',name:'升级',gameCode:'MW4',active:true,serviceCount:1}],platforms:[]
                      };
                      window.fetch=async(url)=>{
                        const value=String(url);
                        if(value==='/api/admin/xingwei/access')return {ok:true,json:async()=>({allowAll:false,permissionCodes:['service:view','service:manage'],mode:'SHADOW_RBAC'})};
                        if(value==='/api/admin/xingwei/catalog/snapshot')return {ok:true,json:async()=>snapshot};
                        if(value==='/api/admin/xingwei/catalog/services/8/publish-review')return {ok:true,json:async()=>({serviceId:8,slug:'expired-service',name:'过期服务',ready:false,blockers:1,warnings:1,checks:[
                          {code:'NOT_EXPIRED',label:'服务未过期',severity:'BLOCKER',passed:false,detail:'服务已过期'},
                          {code:'COVER_RECOMMENDED',label:'服务封面',severity:'WARNING',passed:false,detail:'未配置封面'}
                        ]})};
                        return {ok:false,status:404,json:async()=>({error:'not found'})};
                      };
                    }
                    """);
            addScript(page);
            page.waitForFunction("() => document.querySelectorAll(\"button[data-action='review']\").length === 1");
            page.locator("button[data-action='review']").click();
            page.waitForFunction("() => document.querySelector('#xcatReviewDialog').open && document.querySelector('#xcatReviewSummary').textContent.includes('检查未通过')");
            assertTrue((Boolean) page.evaluate("() => document.querySelector('#xcatReviewPublish').hidden"));
            assertTrue(page.locator("#xcatReviewChecks").textContent().contains("阻断项"));
            assertTrue((Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"));
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
