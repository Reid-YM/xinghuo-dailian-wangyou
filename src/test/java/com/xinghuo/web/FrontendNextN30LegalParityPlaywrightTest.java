package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextN30LegalParityPlaywrightTest {
    @Test void bothLanguagesRetainEveryFormalParagraphNoticeAndConfiguredIdentity() throws Exception {
        var preview = new FrontendNextPreviewController();
        var mvc = MockMvcBuilders.standaloneSetup(preview, new FrontendNextN28LegalController(preview)).build();
        try (Playwright p = Playwright.create(); Browser chromium = p.chromium().launch(); Browser webkit = p.webkit().launch()) {
            for (Browser browser : List.of(chromium, webkit)) {
                Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(390, 844));
                Map<String, String> sources = new HashMap<>();
                for (String doc : List.of("rules", "privacy", "rules-en", "privacy-en"))
                    sources.put("/" + doc, Files.readString(Path.of("src/main/resources/templates/" + doc + ".html")));
                page.route("**/*", route -> {
                    String path = URI.create(route.request().url()).getPath();
                    if (path.startsWith("/api/public/legal/") && sources.containsKey(path.substring("/api/public/legal".length()))) route.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setBody(sources.get(path.substring("/api/public/legal".length()))));
                    else if (path.equals("/api/site-config")) route.fulfill(new Route.FulfillOptions().setContentType("application/json").setBody(
                            "{\"brandName\":\"测试星火\",\"legalEntityName\":\"验收测试主体\",\"wechatId\":\"support-test\"}"));
                    else FrontendNextN30DocumentJourneyPlaywrightTest.fulfill(route, mvc, new HashSet<>());
                });
                try {
                    for (String doc : List.of("rules", "privacy", "rules-en", "privacy-en")) {
                        page.navigate("https://example.test/next/" + doc);
                        page.waitForSelector("#n28Legal .n28-document");
                        String displayed = (String) page.locator(".n28-document").evaluate("e => e.textContent.replace(/\\s+/g,' ').trim()");
                        @SuppressWarnings("unchecked")
                        List<String> paragraphs = (List<String>) page.evaluate("""
                            source => {
                              const doc = new DOMParser().parseFromString(source,'text/html');
                              doc.querySelectorAll('[data-config=legalEntityName]').forEach(e=>e.textContent='验收测试主体');
                              doc.querySelectorAll('[data-contact=wechat]').forEach(e=>e.textContent='support-test');
                              return [...doc.querySelectorAll('.rules-card > .rule-alert,.rules-card > section h2,.rules-card > section p,.rules-card > section li')]
                                .map(e=>e.textContent.replace(/\\s+/g,' ').trim()).filter(Boolean);
                            }
                            """, sources.get("/" + doc));
                        assertFalse(paragraphs.isEmpty(), "the formal document must contain legal content");
                        for (String text : paragraphs) assertTrue(displayed.contains(text), doc + " omitted: " + text);
                        assertEquals("https://wydlgf.cn/" + doc, page.locator("link[rel=canonical]").getAttribute("href"));
                        assertEquals(doc.endsWith("-en") ? "en" : "zh-CN", page.locator(".n28-document").getAttribute("lang"));
                        assertTrue(displayed.contains("验收测试主体"));
                        if (doc.startsWith("privacy")) assertTrue(displayed.contains("support-test"));
                        assertEquals(0, page.locator(".n28-document script,.n28-document style,.n28-document iframe").count());
                        assertEquals("/" + doc, page.locator("[data-n28-legacy]").getAttribute("href"));
                        assertEquals("/next/privacy-en?utm_source=legal#contact", page.evaluate(
                                "() => window.XHFrontendNextN15.safeAccountReturn('/privacy-en?utm_source=legal#contact')"));
                        assertEquals("/privacy-en?legacy=1", page.evaluate(
                                "() => window.XHFrontendNextN15.safeAccountReturn('/privacy-en?legacy=1')"));
                    }
                } finally { page.close(); }
            }
        }
    }
}
