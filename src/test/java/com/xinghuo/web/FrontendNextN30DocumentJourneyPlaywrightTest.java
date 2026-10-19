package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.*;
import java.net.URI;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Serve actual MVC documents and all their scripts together, rather than injecting one stage. */
@EnabledIfEnvironmentVariable(named = "PLAYWRIGHT_UI_TESTS", matches = "1")
class FrontendNextN30DocumentJourneyPlaywrightTest {
    private static final Path STATIC = Path.of("src/main/resources/static");
    private static final Map<String, String> PAGES = Map.of(
            "/next/guides", "n23GuidesCenter", "/next/mastery", "n24Mastery",
            "/next/account/submit", "n25SubmissionWorkbench", "/next/records", "n26RecordsCenter",
            "/next/contact", "n27Contact", "/next/trust", "n27Trust",
            "/next/rules", "n28Legal", "/next/privacy", "n28Legal",
            "/next/guides/test-guide", "n22GuideDetail");

    @Test void fullDocumentsLoadIndependentStageDigestsOnMobileAndDesktopInBothEngines() throws Exception {
        var preview = new FrontendNextPreviewController();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(preview,
                new FrontendNextN23GuidesController(preview), new FrontendNextN24MasteryController(preview),
                new FrontendNextN25SubmissionController(preview), new FrontendNextN26RecordsController(preview),
                new FrontendNextN27SupportTrustController(preview), new FrontendNextN28LegalController(preview)).build();
        try (Playwright playwright = Playwright.create()) {
            try (Browser chromium = playwright.chromium().launch(); Browser webkit = playwright.webkit().launch()) {
                for (Browser browser : List.of(chromium, webkit)) {
                    for (int width : new int[]{320, 1280}) {
                        Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, 900));
                        List<String> errors = new ArrayList<>();
                        Set<String> stages = new HashSet<>();
                        page.onPageError(errors::add);
                        page.route("**/*", route -> fulfill(route, mvc, stages));
                        try {
                            for (var entry : PAGES.entrySet()) {
                                page.navigate("https://example.test" + entry.getKey());
                                page.waitForSelector("#" + entry.getValue() + ".active");
                                page.waitForFunction("() => document.documentElement.dataset.siteConfiguration === 'ready'");
                                assertEquals("首页", page.locator(".nav .navlink[data-go=home]").textContent());
                                assertEquals(1, page.locator(".page.active").count(), entry.getKey());
                                assertTrue(page.locator("#" + entry.getValue()).isVisible(), entry.getKey());
                                assertTrue(((String) page.locator("meta[name=robots]").getAttribute("content")).startsWith("noindex"));
                                assertEquals(Boolean.TRUE, page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"),
                                        browser.browserType().name() + " " + width + " " + entry.getKey());
                            }
                            assertTrue(stages.containsAll(List.of("n23-guides-center.js", "n24-mastery.js",
                                    "n25-submission-workbench.js", "n26-records-center.js",
                                    "n27-support-trust.js", "n28-legal-single-source.js")), stages.toString());
                            assertEquals(List.of(), errors);
                        } finally { page.close(); }
                    }
                }
            }
        }
    }

    static void fulfill(Route route, MockMvc mvc, Set<String> stages) {
        try {
            URI uri = URI.create(route.request().url());
            String path = uri.getPath();
            if (path.endsWith(".js") || path.endsWith(".css")) {
                Path file = STATIC.resolve(path.substring(1));
                if (!Files.isRegularFile(file)) { route.fulfill(new Route.FulfillOptions().setStatus(404)); return; }
                if (path.matches("/next/n2[3-8]-.*\\.js")) {
                    String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
                    assertEquals("v=" + digest, uri.getQuery(), path);
                    stages.add(file.getFileName().toString());
                }
                route.fulfill(new Route.FulfillOptions().setContentType(path.endsWith(".js") ? "application/javascript" : "text/css")
                        .setPath(file));
            } else if (path.equals("/api/site-config")) {
                json(route, 200, "{\"brandName\":\"测试星火\",\"quoteEnabled\":true}");
            } else if (path.equals("/api/catalog")) {
                json(route, 200, "{\"games\":[{\"code\":\"CODMW4\",\"name\":\"使命召唤\"}],\"categories\":[],\"platforms\":[],\"gamePlatforms\":[]}");
            } else if (path.equals("/api/tool-station")) {
                json(route, 200, "{\"games\":[{\"code\":\"CODMW4\",\"name\":\"MW4\",\"weapons\":[],\"camoTracks\":{\"order\":[\"MULTIPLAYER\"],\"modes\":{\"MULTIPLAYER\":{\"code\":\"MULTIPLAYER\",\"name\":\"多人模式\",\"items\":[]}}}}]}");
            } else if (path.equals("/api/account/me")) {
                json(route, 401, "{\"message\":\"请登录\"}");
            } else if (path.equals("/api/guides/test-guide")) {
                json(route, 200, "{\"guide\":{\"slug\":\"test-guide\",\"title\":\"测试攻略\"},\"sections\":[{\"heading\":\"正文\",\"bodyMarkdown\":\"实际内容\"}]}");
            } else if (path.startsWith("/api/guest/business/")) {
                json(route, 403, "{}");
            } else if (path.startsWith("/api/")) {
                json(route, 200, "[]");
            } else if (path.equals("/rules") || path.equals("/privacy")) {
                route.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8").setBody(
                        "<html><body><main><div class='rules-card'><h1>正式原文</h1><section><h2>内容</h2><p>测试正文</p></section></div></main></body></html>"));
            } else {
                var response = mvc.perform(get(path)).andReturn().getResponse();
                assertEquals(200, response.getStatus(), path);
                assertEquals("no-store", response.getHeader("Cache-Control"));
                assertEquals("UTF-8", response.getCharacterEncoding());
                route.fulfill(new Route.FulfillOptions().setContentType("text/html;charset=utf-8")
                        .setBody(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private static void json(Route route, int status, String body) {
        route.fulfill(new Route.FulfillOptions().setStatus(status).setContentType("application/json").setBody(body));
    }
}
