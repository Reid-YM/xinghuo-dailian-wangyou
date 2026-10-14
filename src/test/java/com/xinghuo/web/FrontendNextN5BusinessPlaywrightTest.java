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
class FrontendNextN5BusinessPlaywrightTest {
    private static final Path NEXT = Path.of("src/main/resources/static/next");

    @Test
    void chromiumReadsRealBusinessDataShowsDetailAndClaimsExplicitOrder() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.route("**/*", route -> route.fulfill(new Route.FulfillOptions()
                    .setContentType("text/html")
                    .setBody(html())));
            page.navigate("https://example.test/next/account");
            page.evaluate("""
                    () => {
                      window.XHFrontendNextN4={state:{profile:{displayName:'测试用户',contactVerified:true}}};
                      window.__claimBody=null; window.__claimHeader=''; window.confirm=()=>true;
                      const json=(body,status=200)=>new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json'}});
                      window.fetch=async (url,options={}) => {
                        if(url==='/api/console/summary') return json({quoteTotal:2,orderTotal:3,activeOrders:1,securityEvents:4});
                        if(url==='/api/console/quotes') return json({items:[{businessNo:'XH-Q-1',game:'CODMW4',demand:'武器解锁',status:'NEW',statusText:'待处理',currentProgress:'已提交',createdAt:'2026-10-14T00:00:00Z',updatedAt:'2026-10-14T01:00:00Z'}],total:1,page:0,size:50});
                        if(url==='/api/console/orders') return json({items:[{orderNo:'O-1',businessNo:'XH-O-1',status:'IN_PROGRESS',statusText:'进行中',progressPercent:72,progressText:'武器等级处理中',gameName:'CODMW4',serviceName:'武器解锁',createdAt:'2026-10-14T00:00:00Z',updatedAt:'2026-10-14T02:00:00Z'}],total:1,page:0,size:50});
                        if(url==='/api/account/orders/candidates') return json([{businessNo:'XH***901',gameName:'CODMW4',serviceName:'历史服务',createdAt:'2026-09-10T00:00:00Z'}]);
                        if(url==='/api/console/orders/XH-O-1') return json({
                          order:{orderNo:'O-1',businessNo:'XH-O-1',status:'IN_PROGRESS',statusText:'进行中',progressPercent:72,progressText:'武器等级处理中',gameName:'CODMW4',serviceName:'武器解锁',createdAt:'2026-10-14T00:00:00Z',updatedAt:'2026-10-14T02:00:00Z'},
                          sourceQuote:{businessNo:'XH-Q-1',game:'CODMW4',demand:'武器解锁',status:'CONVERTED',statusText:'已转订单'},
                          timeline:[{title:'订单创建',content:'系统已建立业务记录',createdAt:'2026-10-14T00:00:00Z'},{title:'进度更新',content:'武器等级处理中',createdAt:'2026-10-14T02:00:00Z'}],
                          afterSalesRecords:[],afterSalesCases:[{recordId:7,category:'交付或进度',submittedAt:'2026-10-14T03:00:00Z',content:'需要核对进度',status:'OPEN',customerReply:'客服正在核对',pendingCustomerFollowup:false}]
                        });
                        if(url==='/api/console/quotes/XH-Q-1') return json({businessNo:'XH-Q-1',game:'CODMW4',demand:'武器解锁',platform:'PC',expectedTime:'本周',status:'NEW',statusText:'待处理',currentProgress:'已提交',customerSupplement:'补充说明',createdAt:'2026-10-14T00:00:00Z',updatedAt:'2026-10-14T01:00:00Z'});
                        if(url==='/api/account/orders/claim') {
                          window.__claimBody=JSON.parse(options.body||'{}');
                          window.__claimHeader=(options.headers||{})['X-Requested-With']||'';
                          return json({businessNo:'XH-O-LEGACY-1',status:'IN_PROGRESS',statusText:'进行中'});
                        }
                        throw new Error('unexpected '+url);
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n5.js")));
            page.waitForSelector("#n5BusinessCenter");

            assertTrue(page.locator("#n5Summary").textContent().contains("我的询价"));
            assertTrue(page.locator("#n5Summary").textContent().contains("2"));
            assertTrue(page.locator("#n5RecordsBody").textContent().contains("XH-O-1"));
            assertTrue(page.locator("#n5Candidates").textContent().contains("XH***901"));

            page.locator("#n5RecordsBody button").first().click();
            page.waitForSelector("#n5Detail:has-text('武器等级处理中')");
            assertTrue(page.locator("#n5Detail").textContent().contains("来源询价"));
            assertTrue(page.locator("#n5Detail").textContent().contains("客服正在核对"));

            page.locator("[data-n5-tab=quotes]").click();
            assertTrue(page.locator("#n5RecordsBody").textContent().contains("XH-Q-1"));
            page.locator("#n5RecordsBody button").first().click();
            page.waitForSelector("#n5Detail:has-text('补充说明')");

            page.locator("#n5ClaimNo").fill("XH-O-LEGACY-1");
            page.locator("#n5ClaimButton").click();
            page.waitForFunction("() => window.__claimBody && window.__claimBody.businessNo==='XH-O-LEGACY-1'");
            assertEquals("XMLHttpRequest", page.evaluate("() => window.__claimHeader"));
            assertTrue(page.locator("#n5Message").textContent().contains("真实业务数据已读取")
                    || page.locator("#n5Message").textContent().contains("认领成功"));
        }
    }

    @Test
    void webkitKeepsClaimDisabledWhenContactIsNotVerified() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage();
            page.route("**/*", route -> route.fulfill(new Route.FulfillOptions()
                    .setContentType("text/html")
                    .setBody(html())));
            page.navigate("https://example.test/next/account");
            page.evaluate("""
                    () => {
                      window.XHFrontendNextN4={state:{profile:{displayName:'未验证用户',contactVerified:false}}};
                      const json=body=>new Response(JSON.stringify(body),{status:200,headers:{'Content-Type':'application/json'}});
                      window.fetch=async url => {
                        if(url==='/api/console/summary') return json({quoteTotal:0,orderTotal:0,activeOrders:0,securityEvents:0});
                        if(url==='/api/console/quotes') return json({items:[],total:0,page:0,size:50});
                        if(url==='/api/console/orders') return json({items:[],total:0,page:0,size:50});
                        if(url==='/api/account/orders/candidates') return json([{businessNo:'SHOULD-NOT-SHOW'}]);
                        throw new Error('unexpected '+url);
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(NEXT.resolve("n5.js")));
            page.waitForSelector("#n5BusinessCenter");

            assertTrue(page.locator("#n5ClaimButton").isDisabled());
            assertTrue(page.locator("#n5ClaimNo").isDisabled());
            assertTrue(page.locator("#n5Candidates").textContent().contains("尚未完成所有权验证"));
            assertTrue(!page.locator("#n5Candidates").textContent().contains("SHOULD-NOT-SHOW"));
        }
    }

    private static String html() {
        return """
                <body>
                  <section class="page active" data-page="account">
                    <section class="n4-shell"></section>
                    <section class="n4-section"></section>
                  </section>
                  <div class="badge">FRONTEND NEXT</div>
                </body>
                """;
    }
}
