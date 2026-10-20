package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS",matches="1")
class SitewideDeviceAdaptationPlaywrightTest {
    static final Path RESOURCES=Path.of("src/main/resources");
    static String adminDocument(Path path)throws Exception {
        String html=Files.readString(path);
        for(String fragment:List.of("guide-workspace","frontend-control-workspace"))
            html=html.replace("<main th:replace=\"~{fragments/"+fragment+" :: workspace}\"></main>",Files.readString(RESOURCES.resolve("templates/fragments/"+fragment+".html")));
        html=html.replaceAll("(?s)<script\\b[^>]*>.*?</script>","");
        Matcher links=Pattern.compile("<link[^>]*href=\"(/assets/[^\"]+\\.css)(?:\\?[^\"]*)?\"[^>]*>").matcher(html);
        StringBuffer result=new StringBuffer();
        while(links.find())links.appendReplacement(result,Matcher.quoteReplacement("<style>"+Files.readString(RESOURCES.resolve("static"+links.group(1)))+"</style>"));
        links.appendTail(result);return result.toString();
    }
    @Test void everyAdminWorkspaceReflowsAtPhoneTabletAndDesktopSizes()throws Exception {
        try(Playwright p=Playwright.create();Browser chrome=p.chromium().launch();Browser webkit=p.webkit().launch()) {
          for(Browser browser:List.of(chrome,webkit)){
            List<String> problems=new ArrayList<>();
            try(var files=Files.list(RESOURCES.resolve("templates"))) {
                for(Path file:files.filter(x->x.getFileName().toString().matches("admin-(xingwei.*|login)\\.html")).sorted().toList()) {
                    try(Page page=browser.newPage()) {
                        page.route("**/*",r->r.abort());page.setContent(adminDocument(file));
                        for(int width:new int[]{320,390,768,1024,1440,1920}) {
                            page.setViewportSize(width,width<768?780:900);
                            Number excess=(Number)page.evaluate("()=>document.documentElement.scrollWidth-innerWidth");
                            if(excess.doubleValue()>1)problems.add(file.getFileName()+" at "+width+": overflow "+excess);
                        }
                    }
                }
            }
            assertEquals(List.of(),problems);
          }
        }
    }

