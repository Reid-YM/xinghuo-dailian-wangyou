package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class XingweiGuideWorkspacePlaywrightTest {
    private Browser chromium(Playwright p){var options=new BrowserType.LaunchOptions();String executable=System.getenv("XINGHUO_TEST_CHROMIUM");if(executable!=null&&!executable.isBlank())options.setExecutablePath(Path.of(executable));return p.chromium().launch(options);}
    private void setup(Page page,boolean readOnly)throws Exception{setup(page,readOnly,"");}
    private void setup(Page page,boolean readOnly,String query)throws Exception{
        var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);var context=new Context(Locale.SIMPLIFIED_CHINESE);
        context.setVariable("xingweiMode",true);context.setVariable("csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","test-token"));
        String html=engine.process("admin-xingwei-guides",context);
        assertFalse(html.contains("/assets/app.css"));
        assertFalse(html.contains("admin-light-theme"));
        assertFalse(html.contains("admin-brand-layout-fixes"));
        page.route("**/*",r->{String path=java.net.URI.create(r.request().url()).getPath();if(path.startsWith("/assets/"))r.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static"+path)));else r.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setBody(html));});
        page.addInitScript("""
          (readOnly=>{window.calls=[];window.alerts=[];window.conflict=false;window.readFailure=false;window.opened='';window.alert=m=>window.alerts.push(m);window.confirm=()=>true;window.open=url=>{window.opened=url;};
            const row={id:1,gameCode:'CODMW4',gameName:'MW4',slug:'draft-guide',title:'攻略草稿',summary:'草稿摘要',status:'DRAFT',difficulty:'BEGINNER',estimatedMinutes:60,sortOrder:100,tags:[],productCount:0};
            const detail={guide:row,sections:[{id:1,sectionKey:'paragraph',heading:'正文',bodyMarkdown:'草稿内容',sortOrder:10}],products:[{serviceId:9,label:'运营标题',description:'运营说明',imageUrl:'/assets/brand-logo.svg',featured:false,sortOrder:90}],faqs:[{question:'原问题',answer:'原回答',sortOrder:10}],relatedGuides:[]};
            window.fetch=async(url,options={})=>{const method=options.method||'GET';window.calls.push({url,method,headers:options.headers,body:options.body});
              if(url.endsWith('/access'))return new Response(JSON.stringify({canManage:!readOnly}));
              if(url.endsWith('/catalog'))return new Response(JSON.stringify({games:[{code:'CODMW4',name:'MW4'}]}));
              if(url.endsWith('/services'))return new Response(JSON.stringify([{id:9,gameCode:'CODMW4',name:'目录名称',shortDesc:'目录说明'}]));
              if(method==='PUT'){
                if(window.conflict)return new Response(JSON.stringify({error:'攻略已被更新，请重新读取后核对保存'}),{status:409});
                window.savedBody=JSON.parse(options.body);row.title=window.savedBody.title;return new Response(JSON.stringify(detail),{headers:{ETag:'"guide-1-v4"'}});
              }
              if(url.endsWith('/1')&&window.readFailure)return new Response('读取失败',{status:503});
              if(url.endsWith('/1'))return new Response(JSON.stringify(detail),{headers:{ETag:'"guide-1-v3"'}});
              return new Response(JSON.stringify([row,{...row,id:2,title:'已发布进阶攻略',status:'PUBLISHED'}]));
            };
          })(READ_ONLY);
          """.replace("READ_ONLY",String.valueOf(readOnly)));
        page.navigate("https://example.test/admin/xingwei/guides"+query);
        page.waitForFunction("() => document.querySelector('#guideCount').textContent==='2'");
    }
    @Test
    void chromiumUsesNativeApisAndVersionHeadersAndRetainsConflictingDraft()throws Exception{
        try(Playwright p=Playwright.create();Browser b=chromium(p)){
            Page page=b.newPage();setup(page,false);
            assertEquals(1,page.locator("body.xw-guide-native").count());
            assertEquals(0,page.locator("a[href='/admin/guides'],a[href*='legacy=1']").count());
            assertTrue(page.locator("#guideRows").textContent().contains("草稿"));page.locator("[data-guide-id='1']").click();page.waitForFunction("() => !document.querySelector('#guideForm').classList.contains('hidden')");
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
    void chromiumFiltersRestoreAndSelectionAndRefreshKeepDraftSafe()throws Exception{
        try(Playwright p=Playwright.create();Browser b=chromium(p)){
            Page page=b.newPage();setup(page,false);
            page.locator("[data-guide-id='1']").click();
            assertTrue(page.locator("#guideForm input[name=reason]").isHidden());
            page.locator("#guideForm input[name=title]").fill("尚未保存的草稿");
            page.locator("#guideStatusFilter").selectOption("PUBLISHED");
            page.locator("#guideFilters button[type=submit]").click();
            assertEquals(1,page.locator("[data-guide-id]").count());
            page.locator("#guideSelectAll").check();
            assertEquals(true,page.evaluate("() => [...document.querySelectorAll('[data-guide-check]:checked')].map(n=>n.dataset.guideCheck).join(',')==='2'"));
            assertTrue(page.url().contains("status=PUBLISHED"));
            page.locator("#guideFilters button[type=reset]").click();
            assertEquals(2,page.locator("[data-guide-id]").count());
            assertEquals("尚未保存的草稿",page.locator("#guideForm input[name=title]").inputValue());
            page.locator("#guideQuery").fill("不存在的攻略");page.locator("#guideFilters button[type=submit]").click();
            assertTrue(page.locator("#guideRows").textContent().contains("没有符合筛选条件"));
            page.locator("#guideFilters button[type=reset]").click();
            page.evaluate("() => {window.confirm=()=>false;}");page.locator("#reloadGuides").click();
            assertEquals("尚未保存的草稿",page.locator("#guideForm input[name=title]").inputValue());
            page.evaluate("() => {window.confirm=()=>true;window.readFailure=true;}");page.locator("#reloadGuides").click();
            page.waitForFunction("() => window.alerts.includes('读取失败')");
            assertEquals("尚未保存的草稿",page.locator("#guideForm input[name=title]").inputValue());
            assertEquals(true,page.evaluate("() => {const event=new Event('beforeunload',{cancelable:true});window.dispatchEvent(event);return event.defaultPrevented;}"));
            Page restored=b.newPage();setup(restored,false,"?q=进阶&game=CODMW4&status=PUBLISHED");
            restored.waitForFunction("() => document.querySelectorAll('[data-guide-id]').length===1");
            assertEquals("PUBLISHED",restored.locator("#guideStatusFilter").inputValue());
            assertEquals("2",restored.locator("[data-guide-id]").getAttribute("data-guide-id"));
            page.locator("#guideFilters button[type=reset]").click();
            page.evaluate("() => scrollTo(0,0)");
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/xingwei-guides-desktop.png")).setFullPage(true));
        }
    }
    @Test void chromiumFullGuideMetadataFaqAndRelatedReadingsRoundTrip()throws Exception{try(Playwright p=Playwright.create();Browser b=chromium(p)){fullGuide(b);}}
    @Test void webkitFullGuideMetadataFaqAndRelatedReadingsRoundTrip()throws Exception{try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){fullGuide(b);}}
    private void fullGuide(Browser b)throws Exception{
        Page page=b.newPage();setup(page,false);page.locator("[data-guide-id='1']").click();
        page.locator("#guideForm input[name=authorName]").fill("内容编辑组");page.locator("#guideForm input[name=sourceName]").fill("游戏官方资料");page.locator("#guideForm input[name=sourceUrl]").fill("https://example.test/source");
        page.locator("#guideForm input[name=coverImage]").fill("/assets/brand-logo.svg");page.locator("#guideForm input[name=sortOrder]").fill("42");
        page.locator("#addGuideFaq").click();page.locator("[data-faq-question]").last().fill("新增问题");page.locator("[data-faq-answer]").last().fill("新增回答");page.locator("#guideRelatedIds").selectOption("2");
        page.locator("#guideForm button[type=submit]").click();page.waitForFunction("()=>!!window.savedBody");
        assertEquals(true,page.evaluate("()=>{const d=window.savedBody;return d.authorName==='内容编辑组'&&d.sourceName==='游戏官方资料'&&d.sourceUrl==='https://example.test/source'&&d.coverImage==='/assets/brand-logo.svg'&&d.sortOrder===42&&d.faqs.length===2&&d.faqs[1].answer==='新增回答'&&d.relatedGuideIds.join(',')==='2'&&d.products[0].label==='运营标题'&&d.products[0].description==='运营说明'&&d.products[0].featured===false&&d.products[0].sortOrder===90;}"));
    }
    @Test void chromiumReadOnlyEditorShowsProtectedDraftPreviewAndFitsMobile()throws Exception{try(Playwright p=Playwright.create();Browser b=chromium(p)){readOnlyMobile(b);}}
    @Test void webkitReadOnlyEditorShowsProtectedDraftPreviewAndFitsMobile()throws Exception{try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){readOnlyMobile(b);}}
    private void readOnlyMobile(Browser b)throws Exception{
        {
            Page page=b.newPage(new Browser.NewPageOptions().setViewportSize(390,844));setup(page,true);
            assertTrue(page.locator("#newGuide").isDisabled());page.locator("[data-guide-id='1']").click();page.waitForFunction("() => !document.querySelector('#guideForm').classList.contains('hidden')");
            assertTrue(page.locator("#guideForm button[type=submit]").isDisabled());page.locator("#previewGuide").click();
            assertEquals("/admin/xingwei/guides/1/preview",page.evaluate("() => window.opened"));
            assertEquals(true,page.evaluate("() => window.calls.every(c=>c.method==='GET')"));
            assertEquals(true,page.evaluate("() => document.documentElement.scrollWidth<=innerWidth"));
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/xingwei-guides-mobile.png")).setFullPage(true));
        }
    }
}
