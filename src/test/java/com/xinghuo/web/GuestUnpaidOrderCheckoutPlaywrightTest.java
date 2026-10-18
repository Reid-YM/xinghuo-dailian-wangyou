package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Reproduces a staff-converted quote that the guest still has to accept and pay. */
@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class GuestUnpaidOrderCheckoutPlaywrightTest {
    @Test
    void previouslyConvertedOrderCanAcceptQuoteAndIssueNativeQr() throws Exception {
        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <div id="recordDetailBox" data-business-no="XHORDERFIRST1"></div>
                    <section id="guestBusinessPanel" hidden></section>
                    """);
            page.evaluate("""
                    () => {
                      window.__acceptCount = 0;
                      window.__qrCount = 0;
                      window.__guest = {
                        businessNo:'XHORDERFIRST1', quoteStatus:'CONVERTED',
                        offer:{subject:'MW4 代练',amount:168,status:'QUOTED',
                          expiresAt:'2099-10-20T00:00:00Z'},
                        order:{orderNo:'XHORDERFIRST1',status:'WAITING_CONFIRMATION'},
                        canAccept:true,paymentEnabled:true,payment:null
                      };
                      window.fetch = async (url, options = {}) => {
                        const path = String(url);
                        const method = options.method || 'GET';
                        if (method === 'POST' && path.endsWith('/accept')) {
                          window.__acceptCount++;
                          window.__guest = {...window.__guest,canAccept:false,
                            offer:{...window.__guest.offer,status:'ACCEPTED'},
                            payment:{paymentNo:'P-1',status:'CREATED',amount:168}};
                          return {ok:true,status:200,json:async()=>window.__guest};
                        }
                        if (method === 'POST' && path.endsWith('/payment/native')) {
                          window.__qrCount++;
                          window.__guest.payment = {...window.__guest.payment,
                            status:'APPROVED',expiresAt:'2099-10-20T00:00:00Z'};
                          return {ok:true,status:200,json:async()=>window.__guest.payment};
                        }
                        if (method === 'GET' && path.endsWith('/XHORDERFIRST1')) {
                          return {ok:true,status:200,json:async()=>window.__guest};
                        }
                        return {ok:false,status:404,json:async()=>({error:'unexpected API'})};
                      };
                    }
                    """);
            page.onDialog(dialog -> dialog.accept());
            page.addScriptTag(new Page.AddScriptTagOptions()
                    .setPath(Path.of("src/main/resources/static/assets/guest-business.js")));
            page.waitForFunction("""
                    () => document.querySelector('#guestBusinessPanel')?.textContent
                      .includes('接受报价并生成订单')
                    """);
            assertTrue(page.locator("#guestBusinessPanel").textContent().contains("XHORDERFIRST1"));
            page.locator("#guestBusinessPanel button:has-text('接受报价并生成订单')").click();
            page.waitForFunction("""
                    () => document.querySelector('#guestBusinessPanel')?.textContent
                      .includes('生成微信支付二维码')
                    """);
            assertEquals(1, ((Number) page.evaluate("() => window.__acceptCount")).intValue());
            page.locator("#guestBusinessPanel button:has-text('生成微信支付二维码')").click();
            page.waitForFunction("""
                    () => document.querySelector('#guestBusinessPanel img[alt="微信支付二维码"]')
                    """);
            assertEquals(1, ((Number) page.evaluate("() => window.__qrCount")).intValue());
        }
    }
}
