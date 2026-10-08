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
     page.setContent("<body data-page='home'><div id='homeHero' class='hero hero-r3'><div id='heroBackground' class='hero-bg-layer'></div><div id='heroVisual' class='hero-visual'><img alt='test'></div></div></body>");
     for(String file:new String[]{"app.css","site-experience-v2.css","site-image-controls.css"})page.addStyleTag(new Page.AddStyleTagOptions().setPath(A.resolve(file)));
     page.addScriptTag(new Page.AddScriptTagOptions().setPath(A.resolve("site-image-controls.js")));
     page.evaluate("""
       () => {const c={heroImageHeight:110,heroImageFit:'contain',heroImagePositionX:15,heroImagePositionY:80,heroBackgroundOpacity:0,heroBackgroundFit:'contain',heroBackgroundPositionX:10,heroBackgroundPositionY:90,heroBackgroundBlur:3};
       SiteImageControls.main(document.querySelector('#heroVisual'),document.querySelector('img'),c);
       SiteImageControls.background(document.querySelector('#homeHero'),document.querySelector('#heroBackground'),c,'/image.png');}
       """);
     assertEquals("110px",page.evaluate("getComputedStyle(document.querySelector('#heroVisual')).height"));
     assertEquals("contain",page.evaluate("getComputedStyle(document.querySelector('img')).objectFit"));
     assertEquals("15% 80%",page.evaluate("getComputedStyle(document.querySelector('img')).objectPosition"));
     assertEquals("contain",page.evaluate("getComputedStyle(document.querySelector('#heroBackground')).backgroundSize"));
     assertEquals("10% 90%",page.evaluate("getComputedStyle(document.querySelector('#heroBackground')).backgroundPosition"));
     assertEquals("0",page.evaluate("getComputedStyle(document.querySelector('#heroBackground')).opacity"));
     page.close();
    }
   }
  }
 }
}
