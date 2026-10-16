package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class XingweiPaymentWorkspacePlaywrightTest {
    Page setup(Browser browser) throws Exception {
        Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(390,844));
        String html=Files.readString(Path.of("src/main/resources/templates/admin-xingwei-payments.html")).replace("th:content=\"${csrf.token}\"","content=\"test-csrf\"").replace("th:content=\"${csrf.headerName}\"","content=\"X-CSRF-TOKEN\"");
        page.route("**/*",r->{String path=java.net.URI.create(r.request().url()).getPath();if(path.startsWith("/assets/"))r.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static"+path)));else r.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(html));});
        page.addInitScript("""
            window.calls=[];window.paymentEnabled=true;window.canManage=true;window.conflict=false;window.failRead=false;window.status='CREATED';
            window.fetch=async(url,options={})=>{const method=options.method||'GET';window.calls.push({url,method,body:options.body,headers:Object.fromEntries(new Headers(options.headers||{}))});
              if(method==='POST'){if(window.conflict)return new Response(JSON.stringify({message:'记录已变化，请重新核对'}),{status:409});
                if(url.endsWith('/close')){window.status='CAPTURED';return new Response(JSON.stringify({status:'CAPTURED'}));}
                return new Response(JSON.stringify({paymentNo:'PAY-N',businessNo:'B-1',amount:'95.50',status:'CREATED'}));}
              if(window.failRead)return new Response(JSON.stringify({message:'读取失败'}),{status:503});
              if(url.includes('/orders/'))return new Response(JSON.stringify({orderNo:'O-1',businessNo:'B-1',status:'NEW',customerBound:true,paymentEligible:true,revision:'order-r1'}));
              return new Response(JSON.stringify({enabled:window.paymentEnabled,canManage:window.canManage,total:1,page:0,size:25,items:[{paymentNo:'PAY-1',businessNo:'B-1',subject:'客服已确认服务',amount:'88.00',currency:'CNY',status:window.status,revision:'payment-r1'}]}));};
            """);page.onDialog(dialog->dialog.accept());page.navigate("http://xinghuo.test/admin/xingwei/payments");page.waitForSelector(".xwp-record");return page;
    }
    @Test void chromiumPrepareUsesExplicitAmountAndRevisionRetainsConflictsAndNeverChargesAutomatically() throws Exception {
        try(Playwright p=Playwright.create();Browser b=p.chromium().launch()){
            Page page=setup(b);page.locator("#paymentOrderIdentifier").fill("B-1");page.locator("#paymentOrderLookup button").click();page.waitForFunction("()=>!document.querySelector('#paymentPrepareForm').hidden");
            page.locator("#paymentAmount").fill("95.50");page.locator("#paymentSubject").fill("客服确认服务");page.locator("#paymentReason").fill("与客户核对最终金额");page.evaluate("()=>window.conflict=true");page.locator("#paymentPrepareSubmit").click();page.waitForFunction("()=>document.querySelector('#paymentPrepareMessage').textContent.includes('记录已变化')");
            assertEquals("95.50",page.locator("#paymentAmount").inputValue());assertEquals("客服确认服务",page.locator("#paymentSubject").inputValue());assertFalse(page.locator("#paymentPrepareSubmit").isDisabled());
            assertEquals("95.50",page.evaluate("()=>JSON.parse(window.calls.find(c=>c.method==='POST').body).amount"));assertEquals("\"order-r1\"",page.evaluate("()=>window.calls.find(c=>c.method==='POST').headers['if-match']"));assertEquals("test-csrf",page.evaluate("()=>window.calls.find(c=>c.method==='POST').headers['x-csrf-token']"));
            page.evaluate("()=>window.conflict=false");page.locator("#paymentPrepareSubmit").click();page.waitForFunction("()=>document.querySelector('#paymentPrepareMessage').textContent.includes('等待客户主动支付')");assertEquals(true,page.evaluate("()=>window.calls.every(c=>c.url.startsWith('/api/admin/xingwei/payments'))"));
            assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
        }
    }
    @Test void webkitCloseUsesServerCapturedResultAndDisabledOrReadOnlyStateCannotWrite() throws Exception {
        try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){
            Page page=setup(b);page.locator(".xwp-record button").click();page.waitForFunction("()=>document.querySelector('#paymentMessage').textContent.includes('已到账')");assertTrue(page.locator("#paymentMessage").textContent().contains("没有关闭"));assertEquals("\"payment-r1\"",page.evaluate("()=>window.calls.find(c=>c.url.endsWith('/close')).headers['if-match']"));
            page.evaluate("()=>{window.paymentEnabled=false;window.status='CREATED';}");page.locator("#paymentRefresh").click();page.waitForFunction("()=>document.querySelector('#paymentMessage').textContent.includes('开关关闭')");assertTrue(page.locator("#paymentPrepare").isHidden());assertEquals(0,page.locator(".xwp-record button").count());
            page.evaluate("()=>{window.paymentEnabled=true;window.canManage=false;}");page.locator("#paymentRefresh").click();page.waitForFunction("()=>document.querySelector('#paymentRows').getAttribute('aria-busy')==='false'");assertTrue(page.locator("#paymentPrepare").isHidden());assertEquals(0,page.locator(".xwp-record button").count());
            page.evaluate("()=>window.failRead=true");page.locator("#paymentRefresh").click();page.waitForFunction("()=>document.querySelector('#paymentMessage').textContent.includes('读取失败')");assertEquals("false",page.locator("#paymentRows").getAttribute("aria-busy"));assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth"));
        }
    }
}
