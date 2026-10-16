package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextN5ActionsPlaywrightTest {
    private static final Path NEXT = Path.of("src/main/resources/static/next");

    @Test
    void chromiumSubmitsQuoteSupplementAndRefreshesBusinessCenter() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = page(browser);
            page.evaluate("""
                    () => {
                      window.__writes=[]; window.__reloads=0;
                      window.XHFrontendNextN5={
                        state:{detail:{businessNo:'XH-Q-A1',game:'CODMW4',demand:'武器解锁'}},
                        reload:async()=>{window.__reloads++;}
                      };
                      window.fetch=async (url,options={}) => {
                        window.__writes.push({url,method:options.method,body:options.body,headers:options.headers||{}});
                        return new Response(JSON.stringify({ok:true,message:'补充说明已提交'}),{status:200,headers:{'Content-Type':'application/json'}});
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n5-actions.js")));
            page.waitForSelector("[data-n5-action=quote-supplement]");
            page.locator("[data-n5-action=quote-supplement] textarea").fill("请优先处理武器等级");
            page.locator("[data-n5-action=quote-supplement] button[type=submit]").click();
            page.waitForFunction("() => window.__writes.length===1 && window.__reloads===1");

            assertEquals("/api/console/quotes/XH-Q-A1/supplement", page.evaluate("() => window.__writes[0].url"));
            assertEquals("fetch", page.evaluate("() => window.__writes[0].headers['X-Requested-With']"));
            assertEquals("请优先处理武器等级", page.evaluate("() => JSON.parse(window.__writes[0].body).content"));
            assertTrue(page.locator("#n5ActionsToast").textContent().contains("补充说明已提交"));
        }
    }

    @Test
    void webkitSubmitsOrderNoteAfterSalesAndFollowupToExistingEndpoints() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = page(browser);
            page.evaluate("""
                    () => {
                      window.__writes=[]; window.__reloads=0;
                      window.XHFrontendNextN5={
                        state:{detail:{
                          order:{businessNo:'XH-O-A1',orderNo:'O-A1'},
                          afterSalesCases:[{recordId:7,category:'交付或进度',status:'OPEN',pendingCustomerFollowup:false}]
                        }},
                        reload:async()=>{window.__reloads++;}
                      };
                      window.fetch=async (url,options={}) => {
                        window.__writes.push({url,method:options.method,body:options.body,headers:options.headers||{}});
                        return new Response(JSON.stringify({ok:true,message:'提交成功'}),{status:200,headers:{'Content-Type':'application/json'}});
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n5-actions.js")));
            page.waitForSelector("[data-n5-action=order-supplement]");

            page.locator("[data-n5-action=order-supplement] textarea").fill("明天继续执行");
            page.locator("[data-n5-action=order-supplement] button[type=submit]").click();
            page.waitForFunction("() => window.__writes.length===1 && window.__reloads===1");

            page.locator("[data-n5-action=after-sales] select").selectOption("REFUND_REVIEW");
            page.locator("[data-n5-action=after-sales] textarea").fill("请核对本订单退款情况");
            page.locator("[data-n5-action=after-sales] button[type=submit]").click();
            page.waitForFunction("() => window.__writes.length===2 && window.__reloads===2");

            page.locator("[data-n5-action=after-sales-followup][data-record-id='7'] textarea").fill("补充：订单截图已准备");
            page.locator("[data-n5-action=after-sales-followup][data-record-id='7'] button[type=submit]").click();
            page.waitForFunction("() => window.__writes.length===3 && window.__reloads===3");

            assertEquals("/api/console/orders/XH-O-A1/supplement", page.evaluate("() => window.__writes[0].url"));
            assertEquals("明天继续执行", page.evaluate("() => JSON.parse(window.__writes[0].body).content"));
            assertEquals("/api/console/orders/XH-O-A1/after-sales", page.evaluate("() => window.__writes[1].url"));
            assertEquals("REFUND_REVIEW", page.evaluate("() => JSON.parse(window.__writes[1].body).category"));
            assertEquals("请核对本订单退款情况", page.evaluate("() => JSON.parse(window.__writes[1].body).content"));
            assertEquals("/api/console/orders/XH-O-A1/after-sales/records/7/followups", page.evaluate("() => window.__writes[2].url"));
            assertEquals("补充：订单截图已准备", page.evaluate("() => JSON.parse(window.__writes[2].body).content"));
            assertEquals("fetch", page.evaluate("() => window.__writes[2].headers['X-Requested-With']"));
        }
    }

    private static Page page(Browser browser) {
        Page page = browser.newPage();
        page.route("**/*", route -> route.fulfill(new Route.FulfillOptions()
                .setContentType("text/html")
                .setBody("""
                        <body>
                          <section class="page active" data-page="account">
                            <section id="n5Detail" class="n5-detail"></section>
                          </section>
                        </body>
                        """)));
        page.navigate("https://example.test/next/account");
        return page;
    }
}
