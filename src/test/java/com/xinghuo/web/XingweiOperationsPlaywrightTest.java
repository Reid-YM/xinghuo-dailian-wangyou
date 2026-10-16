package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class XingweiOperationsPlaywrightTest {
    private Page setup(Browser browser,String query) throws Exception {
        Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(390,844));
        String html=Files.readString(Path.of("src/main/resources/templates/admin-xingwei-operations.html"))
            .replace("th:content=\"${csrf.token}\"","content=\"test-csrf\"").replace("th:content=\"${csrf.headerName}\"","content=\"X-CSRF-TOKEN\"");
        page.route("**/*",route->{String path=java.net.URI.create(route.request().url()).getPath();
            if(path.startsWith("/assets/"))route.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static"+path)));
            else route.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(html));});
        page.addInitScript("""
            window.calls=[];window.canManage=true;window.failRead=false;window.conflict=false;
            window.fetch=async(url,options={})=>{
              const method=options.method||'GET', headers=Object.fromEntries(new Headers(options.headers||{}));
              window.calls.push({url,method,headers,body:options.body});
              if(method!=='GET')return new Response(JSON.stringify(window.conflict?{message:'记录已变化，请重新读取'}:{data:{},revision:'r2'}),{status:window.conflict?409:200});
              if(url.endsWith('/access'))return new Response(JSON.stringify({restoreAdmin:true,modules:Object.fromEntries(['members','community','feedback','display','crm'].map(k=>[k,{view:true,manage:window.canManage}]))}));
              if(window.failRead)return new Response(JSON.stringify({message:'读取失败，请重试'}),{status:503});
              if(url.endsWith('/display/services'))return new Response(JSON.stringify([{id:1,name:'公开服务',status:'PUBLISHED',gameCode:'MW4'}]));
              if(url.endsWith('/display'))return new Response(JSON.stringify([{data:{slotNo:1,mode:'AUTO',serviceId:null},revision:'slot-r1'}]));
              if(url.includes('/submissions/'))return new Response(JSON.stringify({data:{id:1,title:'测试稿件',bodyMarkdown:'<script>不能执行</script>'},revision:'r1'}));
              const data=url.includes('/members')?{id:1,displayName:'测试用户',publicUserId:'XH-1',email:'test@example.invalid',role:'BUYER',status:'ACTIVE'}:
                url.includes('/comments')?{id:1,content:'<script>不能执行</script>',status:'PENDING'}:
                url.includes('/submissions')?{id:1,title:'测试稿件',status:'SUBMITTED'}:
                url.includes('/feedback')?{id:1,title:'测试问题',content:'详细说明',priority:'NORMAL',status:'NEW'}:
                {id:'quote-id',businessNo:'B-1',game:'游戏',demand:'需要报价',leadScore:60,status:'NEW'};
              return new Response(JSON.stringify({items:[{data,revision:'r1'}],page:0,size:25,total:30}));
            };
            """);
        page.navigate("http://xinghuo.test/admin/xingwei/operations?"+query);
        page.waitForSelector("#operationsRows article");return page;
    }
    @Test void chromiumNativeMemberWritesHaveCsrfRevisionAndPreserveConflictingInput() throws Exception {
        try(Playwright p=Playwright.create();Browser b=p.chromium().launch()){
            Page page=setup(b,"module=members&status=ACTIVE&page=1&q=test");
            assertEquals("ACTIVE",page.locator("#operationsFilters select").inputValue());
            assertTrue(page.locator("#operationsPage").textContent().contains("2"));
            page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("编辑").setExact(true)).click();
            page.locator("#operations-displayName").fill("保留当前输入");page.evaluate("()=>window.conflict=true");
            page.locator("#operationsSave").click();page.waitForFunction("()=>document.querySelector('#operationsFormMessage').textContent.includes('记录已变化')");
            assertEquals("保留当前输入",page.locator("#operations-displayName").inputValue());
            assertEquals(true,page.evaluate("()=>document.querySelector('#operationsDialog').open"));
            assertEquals("保留当前输入",page.evaluate("()=>JSON.parse(window.calls.find(c=>c.method==='PATCH').body).displayName"));
            assertEquals("test-csrf",page.evaluate("()=>window.calls.find(c=>c.method==='PATCH').headers['x-csrf-token']"));
            assertEquals("\"r1\"",page.evaluate("()=>window.calls.find(c=>c.method==='PATCH').headers['if-match']"));
            page.evaluate("()=>window.conflict=false");page.locator("#operationsSave").click();page.waitForFunction("()=>!document.querySelector('#operationsDialog').open");
            assertEquals(true,page.evaluate("()=>window.calls.every(c=>c.url.startsWith('/api/admin/xingwei/operations'))"));
        }
    }
    @Test void webkitMobileAllOperationsModulesWorkAndFailedReadsRecover() throws Exception {
        try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){
            Page page=setup(b,"module=comments");
            assertTrue(page.locator("#operationsRows").textContent().contains("<script>不能执行</script>"));assertEquals(0,page.locator("#operationsRows script").count());
            page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("投稿审核").setExact(true)).click();
            page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("查看正文").setExact(true)).click();
            page.waitForFunction("()=>document.querySelector('#operationsFields').textContent.includes('不能执行')");assertEquals(0,page.locator("#operationsFields script").count());page.locator("#operationsClose").click();
            page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("首页展示位").setExact(true)).click();
            page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("编辑展示位").setExact(true)).click();page.locator("#operations-mode").selectOption("MANUAL");page.locator("#operations-serviceId").selectOption("1");page.locator("#operations-reason").fill("选择已发布服务");page.locator("#operationsSave").click();page.waitForFunction("()=>!document.querySelector('#operationsDialog').open");
            assertEquals("\"slot-r1\"",page.evaluate("()=>window.calls.find(c=>c.method==='PATCH').headers['if-match']"));
            page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("测试反馈").setExact(true)).click();page.waitForSelector("#operationsRows article");page.locator("#operationsCreate").click();assertEquals("NORMAL",page.locator("#operations-priority").inputValue());page.locator("#operationsClose").click();
            page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("客户线索 CRM").setExact(true)).click();page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("维护跟进").setExact(true)).click();assertEquals("60",page.locator("#operations-leadScore").inputValue());page.locator("#operationsClose").click();
            page.evaluate("()=>window.failRead=true");page.locator("#operationsRefresh").click();page.waitForFunction("()=>document.querySelector('#operationsMessage').textContent.includes('读取失败')");assertEquals("false",page.locator("#operationsRows").getAttribute("aria-busy"));
            page.evaluate("()=>window.failRead=false");page.locator("#operationsRefresh").click();page.waitForFunction("()=>document.querySelector('#operationsRows').getAttribute('aria-busy')==='false'");assertTrue(page.locator("#operationsRows").textContent().contains("B-1"));
            assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
        }
    }
}