    @Test void publicAndSignedInPagesAdaptAcrossBreakpointsAndLandscape()throws Exception {
        try(Playwright p=Playwright.create();Browser chrome=p.chromium().launch();Browser webkit=p.webkit().launch()) {
            for(Browser browser:List.of(chrome,webkit))try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,390,true)) {
                var pages=new LinkedHashMap<String,String>();
                pages.put("/","#nextHomeGames a");pages.put("/services",".serviceCard");pages.put("/quote","#submit");
                pages.put("/guides",".n23-card");pages.put("/guides/test-guide","#n22GuideDetail h1");pages.put("/mastery","#n24ModePanel");
                pages.put("/records","#n26RecordsCenter.active");pages.put("/contact","#n27Contact.active");pages.put("/rules","#n28Legal");
                pages.put("/account/orders","#n5RecordsBody tbody tr");pages.put("/account/settings","#n6PasswordForm");
                for(var entry:pages.entrySet()) {
                    f.open(entry.getKey(),entry.getValue());
                    for(int[] size:new int[][]{{320,780},{390,844},{768,1024},{1024,768},{1440,900},{1920,1080},{844,390}}) {
                        f.page.setViewportSize(size[0],size[1]);
                        assertEquals(true,f.page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"),entry.getKey()+Arrays.toString(size));
                        assertEquals(1,f.page.getByRole(com.microsoft.playwright.options.AriaRole.MAIN).count());
                    }
                }
                assertEquals(List.of(),f.errors);
            }
        }
    }
    @Test void longAdminFormsAndModalActionsRemainReachableOnShortScreens()throws Exception {
        try(Playwright p=Playwright.create();Browser chrome=p.chromium().launch();Browser webkit=p.webkit().launch()) {
            for(Browser browser:List.of(chrome,webkit))for(String name:List.of("admin-xingwei-business.html","admin-xingwei-catalog.html","admin-xingwei-work.html","admin-xingwei-taxonomy.html","admin-xingwei-channels.html"))try(Page page=browser.newPage()) {
                page.route("**/*",r->r.abort());page.setContent(adminDocument(RESOURCES.resolve("templates/"+name)));
                page.setViewportSize(320,480);
                page.evaluate("""
                    ()=>{for(const input of document.querySelectorAll('input:not([type=hidden]):not([type=file]),textarea'))input.value='long-customer-content-'.repeat(20);
                      for(const heading of document.querySelectorAll('dialog h2,dialog h3'))heading.textContent='LongPublishedTitle'.repeat(15);}
                    """);
                for(Locator dialog:page.locator("dialog").all()) {
                    dialog.evaluate("el=>el.showModal()");
                    assertEquals(true,dialog.evaluate("el=>{const r=el.getBoundingClientRect();return r.left>=0&&r.right<=innerWidth+1&&r.top>=0&&r.bottom<=innerHeight+1;}"),name);
                    for(Locator control:dialog.locator("input:visible:not([type=checkbox]):not([type=radio]),select:visible,textarea:visible").all())
                        assertEquals("16px",control.evaluate("el=>getComputedStyle(el).fontSize"));
                    Locator last=dialog.locator("button:visible").last();
                    if(last.count()>0){last.scrollIntoViewIfNeeded();assertEquals(true,last.evaluate("el=>{const r=el.getBoundingClientRect();return r.top>=0&&r.bottom<=innerHeight+1&&r.right<=innerWidth+1;}"),name);}
                    dialog.evaluate("el=>el.close()");
                }
            }
        }
    }

    @Test void serviceCategoriesAndOperationalTablesScrollWithoutMovingTheDocument()throws Exception {
        try(Playwright p=Playwright.create();Browser chrome=p.chromium().launch();Browser webkit=p.webkit().launch()) {
            for(Browser browser:List.of(chrome,webkit)) {
                try(var f=new FrontendCanonicalRoutesPlaywrightTest.Fixture(browser,390,false)) {
                    String categories=java.util.stream.IntStream.range(0,20).mapToObj(i->"{\"code\":\"CAT"+i+"\",\"gameCode\":\"CODMW4\",\"name\":\"分类 "+i+"\"}").collect(java.util.stream.Collectors.joining(","));
                    f.page.route("**/api/catalog",r->f.json(r,200,FrontendCanonicalRoutesPlaywrightTest.CATALOG.replace("{\"code\":\"LEVEL\",\"gameCode\":\"CODMW4\",\"name\":\"等级服务\"}",categories)));
                    String services=java.util.stream.IntStream.range(0,20).mapToObj(i->FrontendCanonicalRoutesPlaywrightTest.SERVICES.strip().replaceAll("^\\[|\\]$","").replace("\"category\":\"LEVEL\"","\"category\":\"CAT"+i+"\"")).collect(java.util.stream.Collectors.joining(",","[","]"));
                    f.page.route("**/api/services",r->f.json(r,200,services));
                    f.open("/services",".serviceCard");
                    assertEquals(true,f.page.locator(".services .side").evaluate("el=>el.scrollWidth>el.clientWidth&&el.getBoundingClientRect().height<120"));
                    f.page.locator(".cat[data-cat=cat19]").click();
                    assertEquals("true",f.page.locator(".cat[data-cat=cat19]").getAttribute("aria-pressed"));
                    assertEquals(1,f.page.locator(".serviceCard:visible").count());
                    assertEquals(true,f.page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
                }
                try(Page page=browser.newPage()) {
                    page.route("**/*",r->r.abort());page.setViewportSize(320,480);
                    page.setContent(adminDocument(RESOURCES.resolve("templates/admin-xingwei-after-sales.html")));
                    Locator table=page.locator(".xw-table-wrap").first();
                    assertEquals(true,table.evaluate("el=>el.scrollWidth>el.clientWidth"));
                    table.focus();table.press("ArrowRight");table.press("ArrowRight");
                    page.waitForFunction("()=>document.querySelector('.xw-table-wrap').scrollLeft>0");
                    assertEquals(true,page.evaluate("()=>document.documentElement.scrollWidth<=innerWidth+1"));
                }
            }
        }
    }
}
