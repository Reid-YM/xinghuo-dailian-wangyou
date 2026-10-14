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
                    <form id="caseFilters"><select name="status"><option value=""></option></select><select name="category"><option value=""></option></select><input name="q"></form>
                    <p id="caseMessage"></p><table><tbody id="caseRows"></tbody></table>
                    <button id="casePrev"></button><span id="casePage"></span><button id="caseNext"></button>
                    <section id="caseDetail" hidden><div id="caseCurrent"></div><div id="caseEvents"></div>
                      <form id="caseUpdate" hidden><select name="status"><option value="IN_PROGRESS">处理中</option><option value="RESOLVED">已解决</option></select><textarea name="handlingNote"></textarea><textarea name="customerReply"></textarea><button type="submit">保存</button></form>
                    </section>
                    """);
            page.evaluate("""
                    () => { window.__sent=[]; window.fetch=async (url, options={}) => {
                      let data;
                      if(url==='/api/admin/xingwei/access') data={permissionCodes:['order:view','order:manage']};
                      else if(url.startsWith('/api/admin/xingwei/business/after-sales/cases?')) data={items:[{businessNo:'XW-1',record:{recordId:5,categoryLabel:'交付或进度',content:'请核对',status:'IN_PROGRESS',submittedAtMs:1000}}],totalCount:1,hasNext:false};
                      else if(url.endsWith('/history')) data={current:{recordId:5,categoryLabel:'交付或进度',content:'请核对',status:'IN_PROGRESS',version:window.__sent.length?2:1},events:[]};
                      else if(options.method==='PUT'){window.__sent.push(JSON.parse(options.body));data={status:'IN_PROGRESS',version:2};}
                      else throw new Error('unexpected '+url);
                      return new Response(JSON.stringify(data),{status:200,headers:{'Content-Type':'application/json'}});
                    }; }
                    """);
            page.addScriptTag(new Page.AddScriptTagOptions().setPath(
                    Path.of("src/main/resources/static/assets/admin-xingwei-after-sales.js")));
            page.locator("#caseRows button").click();
            page.locator("#caseUpdate option[data-same-status]").waitFor();
            page.locator("#caseUpdate select[name=status]").selectOption("IN_PROGRESS");
            page.locator("#caseUpdate textarea[name=handlingNote]").fill("内部跟进");
            page.locator("#caseUpdate textarea[name=customerReply]").fill("仍在处理");
            page.locator("#caseUpdate button[type=submit]").click();
            page.waitForFunction("() => window.__sent.length===1 && document.querySelector('#caseMessage').textContent.includes('已保存')");
            assertEquals("IN_PROGRESS", page.evaluate("() => window.__sent[0].status"));
            assertEquals("仍在处理", page.evaluate("() => window.__sent[0].customerReply"));
            assertEquals(1, page.evaluate("() => window.__sent[0].expectedVersion"));
            assertTrue(page.locator("#caseMessage").textContent().contains("已保存"));
        }
    }
}
