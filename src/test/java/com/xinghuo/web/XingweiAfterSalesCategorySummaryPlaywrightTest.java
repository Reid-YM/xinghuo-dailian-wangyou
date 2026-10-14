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
class XingweiAfterSalesCategorySummaryPlaywrightTest {
    @Test
    void categoryCardPreservesOtherFiltersAndResetsPage() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            String markup = """
                    <div id="caseTotalCount"></div><div id="casePendingCount"></div>
                    <button data-case-status="OPEN" disabled aria-pressed="false"><strong id="caseOpenCount">—</strong></button>
                    <button data-case-status="IN_PROGRESS" disabled aria-pressed="false"><strong id="caseInProgressCount">—</strong></button>
                    <button data-case-status="RESOLVED" disabled aria-pressed="false"><strong id="caseResolvedCount">—</strong></button>
                    <p id="caseSummaryMessage"></p>
                    <button data-case-category="DELIVERY" disabled aria-pressed="false"><strong id="caseDeliveryCount">—</strong></button>
                    <button data-case-category="CANCELLATION" disabled aria-pressed="false"><strong id="caseCancellationCount">—</strong></button>
                    <button data-case-category="REFUND_REVIEW" disabled aria-pressed="false"><strong id="caseRefundReviewCount">—</strong></button>
                    <button data-case-category="OTHER" disabled aria-pressed="false"><strong id="caseOtherCount">—</strong></button>
                    <p id="caseCategorySummaryMessage"></p>
                    <form id="caseFilters">
                      <select name="status"><option value=""></option><option value="OPEN">待处理</option><option value="IN_PROGRESS">处理中</option><option value="RESOLVED">已解决</option></select>
                      <select name="category"><option value=""></option><option value="DELIVERY">交付</option><option value="CANCELLATION">取消</option><option value="REFUND_REVIEW">退款核对</option><option value="OTHER">其他</option></select>
                      <select name="followup"><option value=""></option><option value="PENDING">待跟进</option></select>
                      <input name="q" maxlength="80"><button type="submit">查询</button>
                    </form>
                    <p id="caseMessage"></p><table><tbody id="caseRows"></tbody></table>
                    <button id="casePrev"></button><span id="casePage"></span><button id="caseNext"></button>
                    <section id="caseDetail" hidden><div id="caseCurrent"></div><div id="caseFollowups"></div><div id="caseEvents"></div><form id="caseUpdate" hidden></form></section>
                    """;
            page.route("**/*", route -> route.fulfill(
                    new com.microsoft.playwright.Route.FulfillOptions().setBody(markup)));
            page.navigate("https://example.test/admin/xingwei/after-sales?status=OPEN&followup=PENDING&q=XW-9&page=3");
            page.evaluate("""
                    () => { window.__queueUrls=[]; window.fetch=async url => {
                      if(url==='/api/admin/xingwei/access') return new Response(JSON.stringify({permissionCodes:['order:view']}));
                      if(url==='/api/admin/xingwei/business/after-sales/cases/summary') return new Response(JSON.stringify({totalCount:10,pendingFollowupCount:2,openCount:5,inProgressCount:3,resolvedCount:2}));
                      if(url==='/api/admin/xingwei/business/after-sales/cases/category-summary') return new Response(JSON.stringify({deliveryCount:4,cancellationCount:2,refundReviewCount:1,otherCount:3}));
                      if(url.startsWith('/api/admin/xingwei/business/after-sales/cases?')) {
                        window.__queueUrls.push(url);
                        return new Response(JSON.stringify({items:[],totalCount:10,hasNext:true}));
                      }
                      throw new Error('unexpected '+url);
                    }; }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(
                    Path.of("src/main/resources/static/assets/admin-xingwei-after-sales.js")));

            page.waitForFunction("() => window.__queueUrls.length===1 && document.querySelector('#caseDeliveryCount').textContent==='4'");
            assertEquals("2", page.locator("#caseCancellationCount").textContent());
            assertEquals("1", page.locator("#caseRefundReviewCount").textContent());
            assertEquals("3", page.locator("#caseOtherCount").textContent());
            assertTrue(page.locator("#caseCategorySummaryMessage").textContent().contains("保留处理状态"));

            page.locator("[data-case-category=CANCELLATION]").click();
            page.waitForFunction("() => window.__queueUrls.some(url => url.includes('category=CANCELLATION') && url.includes('page=0'))");

            assertEquals("CANCELLATION", page.locator("#caseFilters select[name=category]").inputValue());
            assertEquals("true", page.locator("[data-case-category=CANCELLATION]").getAttribute("aria-pressed"));
            assertTrue(page.url().contains("category=CANCELLATION"));
            assertTrue(page.url().contains("status=OPEN"));
            assertTrue(page.url().contains("followup=PENDING"));
            assertTrue(page.url().contains("q=XW-9"));
            assertTrue(!page.url().contains("page="));
        }
    }
}
