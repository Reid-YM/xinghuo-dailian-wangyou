package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.FilePayload;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/** Real native workspace with controllable transport; no production writes or uploads. */
@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class XingweiCatalogDetailsContinuityPlaywrightTest {
    private Browser chromium(Playwright p) {
        var options=new BrowserType.LaunchOptions();
        String executable=System.getenv("XINGHUO_TEST_CHROMIUM");
        if(executable!=null&&!executable.isBlank())options.setExecutablePath(Path.of(executable));
        return p.chromium().launch(options);
    }
    private Page setup(Browser browser) {
        var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");
        var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);
        var context=new Context();context.setVariables(Map.of("csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","test-csrf")));
        String html=engine.process("admin-xingwei-catalog-details",context);
        Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(1440,900));
        page.route("**/*",route->{
            String path=java.net.URI.create(route.request().url()).getPath();
            if(path.startsWith("/assets/"))route.fulfill(new Route.FulfillOptions().setPath(Path.of("src/main/resources/static"+path)));
            else if(path.startsWith("/api/site-assets/"))route.fulfill(new Route.FulfillOptions().setContentType("image/svg+xml").setBody("<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'/>") );
            else route.fulfill(new Route.FulfillOptions().setContentType("text/html").setBody(html));
        });
        page.addInitScript("""
            window.calls=[];window.serverName='目录服务';window.revision=7;window.writeMode='normal';window.uploadPending=false;
            const response=(body,status=200)=>new Response(JSON.stringify(body),{status});
            const snapshot=()=>({canManage:true,restoreAdmin:true,services:[{revision:'v'+window.revision,data:{id:1,name:window.serverName,slug:'service',gameCode:'MW4',category:'TASK',shortDesc:'服务说明',durationText:'1天',priceMode:'FIXED',priceFixed:88,status:'AVAILABLE',active:true}}],games:[],categories:[{data:{gameCode:'MW4',code:'TASK',name:'任务'}}],platforms:[{code:'PC',name:'电脑'},{code:'PS5',name:'PS5'}]});
            // The real template requires the game option as well as its category.
            const data=()=>({...snapshot(),games:[{revision:'g1',data:{code:'MW4',name:'测试游戏',active:true,businessStatus:'ACTIVE',platformCodes:[]}}]});
            window.fetch=async(url,options={})=>{
              const method=options.method||'GET',headers=Object.fromEntries(new Headers(options.headers||{}));
              window.calls.push({url,method,headers,body:options.body instanceof FormData?null:options.body?JSON.parse(options.body):null});
              if(url.endsWith('/images/service')){
                if(window.uploadPending)await new Promise(resolve=>window.finishUpload=resolve);
                return response({url:'/api/site-assets/new-cover'});
              }
              if(method==='PUT'){
                const body=JSON.parse(options.body);
                if(window.writeMode==='network')throw new TypeError('connection lost');
                if(window.writeMode==='pending')await new Promise((resolve,reject)=>{window.finishWrite=resolve;options.signal.addEventListener('abort',()=>reject(new DOMException('Aborted','AbortError')));});
                window.serverName=body.name;window.revision++;
                return response({revision:'v'+window.revision});
              }
              if(window.delayRead){window.delayRead=false;const captured=data();return await new Promise(resolve=>window.finishOldRead=(fail=false)=>resolve(fail?response({message:'过期读取失败'},503):response(captured)));}
              if(window.failRead)return response({message:'资料读取失败'},503);
              return response(data());
            };
            """);
        page.navigate("http://xinghuo.test/admin/xingwei/catalog-details");ready(page);return page;
    }
    private void ready(Page page){page.waitForFunction("()=>document.querySelector('#catalogDetailsRows').getAttribute('aria-busy')==='false'");}
    private void edit(Page page){page.locator("#catalogDetailsRows button").first().click();}
    private boolean unloadProtected(Page page){return (Boolean)page.evaluate("()=>{const e=new Event('beforeunload',{cancelable:true});dispatchEvent(e);return e.defaultPrevented;}");}
    private void submitAgain(Page page){page.evaluate("()=>document.querySelector('#catalogDetailsForm').dispatchEvent(new Event('submit',{cancelable:true,bubbles:true}))");}
    private int writeCount(Page page){return ((Number)page.evaluate("()=>window.calls.filter(c=>c.method==='PUT').length")).intValue();}

    @Test void chromiumDraftUploadAndSaveRemainSafe(){try(Playwright p=Playwright.create();Browser b=chromium(p)){drafts(b);}}
    @Test void webkitDraftUploadAndSaveRemainSafe(){try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){drafts(b);}}
    private void drafts(Browser browser){
        Page page=setup(browser);edit(page);assertFalse(unloadProtected(page));
        page.locator("#catalogDetails-name").fill("修改后的服务");page.locator("#catalogDetails-status").selectOption("BUSY");page.locator("#catalogDetails-reason").fill("更新资料");
        page.locator("#catalogDetails-upload-coverImage").setInputFiles(new FilePayload("cover.png","image/png",new byte[]{1,2,3}));
        page.onceDialog(Dialog::dismiss);page.locator("#catalogDetailsClose").click();assertTrue(page.locator("#catalogDetailsDialog").isVisible());assertTrue(unloadProtected(page));
        page.onceDialog(Dialog::dismiss);page.keyboard().press("Escape");assertEquals("修改后的服务",page.locator("#catalogDetails-name").inputValue());
        page.onceDialog(Dialog::dismiss);page.locator("#catalogDetailsSave").click();assertEquals(0,writeCount(page));
        page.evaluate("()=>window.uploadPending=true");page.getByText("上传服务封面",new Page.GetByTextOptions().setExact(true)).click();
        page.waitForFunction("()=>typeof window.finishUpload==='function'");assertTrue(page.locator("#catalogDetailsClose").isDisabled());submitAgain(page);assertEquals(0,writeCount(page));
        page.keyboard().press("Escape");assertTrue(page.locator("#catalogDetailsDialog").isVisible());
        page.evaluate("()=>window.finishUpload()");page.waitForFunction("()=>document.querySelector('#catalogDetailsFormMessage').textContent.includes('图片已上传')");assertTrue(unloadProtected(page));
        page.onceDialog(Dialog::dismiss);page.locator("#catalogDetailsClose").click();assertEquals("/api/site-assets/new-cover",page.locator("#catalogDetails-coverImage").inputValue());
        page.evaluate("()=>window.writeMode='pending'");page.locator("#catalogDetailsSave").click();page.waitForFunction("()=>typeof window.finishWrite==='function'");
        submitAgain(page);assertEquals(1,writeCount(page));assertTrue(page.locator("#catalogDetailsClose").isDisabled());assertTrue(page.locator("#catalogDetails-name").isDisabled());
        page.setViewportSize(390,844);assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));page.locator("#catalogDetailsSave").scrollIntoViewIfNeeded();page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/catalog-details-saving-mobile.png")));
        page.evaluate("()=>window.finishWrite()");page.waitForFunction("()=>!document.querySelector('#catalogDetailsDialog').open");ready(page);assertTrue(page.locator("#catalogDetailsRows").textContent().contains("修改后的服务"));
        assertEquals(true,page.evaluate("()=>{const c=window.calls.find(c=>c.method==='PUT');return c.headers['if-match']==='\"v7\"'&&c.headers['x-csrf-token']==='test-csrf'&&c.body.coverImage==='/api/site-assets/new-cover'&&c.body.status==='BUSY';}"));
        edit(page);assertFalse(unloadProtected(page));page.locator("#catalogDetails-name").fill("未知结果草稿");page.locator("#catalogDetails-reason").fill("另一次修改");page.evaluate("()=>window.writeMode='network'");page.locator("#catalogDetailsSave").click();
        page.waitForFunction("()=>document.querySelector('#catalogDetailsFormMessage').textContent.includes('保存结果尚未确认')");assertTrue(page.locator("#catalogDetailsSave").isDisabled());assertEquals("未知结果草稿",page.locator("#catalogDetails-name").inputValue());
        assertTrue(page.getByText("上传服务封面",new Page.GetByTextOptions().setExact(true)).isDisabled());assertTrue(page.locator("#catalogDetails-upload-coverImage").isDisabled());int before=writeCount(page);submitAgain(page);assertEquals(before,writeCount(page));assertEquals(0,page.locator("#catalogDetailsRows button").count());
        page.onceDialog(Dialog::accept);page.locator("#catalogDetailsClose").click();page.locator("#catalogDetailsReload").click();ready(page);assertTrue(page.locator("#catalogDetailsRows").textContent().contains("修改后的服务"));
        edit(page);page.locator("#catalogDetails-name").fill("请求超时草稿");page.locator("#catalogDetails-reason").fill("验证超时保护");page.evaluate("()=>window.writeMode='pending'");page.locator("#catalogDetailsSave").click();
        page.waitForFunction("()=>document.querySelector('#catalogDetailsFormMessage').textContent.includes('保存结果尚未确认')");assertTrue(page.locator("#catalogDetailsSave").isDisabled());assertFalse(page.locator("#catalogDetailsClose").isDisabled());
        page.onceDialog(Dialog::accept);page.locator("#catalogDetailsClose").click();page.locator("#catalogDetailsReload").click();ready(page);
        page.locator("#catalogDetailsMode").selectOption("games");edit(page);assertFalse(unloadProtected(page));page.locator("#catalogDetails-featured").check();assertTrue(unloadProtected(page));page.locator("#catalogDetails-featured").uncheck();assertFalse(unloadProtected(page));
        page.locator("#catalogDetails-platformCodes").selectOption(new String[]{"PC","PS5"});assertTrue(unloadProtected(page));page.onceDialog(Dialog::dismiss);page.locator("#catalogDetailsClose").click();assertTrue(page.locator("#catalogDetailsDialog").isVisible());page.onceDialog(Dialog::accept);page.locator("#catalogDetailsClose").click();page.close();
    }
    @Test void chromiumReadsDiscardStaleResultsAndRecover(){try(Playwright p=Playwright.create();Browser b=chromium(p)){reads(b);}}
    @Test void webkitReadsDiscardStaleResultsAndRecover(){try(Playwright p=Playwright.create();Browser b=p.webkit().launch()){reads(b);}}
    private void reads(Browser browser){
        Page page=setup(browser);page.evaluate("()=>window.delayRead=true");page.locator("#catalogDetailsReload").click();page.waitForFunction("()=>typeof window.finishOldRead==='function'");assertEquals(0,page.locator("#catalogDetailsRows button").count());
        page.evaluate("()=>window.serverName='最新资料'");page.locator("#catalogDetailsReload").click();ready(page);assertTrue(page.locator("#catalogDetailsRows").textContent().contains("最新资料"));
        page.evaluate("()=>window.finishOldRead(true)");page.waitForFunction("()=>document.querySelector('#catalogDetailsRows').getAttribute('aria-busy')==='false'");assertFalse(page.locator("#catalogDetailsMessage").textContent().contains("过期"));
        page.evaluate("()=>window.failRead=true");page.locator("#catalogDetailsReload").click();ready(page);assertEquals(0,page.locator("#catalogDetailsRows button").count());assertTrue(page.locator("#catalogDetailsRows").textContent().contains("读取失败"));assertTrue(page.locator("#catalogDetailsBulk").isHidden());
        page.evaluate("()=>window.failRead=false");page.locator("#catalogDetailsReload").click();ready(page);edit(page);page.locator("#catalogDetails-name").fill("保存成功但刷新失败");page.locator("#catalogDetails-reason").fill("修改并核对");
        page.evaluate("()=>window.failRead=true");page.locator("#catalogDetailsSave").click();page.waitForFunction("()=>document.querySelector('#catalogDetailsMessage').textContent.includes('已保存，但列表刷新失败')");assertTrue(page.locator("#catalogDetailsDialog").isHidden());assertEquals(0,page.locator("#catalogDetailsRows button").count());
        page.evaluate("()=>window.failRead=false");page.locator("#catalogDetailsReload").click();ready(page);assertTrue(page.locator("#catalogDetailsRows").textContent().contains("保存成功但刷新失败"));
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("/tmp/catalog-details-recovered-desktop.png")));page.close();
    }
}
