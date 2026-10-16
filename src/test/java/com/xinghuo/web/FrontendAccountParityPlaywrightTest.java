package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.FilePayload;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class FrontendAccountParityPlaywrightTest {
    private static final Path NEXT=Path.of("src/main/resources/static/next");
    private Page page(Browser browser){
        Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(390,844));
        page.route("**/*",r->{if(r.request().url().endsWith("/qr.png")){r.fulfill(new Route.FulfillOptions().setContentType("image/svg+xml").setBody("<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10'/></svg>"));return;}r.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setBody("<html><head></head><body><section class='page active' data-page='account'><div class='n4-shell'></div><section class='n4-section'></section></section><div class='badge'></div></body></html>"));});
        page.navigate("https://example.test/next/account");return page;
    }
    @Test void chromiumAvatarUploadAndContactFailurePreserveInputAndUseServerReturnedAvatar()throws Exception{
        try(Playwright p=Playwright.create();Browser b=p.chromium().launch()){
            Page page=page(b);page.evaluate("""
              ()=>{window.calls=[];window.failContact=true;const profile={displayName:'测试用户',avatarUrl:'/old-avatar.png',quoteContacts:[],emailVerified:false};
              window.XHFrontendNextN4={state:{profile,sessions:[]},reload:async()=>{window.reloaded=true;}};
              window.fetch=async(url,options={})=>{window.calls.push({url,method:options.method||'GET',headers:options.headers,body:options.body});
                if(url.endsWith('/profile/avatar')){window.multipart=options.body instanceof FormData;return new Response(JSON.stringify({...profile,avatarUrl:'/new-avatar.png'}));}
                if(url.endsWith('/profile')){window.profileBody=JSON.parse(options.body);return new Response(JSON.stringify({...profile,...window.profileBody}));}
                if(url.endsWith('/quote-contacts')){if(window.failContact)return new Response(JSON.stringify({message:'暂时无法保存，请重试'}),{status:409});return new Response(JSON.stringify({...profile,quoteContacts:[{type:'WECHAT',masked:'tes***'}]}));}
                if(url.endsWith('/quote-contacts/0'))return new Response(JSON.stringify({...profile,quoteContacts:[]}));
                return new Response('[]');};}
              """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n6-account-security.js")));page.waitForSelector("#n6ProfileForm");
            page.locator("#n6DisplayName").fill("更新昵称");
            page.locator("#n6Avatar").setInputFiles(new FilePayload("avatar.png","image/png",Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jFxoAAAAASUVORK5CYII=")));
            page.locator("#n6ProfileForm button[type=submit]").click();page.waitForFunction("()=>window.reloaded===true");
            assertEquals(true,page.evaluate("()=>window.multipart"));assertEquals("/new-avatar.png",page.evaluate("()=>window.profileBody.avatarUrl"));
            page.locator("#n6ContactType").selectOption("WECHAT");page.locator("#n6ContactValue").fill("test_wechat");page.locator("#n6ContactForm button").click();
            page.waitForFunction("()=>document.querySelector('#n6ContactMessage').textContent.includes('重试')");assertEquals("test_wechat",page.locator("#n6ContactValue").inputValue());
            page.evaluate("()=>window.failContact=false");page.locator("#n6ContactForm button").click();page.waitForFunction("()=>document.querySelector('#n6Contacts').textContent.includes('tes***')");
            assertEquals("",page.locator("#n6ContactValue").inputValue());page.locator("#n6Contacts button").click();page.waitForFunction("()=>document.querySelector('#n6Contacts').textContent.includes('暂无')");
            assertEquals(true,page.evaluate("()=>window.calls.every(c=>c.headers['X-Requested-With']==='fetch')"));
        }
    }
    @Test void webkitCloudFavoritesSubmissionsAndServerConfirmedPaymentRemainAvailable()throws Exception{
        try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){
            Page page=page(b);page.onDialog(d->d.accept());page.evaluate("""
              ()=>{window.calls=[];window.favorited=true;window.submitted=true;window.paymentStatus='CREATED';window.XHFrontendNextN4={state:{profile:{displayName:'测试用户'}}};
                window.fetch=async(url,options={})=>{const method=options.method||'GET';window.calls.push({url,method,headers:options.headers,body:options.body});let body={};
                  if(url==='/api/console/summary')body={quoteTotal:0,orderTotal:1,activeOrders:1};
                  else if(url==='/api/console/quotes')body={items:[],total:0};
                  else if(url==='/api/console/orders')body={items:[{businessNo:'B-1',gameName:'游戏',serviceName:'测试服务',status:'IN_PROGRESS',progressPercent:20}],total:1};
                  else if(url==='/api/console/orders/B-1')body={order:{businessNo:'B-1'},timeline:[],afterSalesCases:[{recordId:1,category:'交付',status:'IN_PROGRESS',content:'问题',replies:[{content:'第一次公开回复',repliedAt:'2026-10-15T00:00:00Z'}],followups:[{content:'后续补充',createdAt:'2026-10-16T00:00:00Z'}]}]};
                  else if(url.includes('/payments/')){if(method==='POST')window.paymentStatus='APPROVED';body={paymentNo:'PAY-1',businessNo:'B-1',amount:'88.00',subject:'客服确认服务',status:window.paymentStatus,expiresAt:new Date(Date.now()+60000).toISOString()};}
                  else if(url==='/api/account/progress')body=[{toolKey:'mw4-camo',version:1,updatedAt:'2026-10-16T00:00:00Z'}];
                  else if(url.startsWith('/api/account/favorites?'))body=window.favorited?[{targetType:'GUIDE',targetId:'actual-guide'}]:[];
                  else if(url==='/api/account/favorites/toggle'){window.favorited=false;body={favorited:false};}
                  else if(url.startsWith('/api/account/submissions?'))body=[{id:1,title:'我的稿件',status:window.submitted?'SUBMITTED':'WITHDRAWN',statusText:window.submitted?'等待审核':'已撤回'}];
                  else if(url.endsWith('/submissions/1/withdraw')){window.submitted=false;body={message:'已撤回'};}
                  else body=[];
                  return new Response(JSON.stringify(body));};}
              """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n5.js")));page.waitForSelector("#n5PersonalAssets");
            page.waitForFunction("()=>document.querySelector('[data-personal-asset=progress]').textContent.includes('mw4-camo')");
            assertEquals("/guides/actual-guide",page.locator("[data-personal-asset=favorites] a").getAttribute("href"));
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,new Page.GetByRoleOptions().setName("取消收藏").setExact(true)).click();page.waitForFunction("()=>document.querySelector('[data-personal-asset=favorites]').textContent.includes('暂无')");
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,new Page.GetByRoleOptions().setName("撤回投稿").setExact(true)).click();page.waitForFunction("()=>document.querySelector('[data-personal-asset=submissions]').textContent.includes('已撤回')");
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,new Page.GetByRoleOptions().setName("查看详情").setExact(true)).click();page.waitForSelector("#n5Payment button");
            assertTrue(page.locator("#n5Detail").textContent().contains("第一次公开回复"));assertTrue(page.locator("#n5Detail").textContent().contains("后续补充"));
            page.locator("#n5Payment").getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("生成支付二维码")).click();page.waitForSelector("#n5Payment img");
            assertFalse(page.locator("#n5Payment").textContent().contains("已确认支付成功"));
            page.evaluate("()=>window.paymentStatus='CAPTURED'");page.locator("#n5Payment").getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,new Locator.GetByRoleOptions().setName("刷新支付状态")).click();
            page.waitForFunction("()=>document.querySelector('#n5Payment').textContent.includes('服务端已确认支付成功')");
            assertEquals(true,page.evaluate("()=>window.calls.filter(c=>c.method==='POST').every(c=>c.headers['X-Requested-With']==='fetch')"));
        }
    }
    @Test void chromiumPasswordResetMailTokenIsConsumedWithoutEmailOrCodeAndRemovedFromUrl()throws Exception{
        try(Playwright p=Playwright.create();Browser b=p.chromium().launch()){
            Page page=page(b);page.navigate("https://example.test/account/reset?token=fixture-reset-token");
            page.evaluate("""
              ()=>{window.fetch=async(url,options={})=>{if(url.endsWith('/me'))return new Response(JSON.stringify({message:'请先登录'}),{status:401});if(url.endsWith('/password/reset')){window.resetBody=JSON.parse(options.body);window.resetHeader=options.headers['X-Requested-With'];return new Response(JSON.stringify({message:'重置成功'}));}return new Response(JSON.stringify({enabled:false}));};}
              """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n4.js")));page.waitForSelector("#n4LoginForm");page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n7-auth.js")));page.waitForSelector("#n7ResetSubmit");
            assertFalse(page.url().contains("fixture-reset-token"));assertTrue(page.locator("#n7ResetEmail").isHidden());
            page.locator("#n7ResetPassword").fill("ResetTesting2026!");page.locator("#n7ResetConfirm").fill("ResetTesting2026!");page.locator("#n7ResetSubmit").click();
            page.waitForFunction("()=>window.resetBody?.token==='fixture-reset-token'");assertEquals("fetch",page.evaluate("()=>window.resetHeader"));assertEquals(false,page.evaluate("()=>Object.hasOwn(window.resetBody,'code')"));
        }
    }
}
