package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class XingweiBusinessActionsPlaywrightTest {
    private void setup(Page page,boolean readOnly)throws Exception{
        String html=Files.readString(Path.of("src/main/resources/templates/admin-xingwei-business.html")).replaceAll("<script[^>]*></script>","");
        page.route("**/*",r->r.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setBody(html)));
        page.navigate("https://example.test/admin/xingwei/business");
        for(String css:new String[]{"admin-xingwei.css","admin-xingwei-org.css","admin-xingwei-business.css"})page.addStyleTag(new Page.AddStyleTagOptions().setPath(Path.of("src/main/resources/static/assets/"+css)));
        page.evaluate("""
          readOnly=>{
            window.calls=[];window.converted=false;window.conflict=false;
            document.querySelector('meta[name=_csrf]').content='test-token';document.querySelector('meta[name=_csrf_header]').content='X-CSRF-TOKEN';
            window.fetch=async(url,options={})=>{
              window.calls.push({url,method:options.method,body:options.body,headers:options.headers});
              if(url==='/api/admin/xingwei/access')return new Response(JSON.stringify({mode:'SHADOW_RBAC',dataScope:'SELF',departmentCode:'03',adminUserId:3,allowAll:false,permissionCodes:readOnly?['quote:view','order:view']:['quote:manage','order:manage']}));
              if(url.endsWith('/convert')&&options.method==='POST'){window.converted=true;return new Response(JSON.stringify({businessNo:'XWB1',orderNo:'ORDER1'}));}
              if(options.method==='PATCH')return new Response(JSON.stringify(window.conflict?{error:'订单已被更新，请刷新后重新编辑'}:{}),{status:window.conflict?409:200});
              if(url.endsWith('/quotes'))return new Response(JSON.stringify({dataScope:'SELF',visibleCount:1,items:[{businessNo:'XWB1',game:'MW4',demand:'武器升级',status:window.converted?'CONVERTED':'NEW',canManage:!readOnly&&!window.converted,canConvert:!readOnly&&!window.converted,canAssign:false,hasScreenshot:true,ownership:{assigned:true,ownerAdminUserId:3,ownerUsername:'staff',departmentCode:'03'}}]}));
              if(url.endsWith('/orders'))return new Response(JSON.stringify({dataScope:'SELF',visibleCount:1,items:[{businessNo:'XWB1',orderNo:'ORDER1',status:'IN_PROGRESS',progressPercent:20,progressText:'原有进度',version:4,canManage:!readOnly,canAssign:false,ownership:{assigned:true,ownerAdminUserId:3,departmentCode:'03'}}]}));
              return new Response('{}',{status:404});
            };
          }
          """,readOnly);
        page.addScriptTag(new Page.AddScriptTagOptions().setPath(Path.of("src/main/resources/static/assets/admin-xingwei-business.js")));
        page.waitForFunction("() => document.querySelector('#xwbQuoteRows').textContent.includes('XWB1')");
    }
    @Test
    void chromiumConvertsWithInheritedBusinessNumberAndPreservesConflictingOrderDraft()throws Exception{
        try(Playwright p=Playwright.create();Browser b=p.chromium().launch()){
            Page page=b.newPage();page.onDialog(Dialog::accept);setup(page,false);
            page.locator("#xwbQuoteRows").getByText("处理询价",new Locator.GetByTextOptions().setExact(true)).click();
            page.locator("#xwbQuoteActionForm textarea[name=reason]").fill("需求已确认");page.locator("#xwbConvertQuote").click();
            page.waitForFunction("() => document.querySelector('#xwbOrderRows').textContent.includes('XWB1')");
            assertEquals("test-token",page.evaluate("() => window.calls.find(c=>c.url.endsWith('/convert')).headers['X-CSRF-TOKEN']"));
            assertEquals("NEW",page.evaluate("() => JSON.parse(window.calls.find(c=>c.url.endsWith('/convert')).body).expectedStatus"));
            page.locator("#xwbOrderRows").getByText("编辑订单",new Locator.GetByTextOptions().setExact(true)).click();
            page.locator("#xwbOrderActionForm input[name=progressText]").fill("保留我的进度输入");page.locator("#xwbOrderActionForm textarea[name=reason]").fill("更新进度");
            page.evaluate("() => window.conflict=true");page.locator("#xwbOrderActionForm button[type=submit]").click();
            page.waitForFunction("() => document.querySelector('#xwbOrderActionState').textContent.includes('已被更新')");
            assertEquals("保留我的进度输入",page.locator("#xwbOrderActionForm input[name=progressText]").inputValue());
            assertEquals(4,page.evaluate("() => JSON.parse(window.calls.find(c=>c.method==='PATCH').body).version"));
            assertEquals(true,page.evaluate("() => document.querySelector('#xwbOrderActionDialog').open"));
            assertEquals(false,page.evaluate("() => window.calls.some(c=>/wechat|payment|refund/i.test(c.url))"));
        }
    }
    @Test
    void webkitMobileOrderEditorFitsAndKeepsLongDraftReadable()throws Exception{
        try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){
            Page page=b.newPage(new Browser.NewPageOptions().setViewportSize(390,844));setup(page,false);
            page.locator("[data-tab=orders]").click();page.waitForFunction("() => document.querySelector('#xwbOrderRows').textContent.includes('XWB1')");
            page.locator("#xwbOrderRows").getByText("编辑订单",new Locator.GetByTextOptions().setExact(true)).click();
            page.locator("#xwbOrderActionForm input[name=progressText]").fill("当前进度需要补充说明".repeat(10));
            assertEquals(true,page.evaluate("() => {const r=document.querySelector('#xwbOrderActionDialog').getBoundingClientRect();return r.left>=0&&r.right<=innerWidth;}"));
        }
    }
    @Test
    void webkitReadOnlyStaffCanSeeScreenshotsButCannotCreateOrEdit()throws Exception{
        try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){
            Page page=b.newPage(new Browser.NewPageOptions().setViewportSize(390,844));setup(page,true);
            assertTrue(page.locator("#xwbCreateOrder").isHidden());assertEquals(0,page.locator("#xwbQuoteRows").getByText("处理询价",new Locator.GetByTextOptions().setExact(true)).count());
            assertEquals(1,page.locator("a[href='/api/admin/xingwei/business/quotes/XWB1/image']").count());
            assertEquals(true,page.evaluate("() => document.documentElement.scrollWidth<=innerWidth"));
            page.locator("[data-tab=orders]").click();page.waitForFunction("() => document.querySelector('#xwbOrderRows').textContent.includes('XWB1')");
            assertEquals(0,page.locator("#xwbOrderRows").getByText("编辑订单",new Locator.GetByTextOptions().setExact(true)).count());
        }
    }
}
