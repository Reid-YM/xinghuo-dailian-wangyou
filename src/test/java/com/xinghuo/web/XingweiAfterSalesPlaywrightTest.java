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
    void managerCanAppendReplyWithoutChangingStatus() {
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.setContent("""
                    <meta name="_csrf" content="token"><meta name="_csrf_header" content="X-CSRF-TOKEN">
                    <form id="caseFilters"><select name="status"><option value=""></option></select><select name="category"><option value=""></option></select><select name="followup"><option value=""></option><option value="PENDING">待跟进</option></select><input name="q"><button type="submit">查询</button></form>
                    <p id="caseMessage"></p><table><tbody id="caseRows"></tbody></table>
                    <button id="casePrev"></button><span id="casePage"></span><button id="caseNext"></button>
                    <section id="caseDetail" hidden><div id="caseCurrent"></div><div id="caseFollowups"></div><div id="caseEvents"></div>
                      <form id="caseUpdate" hidden><select name="status"><option value="IN_PROGRESS">处理中</option><option value="RESOLVED">已解决</option></select><textarea name="handlingNote"></textarea><textarea name="customerReply"></textarea><button type="submit">保存</button></form>
                    </section>
                    """);
            page.evaluate("""
                    () => { window.__sent=[]; window.__queueUrls=[]; window.fetch=async (url, options={}) => {
                      let data;
                      if(url==='/api/admin/xingwei/access') data={permissionCodes:['order:view','order:manage']};
                      else if(url.startsWith('/api/admin/xingwei/business/after-sales/cases?')) {window.__queueUrls.push(url);data={items:[{businessNo:'XW-1',record:{recordId:5,categoryLabel:'交付或进度',content:'请核对',status:'IN_PROGRESS',submittedAtMs:1000},latestCustomerFollowupAtMs:2000,pendingCustomerFollowup:true}],totalCount:1,hasNext:false};}
                      else if(url.endsWith('/history')) data={current:{recordId:5,categoryLabel:'交付或进度',content:'请核对',status:'IN_PROGRESS',version:window.__sent.length?2:1},events:[],handledThroughFollowupId:0,customerFollowups:[{followupId:9,content:'客户有新补充',createdAt:'2026-10-15T03:00:00Z'}]};
                      else if(options.method==='PUT'){window.__sent.push(JSON.parse(options.body));data={status:'IN_PROGRESS',version:2};}
                      else throw new Error('unexpected '+url);
                      return new Response(JSON.stringify(data),{status:200,headers:{'Content-Type':'application/json'}});
                    }; }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(
                    Path.of("src/main/resources/static/assets/admin-xingwei-after-sales.js")));
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
        }
    }
}
