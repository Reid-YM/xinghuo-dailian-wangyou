package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class FrontendShellOverflowPlaywrightTest {
    @Test void actualShellContainsLongCatalogTextFormsAndScrollableAccountTables() throws Exception {
        String html=Files.readString(Path.of("src/main/resources/static/next/index.html"))
            .replace("<head>","<head><base href='https://example.test/'>")
            .replaceAll("(?s)<script\\b[^>]*>.*?</script>","");
        try(Playwright p=Playwright.create();Browser chromium=p.chromium().launch();Browser webkit=p.webkit().launch()) {
            for(Browser browser:List.of(chromium,webkit)) for(int width:new int[]{320,390,768,1440}) {
                Page page=browser.newPage(new Browser.NewPageOptions().setViewportSize(width,900));
                page.route("**/*",route->{
                    var path=java.net.URI.create(route.request().url()).getPath();
                    if(path.equals("/assets/brand-logo.svg"))route.fulfill(new Route.FulfillOptions().setContentType("image/svg+xml").setPath(Path.of("src/main/resources/static/assets/brand-logo.svg")));
                    else route.fulfill(new Route.FulfillOptions().setStatus(404));
                });
                page.setContent(html);
                page.waitForFunction("()=>document.querySelector('.brand-logo').naturalWidth>0");
                assertEquals(32.0,((Number)page.locator(".brand-logo").evaluate("e=>e.getBoundingClientRect().width")).doubleValue(),0.5);
                page.evaluate("""
                    () => {
                      const word='LONGCUSTOMERSERVICETITLE0123456789'.repeat(12);
                      document.querySelector('.brand [data-config=brandName]').textContent=word;
                      document.querySelector('[data-page=account] .shell').innerHTML='<div class=account><aside class=menu>个人中心</aside><div class=tablebox><table class=data><tr><td>'+word+'</td></tr></table></div></div>';
                      document.querySelectorAll('.page h1,.page h3,.page p,.sumrow b,.stat strong').forEach(e=>e.textContent=word);
                      document.querySelectorAll('.page select').forEach(e=>e.replaceChildren(new Option(word,word)));
                      document.querySelectorAll('.page input').forEach(e=>e.value=word);
                    }
                    """);
                assertTrue((Boolean)page.locator(".brand").evaluate("e=>{const b=e.getBoundingClientRect(),n=e.closest('.nav').getBoundingClientRect();return b.top>=n.top && b.bottom<=n.bottom && b.right<=n.right}"),"long configured brand must fit inside header");
                for(String name:List.of("home","services","quote","account")) {
                    page.evaluate("name=>document.querySelectorAll('.page').forEach(e=>e.classList.toggle('active',e.dataset.page===name))",name);
                    page.waitForTimeout(450);
                    Number overflow=(Number)page.evaluate("()=>document.documentElement.scrollWidth-document.documentElement.clientWidth");
                    assertTrue(overflow.doubleValue()<=1,browser.browserType().name()+" "+width+" "+name+" overflow="+overflow+" "+page.evaluate("()=>[...document.querySelectorAll('.page.active *')].filter(e=>e.getBoundingClientRect().right>innerWidth+1 || e.scrollWidth>e.clientWidth+1).slice(0,10).map(e=>e.tagName+'.'+e.className+':'+Math.round(e.getBoundingClientRect().width)+':scroll'+e.scrollWidth+':client'+e.clientWidth).join(',')"));
                    assertTrue((Boolean)page.evaluate("""
                        () => [...document.querySelectorAll('.page.active .tablebox')].every(e=>e.getBoundingClientRect().right<=innerWidth+1 && getComputedStyle(e).overflowX==='auto')
                        """));
                }
                page.evaluate("()=>document.querySelectorAll('.page').forEach(e=>e.classList.toggle('active',e.dataset.page==='quote'))");
                var input=page.locator(".page[data-page=quote] .field input:not([type=file])").first();
                input.focus();input.press("End");
                assertEquals("solid",input.evaluate("e=>getComputedStyle(e.closest('.field')).outlineStyle"));
                assertTrue((Boolean)input.evaluate("e=>{const r=e.getBoundingClientRect();return r.left>=0&&r.right<=innerWidth+1&&e.closest('.field').scrollLeft===0}"),"long input must stay visible and focusable without panning its field");
                assertTrue((Boolean)page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
                page.close();
            }
        }
    }
}
