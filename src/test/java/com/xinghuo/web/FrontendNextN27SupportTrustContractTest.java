package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN27SupportTrustContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void explicitPreviewRoutesDelegateToSharedNextDocument() throws Exception {
        String controller = read("java/com/xinghuo/web/FrontendNextN27SupportTrustController.java");
        assertTrue(controller.contains("@GetMapping({\"/next/contact\", \"/next/trust\"})"));
        assertTrue(controller.contains("preview.frontendNext(request)"));
    }

    @Test
    void supportAndTrustReusePublishedConfigurationAndReadOnlyCases() throws Exception {
        String runtime = read("resources/static/next/n27-support-trust.js");

        assertTrue(runtime.contains("configuration?.contactChannels"));
        assertTrue(runtime.contains("configuration?.wecomKefuUrl"));
        assertTrue(runtime.contains("configuration?.trustTags"));
        assertTrue(runtime.contains("configuration?.trustCases"));
        assertTrue(runtime.contains("configuration?.trustFaq"));
        assertTrue(runtime.contains("fetch('/api/public/cases'"));
        assertTrue(runtime.contains("method: 'GET'"));
        assertTrue(runtime.contains("credentials: 'same-origin'"));
        assertTrue(runtime.contains("cache: 'no-store'"));
        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
    }

    @Test
    void renderedContentUsesTextDomAndSafeSupportLinks() throws Exception {
        String runtime = read("resources/static/next/n27-support-trust.js");

        assertTrue(runtime.contains("element.textContent = String(value)"));
        assertTrue(runtime.contains("url.protocol === 'https:'"));
        assertTrue(runtime.contains("!url.username && !url.password"));
        assertTrue(runtime.contains("link.dataset.supportDirect = ''"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));
        assertFalse(runtime.contains("document.write"));
    }

    @Test
    void contactAndTrustStayInsideNextWithExplicitLegacyEscapes() throws Exception {
        String runtime = read("resources/static/next/n27-support-trust.js");
        String routes = read("resources/static/next/n14-route-continuity.js");

        assertTrue(runtime.contains("legacy.href = `/${page}`"));
        assertTrue(runtime.contains("legacy.dataset.n27Legacy = '1'"));
        assertTrue(runtime.contains("canonical.href = `https://wydlgf.cn/${page}`"));
        assertTrue(runtime.contains("robots.content = 'noindex,follow'"));
        assertTrue(routes.contains("nextUrl(url, '/next/rules')"));
        assertTrue(routes.contains("nextUrl(url, '/next/privacy')"));

        assertTrue(routes.contains("url.pathname === '/contact'"));
        assertTrue(routes.contains("nextUrl(url, '/next/contact')"));
        assertTrue(routes.contains("url.pathname === '/trust'"));
        assertTrue(routes.contains("nextUrl(url, '/next/trust')"));
        assertTrue(routes.contains("stageSource('/next/n27-support-trust.js')"));
        assertTrue(routes.contains("link.dataset.n27Legacy === '1'"));
    }
}
