package com.xinghuo.web;
import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="PLAYWRIGHT_UI_TESTS", matches="1")
class ImageControlsPlaywrightTest {
 private static final Path A=Path.of("src/main/resources/static/assets");
 @Test void imageParametersWinAtDesktopAndMobileWidths() throws Exception {
  try(var pw=Playwright.create()) {
   for(var type:new BrowserType[]{pw.chromium(),pw.webkit()}) try(var browser=type.launch()) {
    for(int width:new int[]{390,760,1366,1920}) {
     var page=browser.newPage(new Browser.NewPageOptions().setViewportSize(width,900));
     page.setContent("<body data-page='home'><div id='homeHero' class='hero hero-r3'><div id='heroBackground' class='hero-bg-layer'></div><div id='heroVisual' class='hero-visual'><img alt='test'></div></div><div class='service-cover'><img alt='cover'></div></body>");
     for(String file:new String[]{"app.css","site-experience-v2.css","site-image-controls.css"})page.addStyleTag(new Page.AddStyleTagOptions().setPath(A.resolve(file)));
     page.addScriptTag(new Page.AddScriptTagOptions().setPath(A.resolve("site-image-controls.js")));
     page.evaluate("""
       () => {const c={heroImageHeight:110,heroImageFit:'contain',heroImagePositionX:15,heroImagePositionY:80,heroImageMobileHeight:170,heroImageMobileFit:'cover',heroImageMobilePositionX:60,heroImageMobilePositionY:35,heroBackgroundOpacity:0,heroBackgroundFit:'contain',heroBackgroundPositionX:10,heroBackgroundPositionY:90,heroBackgroundBlur:3,heroBackgroundMobileFit:'cover',heroBackgroundMobilePositionX:70,heroBackgroundMobilePositionY:25,serviceCoverHeight:135,serviceCoverFit:'contain',serviceCoverPositionX:20,serviceCoverPositionY:30,serviceCoverMobileHeight:150,serviceCoverMobileFit:'cover',serviceCoverMobilePositionX:80,serviceCoverMobilePositionY:40};
       SiteImageControls.main(document.querySelector('#heroVisual'),document.querySelector('#heroVisual img'),c);
       const cover=SiteImageControls.serviceCoverVars(c);
       document.documentElement.style.setProperty('--service-cover-height',cover.height);
       document.documentElement.style.setProperty('--service-cover-fit',cover.fit);
       document.documentElement.style.setProperty('--service-cover-position',cover.position);
       SiteImageControls.background(document.querySelector('#homeHero'),document.querySelector('#heroBackground'),c,'/image.png');}
       """);
     boolean mobile=width<768;
     assertEquals(mobile?"170px":"110px",page.evaluate("getComputedStyle(document.querySelector('#heroVisual')).height"));
     assertEquals(mobile?"cover":"contain",page.evaluate("getComputedStyle(document.querySelector('img')).objectFit"));
     assertEquals(mobile?"60% 35%":"15% 80%",page.evaluate("getComputedStyle(document.querySelector('img')).objectPosition"));
     assertEquals(mobile?"cover":"contain",page.evaluate("getComputedStyle(document.querySelector('#heroBackground')).backgroundSize"));
     assertEquals(mobile?"70% 25%":"10% 90%",page.evaluate("getComputedStyle(document.querySelector('#heroBackground')).backgroundPosition"));
     assertEquals(mobile?"0.78":"0",page.evaluate("getComputedStyle(document.querySelector('#heroBackground')).opacity"));
     assertEquals(mobile?"150px":"135px",page.evaluate("getComputedStyle(document.querySelector('.service-cover')).height"));
     assertEquals(mobile?"cover":"contain",page.evaluate("getComputedStyle(document.querySelector('.service-cover img')).objectFit"));
     page.close();
    }
   }
  }
 }
 @Test void adminEditorKeepsFieldsAccessibleAndTracksDrafts() throws Exception {
  String html=Files.readString(Path.of("src/main/resources/templates/admin.html"));
  int start=html.indexOf("<div class=\"config-group hidden\" data-config-section=\"visual\">");
  int end=html.indexOf("<div class=\"config-group hidden\" data-config-section=\"notice\">",start);
  String editor=html.substring(start,end).replace("config-group hidden","config-group");
  try(var pw=Playwright.create();var browser=pw.chromium().launch()) {
   for(int width:new int[]{390,1366,1920}) {
    var page=browser.newPage(new Browser.NewPageOptions().setViewportSize(width,900));
    page.setContent("<body class='admin-control-center-v2'><form id='configForm'>"+editor+"</form></body>");
    for(String file:new String[]{"app.css","admin-control-center-v2.css","site-image-controls.css"})page.addStyleTag(new Page.AddStyleTagOptions().setPath(A.resolve(file)));
    for(String file:new String[]{"site-image-controls.js","admin-image-editor.js"})page.addScriptTag(new Page.AddScriptTagOptions().setPath(A.resolve(file)));
    page.evaluate("document.dispatchEvent(new CustomEvent('site-config-loaded'))");
    page.getByLabel("手机主图高度（px）").fill("180");
    assertEquals("参数有未保存修改",page.locator("#visualSaveState").innerText());
    page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,new Page.GetByRoleOptions().setName("手机").setExact(true)).click();
    assertEquals("mobile",page.locator("#heroBackgroundLivePreview").getAttribute("data-device"));
    assertTrue((Boolean)page.evaluate("document.documentElement.scrollWidth <= innerWidth+1"));
    assertEquals(3,page.locator(".visual-editor-card").count());
    page.close();
   }
  }
 }

}
