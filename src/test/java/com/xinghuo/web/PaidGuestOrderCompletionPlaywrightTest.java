package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class PaidGuestOrderCompletionPlaywrightTest {
    @Test
    void capturedGuestOrderShowsCompletedAndClosedWithoutNewPaymentAction() {
        try (Playwright pw = Playwright.create(); Browser browser = pw.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <div id="recordDetailBox" data-business-no="XHGUESTPAID01"></div>
                    <section id="guestBusinessPanel" hidden></section>
                    """);
            page.evaluate("""
                    () => {
                      window.__guestState = {
                        businessNo:'XHGUESTPAID01',quoteStatus:'CONVERTED',
                        offer:{subject:'MW4 服务',amount:25,status:'ACCEPTED'},
                        order:{orderNo:'O-PAY-1',status:'CLOSED',progressPercent:100,
                          progressText:'服务已完成并结单',
                          updatedAt:'2026-10-18T02:00:00Z',
                          finishedAt:'2026-10-18T01:55:00Z'},
                        payment:{paymentNo:'P-PAY-1',status:'CAPTURED',amount:25,
                          paidAt:'2026-10-18T01:30:00Z'},
                        paymentEnabled:true,canAccept:false
                      };
                      window.fetch = async () => ({
                        ok:true,status:200,json:async()=>window.__guestState
                      });
                    }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions()
                    .setPath(Path.of("src/main/resources/static/assets/guest-business.js")));
            page.waitForFunction("""
                    () => document.querySelector('#guestBusinessPanel')?.textContent
                       .includes('订单已完成并结单')
                    """);
            String visible = page.locator("#guestBusinessPanel").textContent();
            assertTrue(visible.contains("微信已收款"));
            assertTrue(visible.contains("订单已结单"));
            assertTrue(visible.contains("订单结单"));
            assertTrue(visible.contains("服务完成时间"));
            assertTrue(visible.contains("确认收款时间"));
            assertFalse(visible.contains("生成微信支付二维码"));
            assertEquals(0, page.locator("#guestBusinessPanel img[alt='微信支付二维码']").count());
        }
    }
}
