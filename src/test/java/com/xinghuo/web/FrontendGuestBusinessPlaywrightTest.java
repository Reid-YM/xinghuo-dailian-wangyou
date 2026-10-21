package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class FrontendGuestBusinessPlaywrightTest {
    static final String NO=FrontendCanonicalRoutesPlaywrightTest.NO;
    static String snapshot(int stage) {
        return "{\"businessNo\":\""+NO+"\",\"canAccept\":"+(stage==0)+",\"paymentEnabled\":true,"
            +"\"offer\":{\"subject\":\"服务器正式报价\",\"amount\":168,\"status\":\""+(stage==0?"QUOTED":"ACCEPTED")+"\",\"expiresAt\":\"2099-10-20T00:00:00Z\"},"
            +"\"order\":"+(stage==0?"null":"{\"orderNo\":\""+NO+"\",\"status\":\""+(stage>=4?"CLOSED":stage>=3?"IN_PROGRESS":"WAITING_CONFIRMATION")+"\",\"progressText\":\"客服更新进度\"}")+","
            +"\"payment\":"+(stage==0?"null":"{\"paymentNo\":\"P-TEST\",\"status\":\""+(stage==1?"CREATED":stage==2?"APPROVED":"CAPTURED")+"\",\"amount\":168,\"expiresAt\":\"2099-10-20T00:00:00Z\"}")+"}";
    }
    @Test void previewAndCanonicalRecordReuseServerOfferAcceptancePaymentAndFulfilmentWithoutAmountWrites() throws Exception {
        both(browser -> {
            for(String base:List.of("/next","")) try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,320,false)) {
                AtomicInteger stage=new AtomicInteger(),accepts=new AtomicInteger(),payments=new AtomicInteger();
                f.page.route("**/api/guest/business/**",route->{
                    String path=URI.create(route.request().url()).getPath();String method=route.request().method();
                    if(method.equals("POST")) {
                        assertEquals("fetch", route.request().headerValue("X-Requested-With"), "Guest write must set CSRF guard");
                        assertNull(route.request().postData(),"Client must not supply a charge amount");
                        if(path.endsWith("/accept")){accepts.incrementAndGet();stage.set(1);}
                        else if(path.endsWith("/payment/native")){payments.incrementAndGet();stage.set(2);}
                        else fail("Unexpected guest write");
                    }
                    if(path.endsWith("/qr.png"))route.fulfill(new Route.FulfillOptions().setContentType("image/svg+xml").setBody("<svg xmlns='http://www.w3.org/2000/svg' width='320' height='320'><rect width='320' height='320' fill='white'/></svg>"));
                    else f.json(route,200,snapshot(stage.get()));
                });
                f.open(base+"/record/"+NO,"#guestBusinessPanel:not([hidden])");
                assertEquals(0,accepts.get());assertEquals(0,payments.get());
                assertTrue(f.page.locator("#guestBusinessPanel").textContent().contains("¥168.00"));
                f.page.onceDialog(Dialog::dismiss);
                f.page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("接受报价并生成订单")).click();
                assertEquals(0,accepts.get());
                f.page.onceDialog(Dialog::accept);
                f.page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("接受报价并生成订单")).click();
                f.page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("生成微信支付二维码")).waitFor();
                assertEquals(1,accepts.get());
                f.page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("生成微信支付二维码")).click();
                f.page.locator("#guestBusinessPanel img[alt='微信支付二维码']").waitFor();
                assertEquals(1,payments.get());assertEquals(1,accepts.get());
                assertTrue(f.page.locator("#guestBusinessPanel img").getAttribute("src").startsWith("/api/guest/business/"+NO+"/payment/qr.png"));
                assertEquals(Boolean.TRUE,f.page.evaluate("() => document.documentElement.scrollWidth <= innerWidth+1"));
                stage.set(3);f.page.reload();f.page.waitForSelector("#guestBusinessPanel .notice.success");
                assertTrue(f.page.locator("#guestBusinessPanel").textContent().contains("已付款 · 订单履约进度"));
                assertEquals(0,f.page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("生成微信支付二维码")).count());
                if(browser.browserType().name().equals("chromium")&&base.isEmpty()){
                    f.page.evaluate("() => {document.activeElement?.blur();window.scrollTo(0,0);}");
                    Files.createDirectories(Path.of("target/frontend-review"));
                    f.page.screenshot(new Page.ScreenshotOptions().setFullPage(true).setPath(Path.of("target/frontend-review/guest-paid-320.png")));
                    f.page.setViewportSize(1280,900);
                    f.page.screenshot(new Page.ScreenshotOptions().setFullPage(true).setPath(Path.of("target/frontend-review/guest-paid-1280.png")));
                }
                stage.set(4);f.page.reload();f.page.waitForSelector("#guestBusinessPanel .business-lifecycle");
                assertTrue(f.page.locator("#guestBusinessPanel").textContent().contains("订单已完成并结单"));
                assertEquals(1,payments.get());assertEquals(1,accepts.get());assertEquals(List.of(),f.errors);
            }
        });
    }
    @Test void missingCapabilityHidesGuestActionsAndReadFailureCanRetry() throws Exception {
        both(browser -> {try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,390,false)){
            AtomicInteger status=new AtomicInteger(403),reads=new AtomicInteger();
            f.page.route("**/api/guest/business/**",route->{assertEquals("GET",route.request().method());reads.incrementAndGet();f.json(route,status.get(),status.get()==200?snapshot(0):"{}");});
            f.open("/record/"+NO,"#n13RecordDetail .n13-facts");f.page.waitForFunction("() => !!document.querySelector('#guestBusinessPanel')");
            assertFalse(f.page.locator("#guestBusinessPanel").isVisible());
            status.set(503);f.page.reload();f.page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("重新读取报价与付款")).waitFor();
            status.set(200);f.page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("重新读取报价与付款")).click();
            f.page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("接受报价并生成订单")).waitFor();
            assertTrue(reads.get()>=3);assertEquals(List.of(),f.errors);
        }});
    }
    @Test void unavailableSharedComponentHasAVisibleReloadAndExplicitLegacyEscape() throws Exception {
        both(browser -> {try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,390,false)){
            f.page.route("**/assets/guest-business.js*",Route::abort);
            f.page.route("**/api/guest/business/**",route->f.json(route,200,snapshot(0)));
            f.open("/record/"+NO,"#guestBusinessPanel:not([hidden])");
            assertEquals("/record/"+NO,f.page.getByRole(AriaRole.LINK,new Page.GetByRoleOptions().setName("重新打开业务记录")).getAttribute("href"));
            f.page.unroute("**/assets/guest-business.js*");
            f.page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("重新加载报价与付款")).click();
            f.page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("接受报价并生成订单")).waitFor();
            assertEquals(List.of(),f.errors);
        }});
    }
    @Test void latePrivateResponseCannotReopenOrPollAfterLeavingTheRecord() throws Exception {
        both(browser -> {try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,390,false)){
            AtomicInteger reads=new AtomicInteger();AtomicReference<Route> pending=new AtomicReference<>();
            f.page.route("**/api/guest/business/**",route->{
                assertEquals("GET",route.request().method());
                if(URI.create(route.request().url()).getPath().endsWith("/qr.png")) {
                    route.fulfill(new Route.FulfillOptions().setContentType("image/svg+xml").setBody("<svg xmlns='http://www.w3.org/2000/svg' width='320' height='320'></svg>"));return;
                }
                if(reads.incrementAndGet()==1)pending.set(route);else f.json(route,200,snapshot(2));
            });
            f.open("/record/"+NO,"#n13RecordDetail .n13-facts");
            f.page.waitForFunction("() => !!document.querySelector('#guestBusinessPanel')");
            f.page.locator(".next-mobile-dock [data-go=services]").click();
            assertNotNull(pending.get());f.json(pending.get(),200,snapshot(2));
            f.page.evaluate("() => new Promise(resolve => setTimeout(resolve,5200))");
            assertEquals(0,f.page.locator("#guestBusinessPanel").count());assertEquals(1,reads.get());
            f.page.goBack();f.page.waitForSelector("#guestBusinessPanel img[alt='微信支付二维码']");
            assertEquals(2,reads.get());assertEquals(List.of(),f.errors);
        }});
    }
    interface Check {void run(Browser browser)throws Exception;}
    void both(Check check)throws Exception{try(Playwright p=Playwright.create();Browser a=p.chromium().launch();Browser b=p.webkit().launch()){for(Browser browser:List.of(a,b))check.run(browser);}}
}
