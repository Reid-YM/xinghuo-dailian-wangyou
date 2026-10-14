package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class AccountAfterSalesPlaywrightTest {
    @Test
    void customerSeesExplicitReplyAsTextWithoutInternalNote() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("<body data-page='account-center-orders'><div data-hub-orders></div><aside data-hub-order-detail hidden></aside></body>");
            page.evaluate("""
                    () => { window.__followups=[]; window.fetch = async (url, options={}) => {
                      let data;
                      if (url === '/api/account/me') data={displayName:'客户'};
                      else if (url === '/api/console/orders') data={items:[{businessNo:'XH-1',serviceName:'服务'}]};
                      else if (url === '/api/console/orders/XH-1') data={order:{businessNo:'XH-1'},timeline:[],
                        afterSalesRecords:[{category:'交付或进度',submittedAt:'2026-10-15T00:00:00Z',content:'请核对'}],
                        afterSalesCases:[{recordId:5,category:'交付或进度',status:'IN_PROGRESS',submittedAt:'2026-10-15T00:00:00Z',customerReply:'第二条回复',
                          replies:[{status:'IN_PROGRESS',content:'正在处理 <img src=x onerror=alert(1)>',repliedAt:'2026-10-15T01:00:00Z'},
                                   {status:'IN_PROGRESS',content:'第二条回复',repliedAt:'2026-10-15T02:00:00Z'}],followups:window.__followups}]};
                      else if (url.endsWith('/after-sales/records/5/followups') && options.method==='POST') {
                        window.__followups.push({content:JSON.parse(options.body).content,createdAt:'2026-10-15T03:00:00Z'});data={ok:true};
                      }
                      else if (url === '/api/account/payments/order/XH-1') return new Response('{}',{status:404});
                      else throw new Error('unexpected fetch '+url);
                      return new Response(JSON.stringify(data),{status:200,headers:{'Content-Type':'application/json'}});
                    }; }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(
                    Path.of("src/main/resources/static/assets/account-center.js")));
            page.locator("[data-order-detail]").click();
            page.locator("[data-after-sales-history] h3").last().waitFor();
            String history = page.locator("[data-after-sales-history]").innerText();
            assertTrue(history.contains("处理中"));
            assertTrue(history.contains("正在处理 <img src=x onerror=alert(1)>"));
            assertTrue(history.indexOf("正在处理 <img") < history.indexOf("第二条回复"));
            assertEquals(2, page.locator("[data-after-sales-history] .account-after-sales-entry").last().locator("p").count());
            page.locator(".account-after-sales-followup textarea").fill("新的补充 <img src=x onerror=alert(1)>");
            page.locator(".account-after-sales-followup button[type=submit]").click();
            page.waitForFunction("() => window.__followups.length===1 && document.querySelector('[data-after-sales-history]').textContent.includes('新的补充')");
            assertTrue(page.locator("[data-after-sales-history]").innerText().contains("我的补充"));
            assertEquals(0, page.locator("[data-after-sales-history] img").count());
            assertTrue(!history.contains("内部处理说明"));
            assertEquals(0, page.locator("[data-after-sales-history] img").count());
        }
    }

    @Test
    void structuredHistoryOverridesLegacyNoteAndRefreshesAfterSubmission() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <body data-page="account-center-orders">
                      <div data-hub-orders></div>
                      <aside data-hub-order-detail hidden></aside>
                    </body>
                    """);
            page.evaluate("""
                    () => {
                      window.__records = [];
                      window.__orderGets = 0;
                      window.fetch = async (url, options = {}) => {
                        const method = options.method || 'GET';
                        let data;
                        if (url === '/api/account/me') data = {displayName:'测试用户'};
                        else if (url === '/api/console/orders') data = {items:[{businessNo:'XH-2',serviceName:'服务'}]};
                        else if (url === '/api/console/orders/XH-2' && method === 'GET') {
                          window.__orderGets++;
                          data = {order:{businessNo:'XH-2',customerNote:'[售后问题 · 取消服务 · 2026-10-13T10:00:00Z]\\n旧备注不可显示'},
                                  afterSalesRecords:window.__records, timeline:[]};
                        }
                        else if (url === '/api/account/payments/order/XH-2') return new Response('{}', {status:404});
                        else if (url === '/api/console/orders/XH-2/after-sales' && method === 'POST') {
                          window.__records.push({category:'退款核对',submittedAt:'2026-10-14T10:00:00Z',content:JSON.parse(options.body).content});
                          data = {ok:true,message:'问题说明已提交，请等待客服核对'};
                        } else throw new Error('unexpected fetch ' + url + ' ' + method);
                        return new Response(JSON.stringify(data), {status:200,headers:{'Content-Type':'application/json'}});
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(
                    Path.of("src/main/resources/static/assets/account-center.js")));
            page.locator("[data-order-detail]").click();
            page.locator("[data-after-sales-history]").waitFor();
            assertTrue(page.locator("[data-after-sales-history]").innerText().contains("暂无已提交的售后问题"));
            assertEquals(0, page.locator("[data-after-sales-history] .account-after-sales-entry").count());
            assertTrue(!page.locator("[data-after-sales-history]").innerText().contains("旧备注不可显示"));
            page.locator("[data-after-sales-form] select").selectOption("REFUND_REVIEW");
            page.locator("[data-after-sales-form] textarea").fill("请核对 <img src=x onerror=alert(1)>\n第二行");
            page.locator("[data-after-sales-form] button[type=submit]").click();
            page.waitForFunction("() => window.__orderGets === 2 && "
                    + "document.querySelectorAll('[data-after-sales-history] .account-after-sales-entry').length === 1");
            assertEquals("退款核对", page.locator("[data-after-sales-history] .account-after-sales-entry strong").innerText());
            assertTrue(page.locator("[data-after-sales-history] .account-after-sales-entry").textContent()
                    .contains("请核对 <img src=x onerror=alert(1)>\n第二行"));
            assertEquals(0, page.locator("[data-after-sales-history] img").count());
            assertTrue(!page.locator("[data-after-sales-history]").innerText().contains("旧备注不可显示"));
        }
    }

    @Test
    void orderIssueFormSubmitsCategoryAndContentWithoutPaymentAction() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <body data-page="account-center-orders">
                      <div data-hub-orders></div>
                      <aside data-hub-order-detail hidden></aside>
                    </body>
                    """);
            page.evaluate("""
                    () => {
                      window.__afterSalesPosts = [];
                      window.__customerNote = '\\n\\n[售后问题 · 交付或进度 · 2026-10-13T10:00:00Z]\\n请核对已有进度';
                      window.__orderGets = 0;
                      window.fetch = async (url, options = {}) => {
                        const method = options.method || 'GET';
                        let data;
                        if (url === '/api/account/me') data = {displayName:'测试用户'};
                        else if (url === '/api/console/orders') data = {items:[{businessNo:'XH-1',statusText:'已完成',serviceName:'服务'}]};
                        else if (url === '/api/console/orders/XH-1' && method === 'GET') {
                          window.__orderGets++;
                          data = {order:{businessNo:'XH-1',status:'COMPLETED',customerNote:window.__customerNote}, timeline:[]};
                        }
                        else if (url === '/api/account/payments/order/XH-1') return new Response('{}', {status:404});
                        else if (url === '/api/console/orders/XH-1/after-sales' && method === 'POST') {
                          window.__afterSalesPosts.push(JSON.parse(options.body));
                          window.__customerNote += '\\n\\n[售后问题 · 退款核对 · 2026-10-14T10:00:00Z]\\n' + window.__afterSalesPosts[0].content;
                          data = {ok:true,message:'问题说明已提交，请等待客服核对'};
                        } else throw new Error('unexpected fetch ' + url + ' ' + method);
                        return new Response(JSON.stringify(data), {status:200,headers:{'Content-Type':'application/json'}});
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(
                    Path.of("src/main/resources/static/assets/account-center.js")));
            page.locator("[data-order-detail]").click();
            page.locator("[data-after-sales-form]").waitFor();
            assertTrue(page.locator("[data-after-sales-form]").isVisible());
            assertTrue(page.locator("[data-after-sales-history]").innerText().contains("请核对已有进度"));
            page.locator("[data-after-sales-form] select").selectOption("REFUND_REVIEW");
            page.locator("[data-after-sales-form] textarea").fill("请核对未完成部分 <img src=x onerror=alert(1)>");
            page.locator("[data-after-sales-form] button[type=submit]").click();
            page.waitForFunction("() => window.__afterSalesPosts.length === 1");
            assertEquals("REFUND_REVIEW", page.evaluate("() => window.__afterSalesPosts[0].category"));
            assertEquals("请核对未完成部分 <img src=x onerror=alert(1)>",
                    page.evaluate("() => window.__afterSalesPosts[0].content"));
            page.waitForFunction("() => document.querySelector('[data-after-sales-message]')"
                    + ".textContent.includes('请等待客服核对')");
            assertTrue(page.locator("[data-after-sales-message]").innerText().contains("请等待客服核对"));
            page.waitForFunction("() => window.__orderGets === 2 && "
                    + "document.querySelectorAll('[data-after-sales-history] .account-after-sales-entry').length === 2");
            assertEquals(2, page.locator("[data-after-sales-history] .account-after-sales-entry").count());
            assertTrue(page.locator("[data-after-sales-history]").innerText().contains("请核对未完成部分 <img"));
            assertEquals(0, page.locator("[data-after-sales-history] img").count());
        }
    }
}
