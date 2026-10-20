package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class VisualComponentsPlaywrightTest {
    interface Check {void run(Browser browser)throws Exception;}
    void both(Check check)throws Exception {
        try(Playwright p=Playwright.create();Browser chrome=p.chromium().launch();Browser webkit=p.webkit().launch()) {
            for(Browser browser:List.of(chrome,webkit))check.run(browser);
        }
    }
    @Test void serviceCardsStayAlignedOnHoverAndShowKeyboardFocus()throws Exception {
        both(browser->{for(int width:new int[]{390,1280})try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,width,false)) {
            f.open("/services",".serviceCard");
            f.page.locator(".page.active").evaluate("async el=>{await Promise.all(el.getAnimations().map(a=>a.finished.catch(()=>{})));}");
            Locator card=f.page.locator(".serviceCard").first(),button=card.locator(".btn").first();
            Number before=(Number)card.evaluate("el=>el.getBoundingClientRect().top+scrollY");
            card.hover();
            assertEquals("none",card.evaluate("el=>getComputedStyle(el).transform"));
            assertEquals(before.doubleValue(),((Number)card.evaluate("el=>el.getBoundingClientRect().top+scrollY")).doubleValue(),.1);
            button.hover();assertEquals("none",button.evaluate("el=>getComputedStyle(el).transform"));
            assertTrue(((Number)button.evaluate("el=>el.getBoundingClientRect().height")).doubleValue()>=44);
            assertTrue(Double.parseDouble(String.valueOf(button.evaluate("el=>getComputedStyle(el).fontSize")).replace("px",""))>=13);
            f.page.keyboard().press("Tab");button.focus();
            assertEquals(true,button.evaluate("el=>document.activeElement===el"));
            assertEquals("solid",button.evaluate("el=>getComputedStyle(el).outlineStyle"));
            f.page.waitForFunction("()=>getComputedStyle(document.querySelector('.serviceCard')).borderTopColor===getComputedStyle(document.activeElement).outlineColor");
            assertEquals(List.of(),f.errors);
        }});
    }
    @Test void selectedCategoryRemainsVisibleWhileItsHeadingStaysFixed()throws Exception {
        both(browser->{for(int width:new int[]{390,1280})try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,width,false)) {
            String categories=java.util.stream.IntStream.range(0,20).mapToObj(i->"{\"code\":\"CAT"+i+"\",\"gameCode\":\"CODMW4\",\"name\":\"分类 "+i+"\"}").collect(java.util.stream.Collectors.joining(","));
            String services=java.util.stream.IntStream.range(0,20).mapToObj(i->FrontendCanonicalRoutesPlaywrightTest.SERVICES.strip().replaceAll("^\\[|\\]$","").replace("\"category\":\"LEVEL\"","\"category\":\"CAT"+i+"\"")).collect(java.util.stream.Collectors.joining(",","[","]"));
            f.page.route("**/api/catalog",r->f.json(r,200,FrontendCanonicalRoutesPlaywrightTest.CATALOG.replace("{\"code\":\"LEVEL\",\"gameCode\":\"CODMW4\",\"name\":\"等级服务\"}",categories)));
            f.page.route("**/api/services",r->f.json(r,200,services));
            f.open("/services?category=cat19",".serviceCard:visible");
            Locator heading=f.page.locator(".services .side>b");
            Number left=(Number)heading.evaluate("el=>el.getBoundingClientRect().left");
            assertCategoryVisible(f.page,"cat19");
            if(width==1280)assertEquals(true,f.page.locator(".categoryOptions").evaluate("el=>el.scrollHeight>el.clientHeight&&el.clientHeight<=innerHeight-240"));
            f.page.locator(".cat[data-cat=all]").click();
            f.page.locator(".cat[data-cat=cat19]").click();
            assertEquals(left.doubleValue(),((Number)heading.evaluate("el=>el.getBoundingClientRect().left")).doubleValue(),.1);
            assertCategoryVisible(f.page,"cat19");
            assertEquals(1,f.page.locator(".serviceCard:visible").count());
            assertEquals(true,f.page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
        }});
    }
    private void assertCategoryVisible(Page page,String key) {
        page.waitForFunction("""
            key=>{const rail=document.querySelector('.categoryOptions'),item=rail.querySelector('[data-cat="'+key+'"]');
              const r=item.getBoundingClientRect(),b=rail.getBoundingClientRect();
              return item.getAttribute('aria-pressed')==='true'&&r.left>=b.left-1&&r.right<=b.right+1&&r.top>=b.top-1&&r.bottom<=b.bottom+1;}
            """,key);
    }
    @Test void adminModalCloseControlsStaySquareAndKeyboardVisible()throws Exception {
        both(browser->{try(Page page=browser.newPage()) {
            page.route("**/*",r->r.abort());page.setViewportSize(320,480);
            page.setContent(SitewideDeviceAdaptationPlaywrightTest.adminDocument(Path.of("src/main/resources/templates/admin-xingwei-business.html")));
            page.locator("dialog").first().evaluate("el=>el.showModal()");
            Locator close=page.locator(".xw-dialog-close:visible").first();
            assertEquals(true,close.evaluate("el=>{const r=el.getBoundingClientRect();return r.width>=44&&r.height>=44;}"));
            page.keyboard().press("Tab");close.focus();
            assertEquals(true,close.evaluate("el=>document.activeElement===el"));
            assertEquals("solid",close.evaluate("el=>getComputedStyle(el).outlineStyle"));
            assertEquals("2px",close.evaluate("el=>getComputedStyle(el).outlineWidth"));
        }});
    }

    @Test void compactDesktopNavigationKeepsPublishedLabelsOnOneLine()throws Exception {
        both(browser->{try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,1024,false)) {
            f.open("/services",".serviceCard");
            f.page.locator(".nav .navlink").evaluateAll("(els,labels)=>els.forEach((e,i)=>e.textContent=labels[i])",List.of("首页","服务","询价","个人中心","攻略","MW4 工具站","业务记录","一对一咨询"));
            f.page.locator(".nav .brand span").evaluate("el=>el.textContent='星火网游代练平台'");
            for(int width:new int[]{1024,1180,1280,1440}) {
                f.page.setViewportSize(width,900);
                assertEquals(List.of(),f.page.evaluate("""
                    ()=>{const b=document.querySelector('.navlinks').getBoundingClientRect();
                      return [...document.querySelectorAll('.nav .navlink')].filter(e=>{
                        const r=e.getBoundingClientRect(),range=document.createRange();range.selectNodeContents(e);
                        return r.left<b.left-1||r.right>b.right+1||new Set([...range.getClientRects()].filter(x=>x.width).map(x=>x.top)).size>1;
                      }).map(e=>e.textContent);}
                    """),"width "+width);
                assertEquals(true,f.page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
            }
        }});
    }
}
