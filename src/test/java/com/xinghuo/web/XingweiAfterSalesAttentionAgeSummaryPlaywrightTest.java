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
class XingweiAfterSalesAttentionAgeSummaryPlaywrightTest {
    @Test
    void attentionAgeSummaryRendersWithoutChangingQueueFilters() {
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
                    <strong id="caseAttentionCount">—</strong>
                    <strong id="caseAttentionWithin24hCount">—</strong>
                    <strong id="caseAttention24To72hCount">—</strong>
                    <strong id="caseAttentionOver72hCount">—</strong>
                    <p id="caseAttentionAgeMessage"></p>
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
            page.navigate("https://example.test/admin/xingwei/after-sales?status=OPEN&category=DELIVERY&q=XW-7&page=2");
            page.evaluate("""
                    () => { window.__queueUrls=[]; window.fetch=async url => {
                      if(url==='/api/admin/xingwei/access') return new Response(JSON.stringify({permissionCodes:['order:view']}));
                      if(url==='/api/admin/xingwei/business/after-sales/cases/summary') return new Response(JSON.stringify({totalCount:12,pendingFollowupCount:3,openCount:5,inProgressCount:4,resolvedCount:3}));
                      if(url==='/api/admin/xingwei/business/after-sales/cases/category-summary') return new Response(JSON.stringify({deliveryCount:4,cancellationCount:3,refundReviewCount:2,otherCount:3}));
                      if(url==='/api/admin/xingwei/business/after-sales/cases/attention-age-summary') return new Response(JSON.stringify({attentionCount:7,within24hCount:3,from24To72hCount:2,over72hCount:2}));
                      if(url.startsWith('/api/admin/xingwei/business/after-sales/cases?')) {
                        window.__queueUrls.push(url);
                        return new Response(JSON.stringify({items:[],totalCount:12,hasNext:false}));
                      }
                      throw new Error('unexpected '+url);
                    }; }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(
                    Path.of("src/main/resources/static/assets/admin-xingwei-after-sales.js")));

            page.waitForFunction("() => document.querySelector('#caseAttentionCount').textContent==='7'");
            assertEquals("3", page.locator("#caseAttentionWithin24hCount").textContent());
            assertEquals("2", page.locator("#caseAttention24To72hCount").textContent());
            assertEquals("2", page.locator("#caseAttentionOver72hCount").textContent());
            assertTrue(page.locator("#caseAttentionAgeMessage").textContent().contains("需跟进"));
            assertEquals("OPEN", page.locator("#caseFilters select[name=status]").inputValue());
            assertEquals("DELIVERY", page.locator("#caseFilters select[name=category]").inputValue());
            assertEquals("XW-7", page.locator("#caseFilters input[name=q]").inputValue());
            assertTrue(page.url().contains("page=2"));
        }
    }
}
