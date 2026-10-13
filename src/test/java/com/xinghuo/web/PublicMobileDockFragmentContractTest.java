package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PublicMobileDockFragmentContractTest {
    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @Test
    void ordinaryPublicDockLivesInSharedSiteFragment() throws Exception {
        String site = Files.readString(TEMPLATES.resolve("fragments/site.html"));
        assertTrue(site.contains("th:fragment=\"mobileDock\""));
        assertTrue(site.contains("class=\"mobile-dock\" aria-label=\"手机快捷操作\""));
        assertTrue(site.contains("<a href=\"/\">首页</a><a href=\"/records\">业务记录</a>"));
        assertTrue(site.contains("href=\"/quote\" data-primary-cta>提前询价</a>"));
    }

    @Test
    void migratedPagesDoNotReintroduceInlineOrdinaryDock() throws Exception {
        for (String template : new String[]{"services.html", "trust.html", "records.html"}) {
            String html = Files.readString(TEMPLATES.resolve(template));
            assertTrue(html.contains("fragments/site :: mobileDock"), template + " must reuse shared mobileDock");
            assertFalse(html.contains("<nav class=\"mobile-dock\""), template + " must not hand-write ordinary mobile dock");
        }
    }
}
