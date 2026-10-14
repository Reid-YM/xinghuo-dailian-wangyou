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
