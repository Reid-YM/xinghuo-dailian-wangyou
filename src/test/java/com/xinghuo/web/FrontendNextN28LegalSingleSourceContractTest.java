package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN28LegalSingleSourceContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void explicitLegalPreviewRoutesDelegateToSharedNextDocument() throws Exception {
        String controller = read("java/com/xinghuo/web/FrontendNextN28LegalController.java");
        assertTrue(controller.contains("@GetMapping({\"/next/rules\", \"/next/privacy\", \"/next/rules-en\", \"/next/privacy-en\"})"));
        assertTrue(controller.contains("preview.frontendNext(request)"));
    }

    @Test
    void nextLegalPreviewReadsFormalHtmlInsteadOfOwningSecondCopy() throws Exception {
        String runtime = read("resources/static/next/n28-legal-single-source.js");
        String rules = read("resources/templates/rules.html");
        String privacy = read("resources/templates/privacy.html");

        assertTrue(runtime.contains("fetch(`/${doc}?legacy=1`"));
        assertTrue(runtime.contains("method: 'GET'"));
        assertTrue(runtime.contains("headers: { Accept: 'text/html' }"));
        assertTrue(runtime.contains("new DOMParser().parseFromString(html, 'text/html')"));
        assertTrue(runtime.contains("source.querySelector('.rules-card')"));

        assertTrue(rules.contains("未开工前可取消"));
        assertTrue(privacy.contains("询价信息（未成交）"));
        assertFalse(runtime.contains("未开工前可取消"));
        assertFalse(runtime.contains("询价信息（未成交）"));
        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
    }

    @Test
    void parserRebuildsOnlySafeDomAndNeverInjectsFetchedHtml() throws Exception {
        String runtime = read("resources/static/next/n28-legal-single-source.js");

        assertTrue(runtime.contains("child.nodeType === Node.TEXT_NODE"));
        assertTrue(runtime.contains("['b', 'strong', 'em', 'code'].includes(tag)"));
        assertTrue(runtime.contains("url.protocol === 'https:'"));
        assertTrue(runtime.contains("!url.username && !url.password"));
        assertTrue(runtime.contains("sourceCard.querySelectorAll(':scope > section')"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));
        assertFalse(runtime.contains("document.write"));
        assertFalse(runtime.contains("eval("));
    }

    @Test
    void rulesAndPrivacyStayInsideNextWithExplicitFormalSourceEscape() throws Exception {
        String runtime = read("resources/static/next/n28-legal-single-source.js");
        String routes = read("resources/static/next/n14-route-continuity.js");

        assertTrue(runtime.contains("legacy.href = `/${doc}`"));
        assertTrue(runtime.contains("legacy.dataset.n28Legacy = '1'"));
        assertTrue(runtime.contains("robots.content = 'noindex,follow'"));
        assertTrue(runtime.contains("url.pathname === '/rules'"));
        assertTrue(runtime.contains("url.pathname === '/privacy'"));
        assertTrue(runtime.contains("url.pathname === '/contact'"));

        assertTrue(routes.contains("url.pathname === '/rules'"));
        assertTrue(routes.contains("nextUrl(url, '/next/rules')"));
        assertTrue(routes.contains("url.pathname === '/privacy'"));
        assertTrue(routes.contains("nextUrl(url, '/next/privacy')"));
        assertTrue(routes.contains("stageSource('/next/n28-legal-single-source.js')"));
        assertTrue(routes.contains("link.dataset.n28Legacy === '1'"));
    }
}
