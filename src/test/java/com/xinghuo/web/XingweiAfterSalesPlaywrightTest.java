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
class XingweiAfterSalesPlaywrightTest {
    @Test
    void sharedFilterLinkRestoresFiltersAndPage() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            String markup = """
                    <div id="caseTotalCount"></div><div id="casePendingCount"></div><button data-case-status="OPEN" disabled aria-pressed="false"><strong id="caseOpenCount">—</strong></button><button data-case-status="IN_PROGRESS" disabled aria-pressed="false"><strong id="caseInProgressCount">—</strong></button><button data-case-status="RESOLVED" disabled aria-pressed="false"><strong id="caseResolvedCount">—</strong></button><p id="caseSummaryMessage"></p>
                    <form id="caseFilters"><select name="status"><option value=""></option><option value="OPEN">待处理</option><option value="IN_PROGRESS">处理中</option><option value="RESOLVED">已解决</option></select><select name="category"><option value=""></option><option value="DELIVERY">交付</option></select><select name="followup"><option value=""></option><option value="PENDING">待跟进</option></select><input name="q" maxlength="80"><button type="submit">查询</button></form>
                    <p id="caseMessage"></p><table><tbody id="caseRows"></tbody></table>
                    <button id="casePrev"></button><span id="casePage"></span><button id="caseNext"></button>
                    <section id="caseDetail" hidden><div id="caseCurrent"></div><div id="caseFollowups"></div><div id="caseEvents"></div><form id="caseUpdate" hidden></form></section>
                    """;
            page.route("**/*", route -> route.fulfill(
                    new com.microsoft.playwright.Route.FulfillOptions().setBody(markup)));
            page.navigate("https://example.test/admin/xingwei/after-sales?status=OPEN&category=DELIVERY&followup=PENDING&q=XW-1&page=2");
            page.evaluate("""
                    () => { window.__queueUrls=[]; window.fetch=async url => {
                      if(url==='/api/admin/xingwei/access') return new Response(JSON.stringify({permissionCodes:['order:view']}));
                      if(url==='/api/admin/xingwei/business/after-sales/cases/summary') return new Response(JSON.stringify({totalCount:10,pendingFollowupCount:4,openCount:3,inProgressCount:5,resolvedCount:2}));
                      if(url.startsWith('/api/admin/xingwei/business/after-sales/cases?')) {
                        window.__queueUrls.push(url);
                        return new Response(JSON.stringify({items:[],totalCount:10,hasNext:true}));
                      }
                      throw new Error('unexpected '+url);
                    }; }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(
                    Path.of("src/main/resources/static/assets/admin-xingwei-after-sales.js")));
            page.waitForFunction("() => window.__queueUrls.length===1");
            page.waitForFunction("() => !document.querySelector('#caseNext').disabled && document.querySelector('#casePendingCount').textContent==='4'");
            assertTrue((Boolean) page.evaluate("() => window.__queueUrls[0].includes('page=2') && window.__queueUrls[0].includes('status=OPEN') && window.__queueUrls[0].includes('category=DELIVERY') && window.__queueUrls[0].includes('followup=PENDING') && window.__queueUrls[0].includes('q=XW-1')"));
            assertEquals("OPEN", page.locator("#caseFilters select[name=status]").inputValue());
            assertEquals("4", page.locator("#casePendingCount").textContent());
            assertEquals("3", page.locator("#caseOpenCount").textContent());
            page.locator("#caseNext").click();
            page.waitForFunction("() => window.__queueUrls.some(url => url.includes('page=3'))");
            assertTrue(page.url().contains("page=3"));
            page.goBack();
            page.waitForFunction("() => window.__queueUrls.length>=3 && document.querySelector('#casePage').textContent.includes('第 3 页')");
            assertTrue(page.url().contains("page=2"));
            page.locator("[data-case-status=RESOLVED]").click();
            page.waitForFunction("() => window.__queueUrls.some(url => url.includes('status=RESOLVED') && url.includes('page=0'))");
            assertEquals("RESOLVED", page.locator("#caseFilters select[name=status]").inputValue());
            assertEquals("true", page.locator("[data-case-status=RESOLVED]").getAttribute("aria-pressed"));
            assertTrue(page.url().contains("status=RESOLVED"));
            assertTrue(page.url().contains("category=DELIVERY"));
            assertTrue(page.url().contains("followup=PENDING"));
            assertTrue(page.url().contains("q=XW-1"));
            assertTrue(!page.url().contains("page="));
            page.goBack();
            page.waitForFunction("() => document.querySelector('#caseFilters select[name=status]').value==='OPEN' && document.querySelector('#casePage').textContent.includes('第 3 页')");
            assertEquals("false", page.locator("[data-case-status=RESOLVED]").getAttribute("aria-pressed"));
        }
    }

    @Test
    void summaryFailureLeavesQueueUsableAndDisablesStatusActions() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            String markup = """
                    <div id="caseTotalCount">—</div><div id="casePendingCount">—</div><button data-case-status="OPEN" disabled aria-pressed="false"><strong id="caseOpenCount">—</strong></button><button data-case-status="IN_PROGRESS" disabled aria-pressed="false"><strong id="caseInProgressCount">—</strong></button><button data-case-status="RESOLVED" disabled aria-pressed="false"><strong id="caseResolvedCount">—</strong></button><p id="caseSummaryMessage"></p>
                    <form id="caseFilters"><select name="status"><option value=""></option><option value="OPEN">待处理</option></select><select name="category"><option value=""></option></select><select name="followup"><option value=""></option></select><input name="q"><button type="submit">查询</button></form>
                    <p id="caseMessage"></p><table><tbody id="caseRows"></tbody></table><button id="casePrev"></button><span id="casePage"></span><button id="caseNext"></button>
                    <section id="caseDetail" hidden><div id="caseCurrent"></div><div id="caseFollowups"></div><div id="caseEvents"></div><form id="caseUpdate" hidden></form></section>
                    """;
            page.route("**/*", route -> route.fulfill(new com.microsoft.playwright.Route.FulfillOptions().setBody(markup)));
            page.navigate("https://example.test/admin/xingwei/after-sales");
            page.evaluate("""
                    () => { window.__queueUrls=[]; window.fetch=async url => {
                      if(url==='/api/admin/xingwei/access') return new Response(JSON.stringify({permissionCodes:['order:view']}));
                      if(url==='/api/admin/xingwei/business/after-sales/cases/summary') return new Response(JSON.stringify({message:'概览读取失败'}),{status:503});
                      if(url.startsWith('/api/admin/xingwei/business/after-sales/cases?')) {window.__queueUrls.push(url);return new Response(JSON.stringify({items:[],totalCount:0,hasNext:false}));}
                      throw new Error('unexpected '+url);
                    }; }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(Path.of("src/main/resources/static/assets/admin-xingwei-after-sales.js")));
            page.waitForFunction("() => document.querySelector('#caseSummaryMessage').textContent.includes('概览读取失败') && window.__queueUrls.length===1");
            assertTrue(page.locator("[data-case-status=OPEN]").isDisabled());
            assertEquals("—", page.locator("#caseOpenCount").textContent());
            page.locator("#caseFilters select[name=status]").selectOption("OPEN");
            page.locator("#caseFilters button[type=submit]").click();
            page.waitForFunction("() => window.__queueUrls.length===2");
            assertTrue(page.url().contains("status=OPEN"));
            assertTrue(page.locator("#caseMessage").textContent().contains("已读取"));
        }
    }

    @Test
    void managerCanAppendReplyWithoutChangingStatus() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            String markup = """
                    <meta name="_csrf" content="token"><meta name="_csrf_header" content="X-CSRF-TOKEN">
                    <div id="caseTotalCount"></div><div id="casePendingCount"></div><p id="caseSummaryMessage"></p>
                    <form id="caseFilters"><select name="status"><option value=""></option><option value="OPEN">待处理</option></select><select name="category"><option value=""></option></select><select name="followup"><option value=""></option><option value="PENDING">待跟进</option></select><input name="q"><button type="submit">查询</button></form>
                    <p id="caseMessage"></p><table><tbody id="caseRows"></tbody></table>
                    <button id="casePrev"></button><span id="casePage"></span><button id="caseNext"></button>
                    <section id="caseDetail" hidden><div id="caseCurrent"></div><div id="caseFollowups"></div><div id="caseEvents"></div>
                      <form id="caseUpdate" hidden><select name="status"><option value="IN_PROGRESS">处理中</option><option value="RESOLVED">已解决</option></select><textarea name="handlingNote"></textarea><textarea name="customerReply"></textarea><button type="submit">保存</button></form>
                    </section>
                    """;
            page.route("**/*", route -> route.fulfill(
                    new com.microsoft.playwright.Route.FulfillOptions().setBody(markup)));
            page.navigate("https://example.test/admin/xingwei/after-sales");
            page.setContent(markup);
            page.evaluate("""
                    () => { window.__sent=[]; window.__queueUrls=[]; window.fetch=async (url, options={}) => {
                      let data;
                      if(url==='/api/admin/xingwei/access') data={permissionCodes:['order:view','order:manage']};
                      else if(url==='/api/admin/xingwei/business/after-sales/cases/summary') data={totalCount:12,pendingFollowupCount:3};
                      else if(url.startsWith('/api/admin/xingwei/business/after-sales/cases?')) {window.__queueUrls.push(url);data={items:[{businessNo:'XW-1',record:{recordId:5,categoryLabel:'交付或进度',content:'请核对',status:'IN_PROGRESS',submittedAtMs:1000},latestCustomerFollowupAtMs:2000,pendingCustomerFollowup:true}],totalCount:1,hasNext:false};}
                      else if(url.endsWith('/history')) data={current:{recordId:5,categoryLabel:'交付或进度',content:'请核对',status:'IN_PROGRESS',version:window.__sent.length?2:1},events:[],handledThroughFollowupId:0,customerFollowups:[{followupId:9,content:'客户有新补充',createdAt:'2026-10-15T03:00:00Z'}]};
                      else if(options.method==='PUT'){window.__sent.push(JSON.parse(options.body));data={status:'IN_PROGRESS',version:2};}
                      else throw new Error('unexpected '+url);
                      return new Response(JSON.stringify(data),{status:200,headers:{'Content-Type':'application/json'}});
                    }; }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(
                    Path.of("src/main/resources/static/assets/admin-xingwei-after-sales.js")));
            page.waitForFunction("() => document.querySelector('#casePendingCount')?.textContent==='3'");
            assertEquals("12", page.locator("#caseTotalCount").textContent());
            page.locator("#caseRows button").click();
            assertTrue(page.locator("#caseRows").innerText().contains("客户补充"));
            assertTrue(page.locator("#caseRows").innerText().contains("待跟进"));
            page.locator("#caseFollowups").getByText("客户有新补充").waitFor();
            page.waitForFunction("() => Boolean(document.querySelector('#caseUpdate option[data-same-status]'))");
            page.locator("#caseUpdate select[name=status]").selectOption("IN_PROGRESS");
            page.locator("#caseUpdate textarea[name=handlingNote]").fill("内部跟进");
            page.locator("#caseUpdate textarea[name=customerReply]").fill("仍在处理");
            page.locator("#caseUpdate button[type=submit]").click();
            page.waitForFunction("() => window.__sent.length===1 && document.querySelector('#caseMessage').textContent.includes('已保存')");
            assertEquals("IN_PROGRESS", page.evaluate("() => window.__sent[0].status"));
            assertEquals("仍在处理", page.evaluate("() => window.__sent[0].customerReply"));
            assertEquals(1, page.evaluate("() => window.__sent[0].expectedVersion"));
            assertEquals(9, page.evaluate("() => window.__sent[0].acknowledgedFollowupId"));
            assertTrue(page.locator("#caseMessage").textContent().contains("已保存"));
            page.locator("#caseFilters select[name=followup]").selectOption("PENDING");
            page.locator("#caseFilters button[type=submit]").click();
            page.waitForFunction("() => window.__queueUrls.some(url => url.includes('followup=PENDING'))");
            assertTrue(page.url().contains("followup=PENDING"));
            page.locator("#caseFilters select[name=status]").selectOption("OPEN");
            page.locator("#caseFilters button[type=submit]").click();
            assertTrue(page.url().contains("status=OPEN"));
            page.goBack();
            page.waitForFunction("() => document.querySelector('#caseFilters select[name=status]').value==='' && document.querySelector('#caseFilters select[name=followup]').value==='PENDING'");
            assertTrue(page.url().contains("followup=PENDING"));
        }
    }
}
