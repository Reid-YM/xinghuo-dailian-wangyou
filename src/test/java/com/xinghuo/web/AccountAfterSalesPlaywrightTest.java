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
                      window.fetch = async (url, options = {}) => {
                        const method = options.method || 'GET';
                        let data;
                        if (url === '/api/account/me') data = {displayName:'测试用户'};
                        else if (url === '/api/console/orders') data = {items:[{businessNo:'XH-1',statusText:'已完成',serviceName:'服务'}]};
                        else if (url === '/api/console/orders/XH-1' && method === 'GET') data = {
                          order:{businessNo:'XH-1',status:'COMPLETED'}, timeline:[]
                        };
                        else if (url === '/api/account/payments/order/XH-1') return new Response('{}', {status:404});
                        else if (url === '/api/console/orders/XH-1/after-sales' && method === 'POST') {
                          window.__afterSalesPosts.push(JSON.parse(options.body));
                          data = {ok:true,message:'问题说明已提交，请等待客服核对'};
                        } else throw new Error('unexpected fetch ' + url + ' ' + method);
                        return new Response(JSON.stringify(data), {status:200,headers:{'Content-Type':'application/json'}});
                      };
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(
                    Path.of("src/main/resources/static/assets/account-center.js")));
            page.locator("[data-order-detail]").click();
            assertTrue(page.locator("[data-after-sales-form]").isVisible());
            page.locator("[data-after-sales-form] select").selectOption("REFUND_REVIEW");
            page.locator("[data-after-sales-form] textarea").fill("请核对未完成部分");
            page.locator("[data-after-sales-form] button[type=submit]").click();
            page.waitForFunction("() => window.__afterSalesPosts.length === 1");
            assertEquals("REFUND_REVIEW", page.evaluate("() => window.__afterSalesPosts[0].category"));
            assertEquals("请核对未完成部分", page.evaluate("() => window.__afterSalesPosts[0].content"));
            assertTrue(page.locator("[data-after-sales-message]").innerText().contains("请等待客服核对"));
        }
    }
}
