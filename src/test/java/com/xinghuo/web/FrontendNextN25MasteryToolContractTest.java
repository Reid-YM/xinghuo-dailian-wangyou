package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN25MasteryToolContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerExposesMasteryAndFingerprintsN25Runtime() throws Exception {
        String preview = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        assertTrue(preview.contains("\"/next/mastery\""));
        assertTrue(preview.contains("N25_RUNTIME"));
        assertTrue(preview.contains("/next/n25-mastery-tool.js"));
        assertTrue(preview.contains("\"next/n25-mastery-tool.js\""));
        assertTrue(preview.contains("\"MW4 迷彩追踪器 · \" + brand"));
        assertTrue(preview.contains("\"/mastery\".equals(path)"));
        assertTrue(preview.contains("noindex,follow"));
    }

    @Test
    void nextMasteryReusesSingleAuthoritativeLegacyToolRuntime() throws Exception {
        String runtime = read("resources/static/next/n25-mastery-tool.js");
        String authoritative = read("resources/static/assets/mw4-camo-tracker.js");

        assertTrue(runtime.contains("const LEGACY_RUNTIME = '/assets/mw4-camo-tracker.js"));
        assertTrue(runtime.contains("const LEGACY_STYLE = '/assets/mw4-camo-tracker.css"));
        assertTrue(runtime.contains("script.src = LEGACY_RUNTIME"));
        assertTrue(runtime.contains("link.href = LEGACY_STYLE"));
        assertTrue(authoritative.contains("fetch('/api/tool-station'"));
        assertTrue(authoritative.contains("const TOOL_KEY = 'mw4_camo_tracker'"));
        assertTrue(authoritative.contains("/api/account/progress/${TOOL_KEY}"));
        assertTrue(authoritative.contains("'X-Requested-With': 'fetch'"));
        assertTrue(authoritative.contains("localStorage.setItem(STORAGE_KEY"));
        assertTrue(authoritative.contains("localStorage.setItem(QUOTE_KEY"));
    }

    @Test
    void n25BuildsAllRequiredRuntimeMountPointsBeforeLoadingTool() throws Exception {
        String runtime = read("resources/static/next/n25-mastery-tool.js");

        for (String id : new String[]{
                "overallPercent", "overallCount", "overallBar", "saveState",
                "syncBar", "syncLead", "syncText", "syncAction",
                "modeTabs", "modePanel", "modeHeadTitle", "modeHeadMeta", "modeHeadFinal", "tierCards", "modeNote",
                "pyroPanel", "pyroMeta", "pyroCards", "secretCard",
                "filterBar", "classFilter", "statusFilter", "weaponSearch", "resetProgress", "clearFavorites",
                "weaponHint", "trackerStatus", "weaponGroups", "emptyState", "quoteWithProgress"
        }) {
            assertTrue(runtime.contains("id = '" + id + "'"), id);
        }
        assertTrue(runtime.indexOf("buildPage()") < runtime.indexOf("await loadLegacyRuntime()"));
    }

    @Test
    void masteryLinksAndAuthStayInsideNextWhileLegacyEscapeRemainsPossible() throws Exception {
        String runtime = read("resources/static/next/n25-mastery-tool.js");
        String continuity = read("resources/static/next/n14-route-continuity.js");

        assertTrue(runtime.contains("quote.href = '/next/quote?toolProgress=1&gameCode=CODMW4'"));
        assertTrue(runtime.contains("syncAction.href = '/next/account?next=%2Fnext%2Fmastery'"));
        assertTrue(runtime.contains("delete link.dataset.n14Legacy"));
        assertTrue(runtime.contains("delete link.dataset.n21Legacy"));
        assertTrue(runtime.contains("contact.href = '/contact?legacy=1'"));
        assertTrue(runtime.contains("contact.dataset.n25Legacy = '1'"));
        assertTrue(continuity.contains("link.dataset.n25Legacy === '1'"));
        assertTrue(continuity.contains("url.pathname === '/mastery' || url.pathname === '/mastery/'"));
        assertTrue(continuity.contains("nextUrl(url, '/next/mastery')"));
    }

    @Test
    void previewMetadataKeepsLegacyMasteryCanonicalAndDoesNotDuplicateToolData() throws Exception {
        String runtime = read("resources/static/next/n25-mastery-tool.js");

        assertTrue(runtime.contains("canonical.href = 'https://wydlgf.cn/mastery'"));
        assertTrue(runtime.contains("robots.content = 'noindex,follow'"));
        assertFalse(runtime.contains("fetch('/api/tool-station'"));
        assertFalse(runtime.contains("/api/account/progress/"));
        assertFalse(runtime.contains("const STORAGE_KEY"));
    }
}
