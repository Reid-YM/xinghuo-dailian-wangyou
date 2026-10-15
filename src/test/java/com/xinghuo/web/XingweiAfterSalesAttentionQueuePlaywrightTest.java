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
class XingweiAfterSalesAttentionQueuePlaywrightTest {
    @Test
    void overviewQuickLinkPreservesCurrentFiltersInWebKit() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.webkit().launch()) {
            Page page = browser.newPage();
            String markup = """
                    <form id="caseFilters">
                      <select name="status"><option value=""></option><option value="OPEN">待处理</option></select>
                      <select name="category"><option value=""></option><option value="REFUND_REVIEW">退款核对</option></select>
                      <select name="followup"><option value=""></option><option value="PENDING">待跟进</option></select>
                      <input name="q">
                    </form>
                    <a data-case-attention-age-link="OVER_72H" href="/admin/xingwei/after-sales/attention?age=OVER_72H">72 小时以上</a>
                    """;
            page.route("**/*", route -> route.fulfill(
                    new com.microsoft.playwright.Route.FulfillOptions()
                            .setStatus(200)
                            .setContentType("text/html; charset=utf-8")
                            .setBody(markup)));
            page.navigate("https://example.test/admin/xingwei/after-sales/cases?page=4");
            page.locator("select[name=status]").selectOption("OPEN");
            page.locator("select[name=category]").selectOption("REFUND_REVIEW");
            page.locator("select[name=followup]").selectOption("PENDING");
            page.locator("input[name=q]").fill("XW-9");
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(
                    Path.of("src/main/resources/static/assets/admin-xingwei-after-sales-v4k-links.js")));

            page.locator("[data-case-attention-age-link=OVER_72H]").click();
            page.waitForURL("**/admin/xingwei/after-sales/attention?**");

            assertTrue(page.url().contains("age=OVER_72H"));
            assertTrue(page.url().contains("status=OPEN"));
            assertTrue(page.url().contains("category=REFUND_REVIEW"));
            assertTrue(page.url().contains("followup=PENDING"));
            assertTrue(page.url().contains("q=XW-9"));
            assertTrue(!page.url().contains("page="));
        }
    }

    @Test
    void attentionQueueRestoresUrlSortPaginatesAndShowsWaitingDurationInChromium() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            String markup = """
                    <form id="attentionFilters">
                      <select name="age"><option value="ATTENTION">全部</option><option value="WITHIN_24H">24h</option><option value="FROM_24_TO_72H">24-72h</option><option value="OVER_72H">72h+</option></select>
                      <select name="status"><option value=""></option><option value="OPEN">待处理</option><option value="IN_PROGRESS">处理中</option><option value="RESOLVED">已解决</option></select>
                      <select name="category"><option value=""></option><option value="DELIVERY">交付</option><option value="CANCELLATION">取消</option><option value="REFUND_REVIEW">退款</option><option value="OTHER">其他</option></select>
                      <select name="followup"><option value=""></option><option value="PENDING">待跟进</option></select>
                      <select name="sort"><option value="NEWEST_FIRST">最新</option><option value="OLDEST_FIRST">最久</option></select>
                      <input name="q" maxlength="80"><button type="submit">查询</button>
                    </form>
                    <p id="attentionMessage"></p>
                    <table><tbody id="attentionRows"></tbody></table>
                    <button id="attentionPrev"></button><span id="attentionPage"></span><button id="attentionNext"></button>
                    """;
            page.route("**/*", route -> route.fulfill(
                    new com.microsoft.playwright.Route.FulfillOptions()
                            .setStatus(200)
                            .setContentType("text/html; charset=utf-8")
                            .setBody(markup)));
            page.navigate("https://example.test/admin/xingwei/after-sales/attention?age=FROM_24_TO_72H&status=OPEN&category=OTHER&followup=PENDING&sort=OLDEST_FIRST&q=XW-7&page=2");
            page.evaluate("""
                    () => { window.__queueUrls=[]; window.fetch=async url => {
                      if(url==='/api/admin/xingwei/access') return new Response(JSON.stringify({permissionCodes:['order:view']}));
                      if(url.startsWith('/api/admin/xingwei/business/after-sales/cases/attention-queue?')) {
                        window.__queueUrls.push(url);
                        return new Response(JSON.stringify({
                          items:[{businessNo:'XW-7',record:{recordId:7,orderNo:'O7',category:'OTHER',categoryLabel:'其他问题',content:'需要跟进',submittedAtMs:1000,status:'OPEN',handlingNote:null,handledByAdminUserId:null,updatedAt:null,version:0},latestCustomerFollowupAtMs:2000,pendingCustomerFollowup:true,attentionAtMs:2000,waitingMs:108000000,ageBucket:'FROM_24_TO_72H'}],
                          totalCount:101,page:2,pageSize:50,status:'OPEN',category:'OTHER',followup:'PENDING',query:'XW-7',hasNext:true
                        }));
                      }
                      throw new Error('unexpected '+url);
                    }; }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(
                    Path.of("src/main/resources/static/assets/admin-xingwei-after-sales-attention.js")));

            page.waitForFunction("() => window.__queueUrls.length===1 && document.querySelector('#attentionRows').textContent.includes('XW-7')");
            String first = (String) page.evaluate("() => window.__queueUrls[0]");
            assertTrue(first.contains("age=FROM_24_TO_72H"));
            assertTrue(first.contains("status=OPEN"));
            assertTrue(first.contains("category=OTHER"));
            assertTrue(first.contains("followup=PENDING"));
            assertTrue(first.contains("sort=OLDEST_FIRST"));
            assertTrue(first.contains("q=XW-7"));
            assertTrue(first.contains("page=2"));
            assertEquals("FROM_24_TO_72H", page.locator("select[name=age]").inputValue());
            assertEquals("OLDEST_FIRST", page.locator("select[name=sort]").inputValue());
            assertTrue(page.locator("#attentionRows").textContent().contains("1天6小时"));
            assertTrue(page.locator("#attentionRows").textContent().contains("24–72小时"));
            assertTrue(page.locator("#attentionRows a").getAttribute("href").contains("q=XW-7"));

            page.locator("#attentionPrev").click();
            page.waitForFunction("() => window.__queueUrls.length===2");
            assertTrue(page.url().contains("page=1"));
            assertTrue(page.url().contains("sort=OLDEST_FIRST"));
            assertTrue(((String) page.evaluate("() => window.__queueUrls[1]")).contains("page=1"));
            assertTrue(((String) page.evaluate("() => window.__queueUrls[1]")).contains("sort=OLDEST_FIRST"));
        }
    }
}
