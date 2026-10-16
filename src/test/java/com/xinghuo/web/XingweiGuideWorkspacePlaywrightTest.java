package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class XingweiGuideWorkspacePlaywrightTest {
    private void setup(Page page,boolean readOnly)throws Exception{
        var engine=new SpringTemplateEngine();engine.setTemplateResolver(new StringTemplateResolver());var context=new Context(Locale.SIMPLIFIED_CHINESE);
        context.setVariable("xingweiMode",true);context.setVariable("csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","test-token"));
        String html=engine.process(Files.readString(Path.of("src/main/resources/templates/admin-guides.html")),context).replaceAll("<script[^>]*></script>","");
        page.route("**/*",r->r.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setBody(html)));
        page.navigate("https://example.test/admin/xingwei/guides");
        for(String css:new String[]{"app.css","admin-guides-workspace.css","admin-light-theme.css","admin-brand-layout-fixes.css"})page.addStyleTag(new Page.AddStyleTagOptions().setPath(Path.of("src/main/resources/static/assets/"+css)));
        page.evaluate("""
          readOnly=>{window.calls=[];window.alerts=[];window.conflict=false;window.opened='';window.alert=m=>window.alerts.push(m);window.confirm=()=>true;window.open=url=>{window.opened=url;};
            const row={id:1,gameCode:'CODMW4',gameName:'MW4',slug:'draft-guide',title:'攻略草稿',summary:'草稿摘要',status:'DRAFT',difficulty:'BEGINNER',estimatedMinutes:60,sortOrder:100,tags:[],productCount:0};
            const detail={guide:row,sections:[{id:1,sectionKey:'paragraph',heading:'正文',bodyMarkdown:'草稿内容',sortOrder:10}],products:[],faqs:[],relatedGuides:[]};
            window.fetch=async(url,options={})=>{const method=options.method||'GET';window.calls.push({url,method,headers:options.headers,body:options.body});
              if(url.endsWith('/access'))return new Response(JSON.stringify({canManage:!readOnly}));
              if(url.endsWith('/catalog'))return new Response(JSON.stringify({games:[{code:'CODMW4',name:'MW4'}]}));
              if(url.endsWith('/services'))return new Response('[]');
              if(method==='PUT'){
                if(window.conflict)return new Response(JSON.stringify({error:'攻略已被更新，请重新读取后核对保存'}),{status:409});
                window.savedBody=JSON.parse(options.body);row.title=window.savedBody.title;return new Response(JSON.stringify(detail),{headers:{ETag:'"guide-1-v4"'}});
              }
              if(url.endsWith('/1'))return new Response(JSON.stringify(detail),{headers:{ETag:'"guide-1-v3"'}});
              return new Response(JSON.stringify([row]));
            };
          }
          """,readOnly);
        page.addScriptTag(new Page.AddScriptTagOptions().setPath(Path.of("src/main/resources/static/assets/admin-guides.js")));
        page.waitForFunction("() => document.querySelector('#guideCount').textContent==='1'");
    }
    @Test
    void chromiumUsesNativeApisAndVersionHeadersAndRetainsConflictingDraft()throws Exception{
        try(Playwright p=Playwright.create();Browser b=p.chromium().launch()){
            Page page=b.newPage();setup(page,false);page.locator("[data-guide-id='1']").click();page.waitForFunction("() => !document.querySelector('#guideForm').classList.contains('hidden')");
            page.locator("#guideForm input[name=title]").fill("新的攻略标题");page.locator("#guideForm button[type=submit]").click();
            page.waitForFunction("() => window.savedBody?.title==='新的攻略标题'");
            assertEquals("\"guide-1-v3\"",page.evaluate("() => window.calls.find(c=>c.method==='PUT').headers.get('If-Match')"));
            assertEquals("test-token",page.evaluate("() => window.calls.find(c=>c.method==='PUT').headers.get('X-CSRF-TOKEN')"));
            page.waitForFunction("() => !document.querySelector('#guideForm button[type=submit]').disabled");
            page.evaluate("() => window.conflict=true");page.locator("#guideForm input[name=title]").fill("保留我的草稿");page.locator("#guideForm button[type=submit]").click();
            page.waitForFunction("() => window.alerts.some(m=>m.includes('攻略已被更新'))");
            assertEquals("保留我的草稿",page.locator("#guideForm input[name=title]").inputValue());
            assertEquals(true,page.evaluate("() => window.calls.every(c=>c.url.startsWith('/api/admin/xingwei/guides'))"));
        }
    }
    @Test
    void webkitReadOnlyEditorShowsProtectedDraftPreviewAndFitsMobile()throws Exception{
        try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){
            Page page=b.newPage(new Browser.NewPageOptions().setViewportSize(390,844));setup(page,true);
            assertTrue(page.locator("#newGuide").isDisabled());page.locator("[data-guide-id='1']").click();page.waitForFunction("() => !document.querySelector('#guideForm').classList.contains('hidden')");
            assertTrue(page.locator("#guideForm button[type=submit]").isDisabled());page.locator("#previewGuide").click();
            assertEquals("/admin/xingwei/guides/1/preview",page.evaluate("() => window.opened"));
            assertEquals(true,page.evaluate("() => window.calls.every(c=>c.method==='GET')"));
            assertEquals(true,page.evaluate("() => document.documentElement.scrollWidth<=innerWidth"));
        }
    }
}
