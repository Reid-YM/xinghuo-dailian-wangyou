package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.FilePayload;
import java.nio.file.*;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class XingweiBusinessContinuityPlaywrightTest {
    private Browser chromium(Playwright p){var options=new BrowserType.LaunchOptions();String executable=System.getenv("XINGHUO_TEST_CHROMIUM");if(executable!=null&&!executable.isBlank())options.setExecutablePath(Path.of(executable));return p.chromium().launch(options);}
    private Page setup(Browser browser)throws Exception{
        Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(390,844));
        String html=Files.readString(Path.of("src/main/resources/templates/admin-xingwei-business.html")).replace("th:content=\"${csrf.token}\"","content=\"test-token\"").replace("th:content=\"${csrf.headerName}\"","content=\"X-CSRF-TOKEN\"");
        page.route("**/*",r->{String path=java.net.URI.create(r.request().url()).getPath();if(path.startsWith("/assets/"))r.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static"+path)));else r.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(html));});
        page.addInitScript("""
          window.calls=[];window.confirm=()=>false;window.holdCandidates=false;window.candidateCalls=0;window.pendingCandidates=[];window.candidatesReturned=0;
          window.holdPut=false;window.abortPut=false;window.failQuotes=false;window.failCandidates=false;window.holdOrders=false;window.failOrders=false;window.imageVersion=4;
          window.fetch=async(url,options={})=>{
            const method=options.method||'GET';window.calls.push({url,method,body:options.body,headers:options.headers});
            if(url.endsWith('/access'))return new Response(JSON.stringify({mode:'LEGACY_ADMIN_OVERRIDE',dataScope:'ALL',allowAll:true,permissionCodes:[]}));
            if(url.endsWith('/ownership-candidates')){
              const round=++window.candidateCalls;if(window.holdCandidates)await new Promise(resolve=>window.pendingCandidates.push({round,resolve}));window.candidatesReturned++;
              return new Response(JSON.stringify(window.failCandidates?{message:'暂时无法读取候选'}:[{adminUserId:round+10,displayName:'员工'+round,departmentCode:'03',departmentName:'客服中心'}]),{status:window.failCandidates?503:200});
            }
            if(method==='PUT'){
              if(window.holdPut)await new Promise(resolve=>window.releasePut=resolve);
              if(window.abortPut)throw new DOMException('timeout','AbortError');
              window.assignedOwner=JSON.parse(options.body).ownerAdminUserId;return new Response('{}');
            }
            if(method==='PATCH')return new Response('{}');
            if(method==='POST'&&url.endsWith('/image')){window.imageVersion++;return new Response(JSON.stringify({version:window.imageVersion}));}
            if(url.endsWith('/orders')&&window.holdOrders)await new Promise(resolve=>window.releaseOrders=resolve);
            const fail=(url.endsWith('/quotes')&&window.failQuotes)||(url.endsWith('/orders')&&window.failOrders);
            if(fail)return new Response(JSON.stringify({error:'当前读取失败'}),{status:503});
            const isQuote=url.endsWith('/quotes'),exception=url.endsWith('/exceptions');
            const item=isQuote?{businessNo:'XW-Q1',game:'MW4',demand:'测试询价',status:'NEW',canAssign:true,canManage:true,canConvert:true,ownership:{assigned:!!window.assignedOwner,ownerDisplayName:window.assignedOwner?'负责人'+window.assignedOwner:null,departmentCode:'03'}}:
              {businessNo:exception?'XW-ERR':'XW-O1',orderNo:'ORDER1',status:exception?'EXCEPTION':'IN_PROGRESS',progressText:'已有进度',progressPercent:20,version:window.imageVersion,canManage:true,canAssign:true,ownership:{assigned:true,ownerAdminUserId:3,departmentCode:'03'}};
            return new Response(JSON.stringify({dataScope:'ALL',visibleCount:1,unassignedVisible:true,unassignedCount:0,items:[item]}));
          };
          """);
        page.navigate("http://xinghuo.test/admin/xingwei/business");page.waitForSelector("#xwbQuoteRows tr[data-search]");return page;
    }
    private void openAssign(Page page){page.locator("#xwbQuoteRows").getByText("分配业务",new Locator.GetByTextOptions().setExact(true)).click();}
    @Test void chromiumAssignmentWriteIsSingleAndRefreshFailureIsHonest()throws Exception{try(Playwright p=Playwright.create();Browser b=chromium(p)){assignment(b);}}
    @Test void webkitAssignmentWriteIsSingleAndRefreshFailureIsHonest()throws Exception{try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){assignment(b);}}
    private void assignment(Browser b)throws Exception{
        Page page=setup(b);openAssign(page);page.waitForFunction("()=>!document.querySelector('#xwbAssignTarget').disabled");page.locator("#xwbAssignTarget").selectOption("owner:11");
        page.locator("#xwbAssignCancel").click();assertTrue(page.locator("#xwbAssignDialog").isVisible());page.keyboard().press("Escape");assertEquals("owner:11",page.locator("#xwbAssignTarget").inputValue());assertEquals(true,page.evaluate("()=>{const e=new Event('beforeunload',{cancelable:true});dispatchEvent(e);return e.defaultPrevented;}"));
        page.evaluate("()=>window.holdPut=true");page.locator("#xwbAssignForm button[type=submit]").click();page.waitForFunction("()=>typeof window.releasePut==='function'");assertTrue(page.locator("#xwbAssignCancel").isDisabled());
        page.evaluate("()=>{document.querySelector('#xwbAssignForm').dispatchEvent(new Event('submit',{cancelable:true}));document.querySelector('[data-tab=orders]').click();document.querySelector('#xwbRefresh').click();}");assertEquals(1,page.evaluate("()=>window.calls.filter(c=>c.method==='PUT').length"));assertEquals(0,page.evaluate("()=>window.calls.filter(c=>c.url.endsWith('/orders')).length"));
        page.evaluate("()=>{window.failQuotes=true;window.holdPut=false;window.releasePut();}");page.waitForFunction("()=>document.querySelector('#xwbMessage').textContent.includes('业务归属已保存，但列表刷新失败')");assertEquals(0,page.locator("#xwbQuoteRows tr[data-search]").count());assertEquals(false,page.evaluate("()=>document.querySelector('#xwbAssignDialog').open"));
        assertEquals("test-token",page.evaluate("()=>window.calls.find(c=>c.method==='PUT').headers['X-CSRF-TOKEN']"));
        page.evaluate("()=>window.failQuotes=false");page.locator("#xwbRefresh").click();page.waitForSelector("#xwbQuoteRows tr[data-search]");assertTrue(page.locator("#xwbQuoteRows").textContent().contains("负责人11"));
    }
    @Test void chromiumCandidatesAndListsIgnoreObsoleteRequests()throws Exception{try(Playwright p=Playwright.create();Browser b=chromium(p)){stale(b);}}
    @Test void webkitCandidatesAndListsIgnoreObsoleteRequests()throws Exception{try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){stale(b);}}
    private void stale(Browser b)throws Exception{
        Page page=setup(b);page.evaluate("()=>window.holdCandidates=true");openAssign(page);page.waitForFunction("()=>window.pendingCandidates.length===1");assertTrue(page.locator("#xwbAssignForm button[type=submit]").isDisabled());page.locator("#xwbAssignCancel").click();openAssign(page);page.waitForFunction("()=>window.pendingCandidates.length===2");
        page.evaluate("()=>window.pendingCandidates[1].resolve()");page.waitForFunction("()=>[...document.querySelector('#xwbAssignTarget').options].some(o=>o.value==='owner:12')");page.evaluate("()=>window.pendingCandidates[0].resolve()");page.waitForFunction("()=>window.candidatesReturned===2");assertEquals(0,page.locator("#xwbAssignTarget option[value='owner:11']").count());page.locator("#xwbAssignCancel").click();
        page.evaluate("()=>{window.holdOrders=true;window.failOrders=true;}");page.locator("[data-tab=orders]").click();page.waitForFunction("()=>typeof window.releaseOrders==='function'");assertEquals(0,page.locator("#xwbOrderRows tr[data-search],#xwbQuoteRows tr[data-search]").count());
        page.locator("[data-tab=exceptions]").click();page.waitForFunction("()=>document.querySelector('#xwbOrderRows').textContent.includes('XW-ERR')");page.evaluate("()=>window.releaseOrders()");assertEquals("1",page.locator("#xwbKpiVisible").textContent());assertFalse(page.locator("#xwbMessage").textContent().contains("读取业务数据失败"));
        page.evaluate("()=>{window.holdOrders=false;window.failOrders=false;window.holdCandidates=false;window.failCandidates=true;}");page.locator("[data-tab=quotes]").click();page.waitForSelector("#xwbQuoteRows tr[data-search]");openAssign(page);page.waitForSelector("#xwbAssignRetry:not([hidden])");assertTrue(page.locator("#xwbAssignForm button[type=submit]").isDisabled());
        page.evaluate("()=>window.failCandidates=false");page.locator("#xwbAssignRetry").click();page.waitForFunction("()=>!document.querySelector('#xwbAssignTarget').disabled");page.locator("#xwbAssignTarget").selectOption("owner:14");page.evaluate("()=>window.abortPut=true");page.locator("#xwbAssignForm button[type=submit]").click();page.waitForFunction("()=>document.querySelector('#xwbAssignState').textContent.includes('写入结果尚未确认')");assertTrue(page.locator("#xwbAssignForm button[type=submit]").isDisabled());assertTrue(page.locator("#xwbAssignRetry").isHidden());assertEquals(true,page.evaluate("()=>{const r=document.querySelector('#xwbAssignDialog').getBoundingClientRect();return r.left>=0&&r.right<=innerWidth;}"));
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/business-assignment-mobile.png")));page.setViewportSize(1440,900);page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/business-assignment-desktop.png")));
    }
    @Test void chromiumOrderAndImageDraftsGuardLeaving()throws Exception{try(Playwright p=Playwright.create();Browser b=chromium(p)){drafts(b);}}
    @Test void webkitOrderAndImageDraftsGuardLeaving()throws Exception{try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){drafts(b);}}
    private void drafts(Browser b)throws Exception{
        Page page=setup(b);page.locator("[data-tab=orders]").click();page.waitForSelector("#xwbOrderRows tr[data-search]");page.locator("#xwbOrderRows").getByText("编辑订单",new Locator.GetByTextOptions().setExact(true)).click();
        page.locator("#xwbOrderActionForm select[name=status]").selectOption("COMPLETED");assertEquals(true,page.evaluate("()=>{const e=new Event('beforeunload',{cancelable:true});dispatchEvent(e);return e.defaultPrevented;}"));page.locator("[data-action-close=order]").click();assertTrue(page.locator("#xwbOrderActionDialog").isVisible());
        page.evaluate("()=>window.confirm=()=>true");page.locator("[data-action-close=order]").click();page.locator("#xwbOrderRows").getByText("编辑订单",new Locator.GetByTextOptions().setExact(true)).click();page.evaluate("()=>window.confirm=()=>false");
        byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");page.locator("#xwbOrderImageForm input[type=file]").setInputFiles(new FilePayload("progress.png","image/png",png));page.keyboard().press("Escape");assertTrue(page.locator("#xwbOrderActionDialog").isVisible());
        page.locator("#xwbOrderActionForm textarea[name=reason]").fill("更新进度");page.locator("#xwbOrderActionForm button[type=submit]").click();assertEquals(0,page.evaluate("()=>window.calls.filter(c=>c.method==='PATCH').length"));
        page.evaluate("()=>window.confirm=()=>true");page.locator("[data-action-close=order]").click();page.locator("#xwbOrderRows").getByText("编辑订单",new Locator.GetByTextOptions().setExact(true)).click();page.locator("#xwbOrderImageForm input[type=file]").setInputFiles(new FilePayload("progress.png","image/png",png));page.locator("#xwbOrderImageForm button[type=submit]").click();page.waitForFunction("()=>document.querySelector('#xwbOrderActionState').textContent.includes('进度截图已更新')");assertEquals("5",page.locator("#xwbOrderActionForm input[name=version]").inputValue());assertEquals(false,page.evaluate("()=>{const e=new Event('beforeunload',{cancelable:true});dispatchEvent(e);return e.defaultPrevented;}"));assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
    }
}
