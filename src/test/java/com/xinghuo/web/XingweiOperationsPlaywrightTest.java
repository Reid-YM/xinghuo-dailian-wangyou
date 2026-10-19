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
    private Browser chromium(Playwright p){var options=new BrowserType.LaunchOptions();String executable=System.getenv("XINGHUO_TEST_CHROMIUM");if(executable!=null&&!executable.isBlank())options.setExecutablePath(Path.of(executable));return p.chromium().launch(options);}
    private Page setup(Browser browser,String query) throws Exception {
        return setup(browser,query,false);
    }
    private Page setup(Browser browser,String query,boolean readOnlyCrm)throws Exception{
        Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(390,844));
        String html=Files.readString(Path.of("src/main/resources/templates/admin-xingwei-operations.html"))
            .replace("th:content=\"${csrf.token}\"","content=\"test-csrf\"").replace("th:content=\"${csrf.headerName}\"","content=\"X-CSRF-TOKEN\"");
        page.route("**/*",route->{String path=java.net.URI.create(route.request().url()).getPath();
            if(path.startsWith("/assets/"))route.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static"+path)));
            else route.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(html));});
        page.addInitScript("""
            window.calls=[];window.canManage=!READ_ONLY_CRM;window.failRead=false;window.conflict=false;window.holdGet="";window.failGet="";window.pendingReads=[];window.heldReadReturns=0;window.holdWrite=false;
            window.fetch=async(url,options={})=>{
              const method=options.method||'GET', headers=Object.fromEntries(new Headers(options.headers||{}));
              window.calls.push({url,method,headers,body:options.body});
              if(method==='GET'&&window.holdGet&&url.includes(window.holdGet)){await new Promise(resolve=>window.pendingReads.push({url,resolve}));window.heldReadReturns++;}
              if(method!=='GET'&&window.holdWrite)await new Promise(resolve=>window.releaseWrite=resolve);
              if(method!=='GET')return new Response(JSON.stringify(window.conflict?{message:'记录已变化，请重新读取'}:{data:{},revision:'r2'}),{status:window.conflict?409:200});
              if(url.endsWith('/access'))return new Response(JSON.stringify({restoreAdmin:!READ_ONLY_CRM,modules:Object.fromEntries((READ_ONLY_CRM?['crm']:['members','community','feedback','display','crm']).map(k=>[k,{view:true,manage:window.canManage}]))}));
              if(window.failRead||(window.failGet&&url.includes(window.failGet)))return new Response(JSON.stringify({message:'读取失败，请重试'}),{status:503});
              if(url.endsWith('/logs'))return new Response(JSON.stringify([{action:'LOGIN',createdAt:'2026-10-16T00:00:00Z',ipMasked:'192.0.*.*'}]));
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
            """.replace("READ_ONLY_CRM",Boolean.toString(readOnlyCrm)));
        page.navigate("http://xinghuo.test/admin/xingwei/operations?"+query);
        page.waitForSelector("#operationsRows article");return page;
    }
    @Test void chromiumNativeMemberWritesHaveCsrfRevisionAndPreserveConflictingInput() throws Exception {
        try(Playwright p=Playwright.create();Browser b=chromium(p)){
            Page page=setup(b,"module=members&status=ACTIVE&page=1&q=test");
            assertEquals("ACTIVE",page.locator("#operationsFilters select[name=status]").inputValue());
            assertEquals("正常",page.locator("#operationsFilters option[value=ACTIVE]").textContent());
            assertTrue(page.locator("#operationsPage").textContent().contains("2"));
            page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("编辑").setExact(true)).click();
            assertEquals("客户",page.locator("#operations-role option[value=BUYER]").textContent());
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
    @Test void chromiumCrmIntentSourceFiltersSurviveReloadAndDoNotLeakIntoMemberSearch()throws Exception{
        try(Playwright p=Playwright.create();Browser b=chromium(p)){
            Page page=setup(b,"module=crm&intent=HIGH&source=search");
            assertEquals("HIGH",page.locator("[name=intent]").inputValue());assertEquals("search",page.locator("[name=source]").inputValue());
            assertEquals(true,page.evaluate("()=>window.calls.some(c=>c.url.includes('/leads?')&&c.url.includes('intent=HIGH')&&c.url.includes('source=search'))"));
            page.locator("[name=intent]").selectOption("MEDIUM");page.locator("[name=source]").fill("campaign");page.locator("#operationsFilters button[type=submit]").click();
            page.waitForFunction("()=>location.search.includes('intent=MEDIUM')&&location.search.includes('source=campaign')");
            page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("网站会员").setExact(true)).click();page.waitForFunction("()=>document.querySelector('#operationsTitle').textContent==='网站会员'");
            assertTrue(page.locator("[name=intent]").isDisabled());assertTrue(page.locator("[name=source]").isDisabled());
            assertEquals(true,page.evaluate("()=>{const url=window.calls.filter(c=>c.url.includes('/members?')).at(-1).url;return !url.includes('intent=')&&!url.includes('source=');}"));
        }
    }
    @Test void chromiumMobileAllOperationsModulesWorkAndFailedReadsRecover()throws Exception{try(Playwright p=Playwright.create();Browser b=chromium(p)){allModules(b);}}
    @Test void webkitMobileAllOperationsModulesWorkAndFailedReadsRecover()throws Exception{try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){allModules(b);}}
    @Test void chromiumDraftsAndPendingSavesStaySafe()throws Exception{try(Playwright p=Playwright.create();Browser b=chromium(p)){draftJourney(b);}}
    @Test void webkitDraftsAndPendingSavesStaySafe()throws Exception{try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){draftJourney(b);}}
    private void draftJourney(Browser b)throws Exception{
        Page page=setup(b,"module=crm");page.evaluate("()=>window.confirm=()=>false");
        page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("维护跟进")).click();page.locator("#operations-nextAction").fill("尚未保存的联系计划");
        page.locator("#operationsClose").click();assertTrue(page.locator("#operations-nextAction").isVisible());page.keyboard().press("Escape");assertEquals("尚未保存的联系计划",page.locator("#operations-nextAction").inputValue());
        assertEquals(true,page.evaluate("()=>{const e=new Event('beforeunload',{cancelable:true});window.dispatchEvent(e);return e.defaultPrevented;}"));
        assertEquals(0,page.locator("#operations-followUpNote").count());
        page.evaluate("()=>window.holdWrite=true");page.locator("#operationsSave").click();page.waitForFunction("()=>typeof window.releaseWrite==='function'");
        page.evaluate("()=>{document.querySelector('#operationsForm').dispatchEvent(new Event('submit',{cancelable:true}));document.querySelector('#operationsRefresh').click();document.querySelector('#operationsFilters').dispatchEvent(new Event('submit',{cancelable:true}));}");
        assertEquals(1,page.evaluate("()=>window.calls.filter(c=>c.method==='PATCH').length"));
        assertEquals(false,page.evaluate("()=>Object.hasOwn(JSON.parse(window.calls.find(c=>c.method==='PATCH').body),'followUpNote')"));assertEquals(1,page.evaluate("()=>window.calls.filter(c=>c.url.includes('/leads?')).length"));
        page.evaluate("()=>{window.failRead=true;window.holdWrite=false;window.releaseWrite();}");page.waitForFunction("()=>document.querySelector('#operationsMessage').textContent.includes('保存成功，但列表读取失败')");
        assertEquals(false,page.evaluate("()=>document.querySelector('#operationsDialog').open"));assertEquals(0,page.locator("#operationsRows article").count());assertEquals(0,page.locator("#operationsFields input,#operationsFields textarea").count());
        assertEquals(false,page.evaluate("()=>{const e=new Event('beforeunload',{cancelable:true});window.dispatchEvent(e);return e.defaultPrevented;}"));
        page.evaluate("()=>window.failRead=false");page.locator("#operationsRefresh").click();page.waitForSelector("#operationsRows article");
        page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("维护跟进")).click();page.locator("#operations-nextAction").fill("准备下一次联系");page.evaluate("()=>window.confirm=()=>true");page.locator("#operationsClose").click();page.waitForFunction("()=>!document.querySelector('#operationsDialog').open");
    }
    @Test void chromiumStaleResponsesCannotReopenOrOverwriteAnotherModule()throws Exception{try(Playwright p=Playwright.create();Browser b=chromium(p)){staleJourney(b);}}
    @Test void webkitStaleResponsesCannotReopenOrOverwriteAnotherModule()throws Exception{try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){staleJourney(b);}}
    private void staleJourney(Browser b)throws Exception{
        Page page=setup(b,"module=members");page.evaluate("()=>window.holdGet='/logs'");page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("登录日志")).click();page.waitForFunction("()=>window.pendingReads.length===1");
        page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("客户线索").setExact(true)).click();page.waitForSelector("#operationsRows article");page.evaluate("()=>{window.holdGet='';window.pendingReads.shift().resolve();}");
        page.waitForFunction("()=>window.heldReadReturns===1");assertEquals(false,page.evaluate("()=>document.querySelector('#operationsDialog').open"));
        page.evaluate("()=>{window.holdGet='/members?';window.failGet='/members?';}");page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("网站会员").setExact(true)).click();page.waitForFunction("()=>window.pendingReads.length===1");assertEquals(0,page.locator("#operationsRows article").count());assertTrue(page.locator("#operationsNext").isDisabled());
        page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("测试反馈").setExact(true)).click();page.waitForSelector("#operationsRows article");page.evaluate("()=>{window.holdGet='';window.pendingReads.shift().resolve();}");
        page.waitForFunction("()=>window.heldReadReturns===2&&document.querySelector('#operationsRows').getAttribute('aria-busy')==='false'");assertTrue(page.locator("#operationsRows").textContent().contains("测试问题"));assertFalse(page.locator("#operationsMessage").textContent().contains("读取失败"));
        page.evaluate("()=>window.failRead=true");page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("客户线索").setExact(true)).click();page.waitForFunction("()=>document.querySelector('#operationsMessage').textContent.includes('读取失败')");assertEquals(0,page.locator("#operationsRows article").count());assertTrue(page.url().contains("module=crm"));
        page.evaluate("()=>window.failRead=false");page.locator("#operationsRefresh").click();page.waitForSelector("#operationsRows article");assertTrue(page.locator("#operationsRows").textContent().contains("B-1"));
    }
    @Test void chromiumReadOnlyEmployeeHasOnlyAuthorizedCrmActions()throws Exception{try(Playwright p=Playwright.create();Browser b=chromium(p)){readOnlyJourney(b);}}
    @Test void webkitReadOnlyEmployeeHasOnlyAuthorizedCrmActions()throws Exception{try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){readOnlyJourney(b);}}
    private void readOnlyJourney(Browser b)throws Exception{
        Page page=setup(b,"module=members",true);assertEquals(1,page.locator("#operationsModules button").count());assertEquals("客户线索",page.locator("#operationsModules button").textContent());assertTrue(page.locator("#operationsCreate").isHidden());
        assertEquals(0,page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("维护跟进")).count());page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("详情").setExact(true)).click();assertTrue(page.locator("#operationsSave").isHidden());page.locator("#operationsClose").click();assertEquals(true,page.evaluate("()=>window.calls.every(c=>c.method==='GET')"));
    }
    private void allModules(Browser b)throws Exception{
        {
            Page page=setup(b,"module=comments");
            assertTrue(page.locator("#operationsRows").textContent().contains("<script>不能执行</script>"));assertEquals(0,page.locator("#operationsRows script").count());
            page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("投稿审核").setExact(true)).click();
            page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("查看正文").setExact(true)).click();
            page.waitForFunction("()=>document.querySelector('#operationsFields').textContent.includes('不能执行')");assertEquals(0,page.locator("#operationsFields script").count());page.locator("#operationsClose").click();
            page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("首页展示位").setExact(true)).click();
            page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("编辑展示位").setExact(true)).click();page.locator("#operations-mode").selectOption("MANUAL");assertEquals("手动",page.locator("#operations-mode option[value=MANUAL]").textContent());page.locator("#operations-serviceId").selectOption("1");page.locator("#operationsSave").click();page.waitForFunction("()=>!document.querySelector('#operationsDialog').open");
            assertEquals("\"slot-r1\"",page.evaluate("()=>window.calls.find(c=>c.method==='PATCH').headers['if-match']"));
            page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("测试反馈").setExact(true)).click();page.waitForSelector("#operationsRows article");page.locator("#operationsCreate").click();assertEquals("NORMAL",page.locator("#operations-priority").inputValue());page.locator("#operationsClose").click();
            page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("客户线索").setExact(true)).click();page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("维护跟进").setExact(true)).click();assertEquals("60",page.locator("#operations-leadScore").inputValue());page.locator("#operationsClose").click();
            page.evaluate("()=>window.failRead=true");page.locator("#operationsRefresh").click();page.waitForFunction("()=>document.querySelector('#operationsMessage').textContent.includes('读取失败')");assertEquals("false",page.locator("#operationsRows").getAttribute("aria-busy"));
            page.evaluate("()=>window.failRead=false");page.locator("#operationsRefresh").click();page.waitForFunction("()=>document.querySelector('#operationsRows').getAttribute('aria-busy')==='false'");assertTrue(page.locator("#operationsRows").textContent().contains("B-1"));
            assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
        }
    }
}
