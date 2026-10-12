package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PublicCanonicalContractTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @Test
    void staticIndexablePagesKeepSelfReferencingCanonicalUrls() throws Exception {
        Map<String, String> pages = Map.ofEntries(
                Map.entry("index.html", "href=\"https://wydlgf.cn/\""),
                Map.entry("services.html", "href=\"https://wydlgf.cn/services\""),
                Map.entry("mw4-camo-tracker.html", "href=\"https://wydlgf.cn/mastery\""),
                Map.entry("quote.html", "href=\"https://wydlgf.cn/quote\""),
                Map.entry("contact.html", "href=\"https://wydlgf.cn/contact\""),
                Map.entry("trust.html", "href=\"https://wydlgf.cn/trust\""),
                Map.entry("rules.html", "href=\"https://wydlgf.cn/rules\""),
                Map.entry("rules-en.html", "href=\"https://wydlgf.cn/rules-en\""),
                Map.entry("privacy.html", "href=\"https://wydlgf.cn/privacy\""),
                Map.entry("privacy-en.html", "href=\"https://wydlgf.cn/privacy-en\"")
        );

        for (Map.Entry<String, String> page : pages.entrySet()) {
            String html = Files.readString(TEMPLATES.resolve(page.getKey()));
            assertTrue(html.contains("rel=\"canonical\""),
                    page.getKey() + " must declare a canonical URL");
            assertTrue(html.contains(page.getValue()),
                    page.getKey() + " must keep its expected self-referencing canonical URL");
        }
    }

    @Test
    void dynamicIndexablePagesKeepServerGeneratedCanonicalUrls() throws Exception {
        for (String file : new String[]{"service-detail.html", "game.html", "guides.html", "guide.html"}) {
            String html = Files.readString(TEMPLATES.resolve(file));
            assertTrue(html.contains("rel=\"canonical\""),
                    file + " must declare a canonical URL");
            assertTrue(html.contains("th:href=\"${canonicalUrl}\""),
                    file + " must use the controller-provided canonicalUrl");
        }
    }
}
