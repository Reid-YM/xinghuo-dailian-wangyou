package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN13RecordDetailContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerExposesRecordRouteWithoutReplacingLegacyPage() throws Exception {
        String preview = read("java/com/xinghuo/web/FrontendNextPreviewController.java");
        String legacy = read("java/com/xinghuo/controller/PageController.java");

        assertTrue(preview.contains("/next/record/{businessNo}"));
        assertTrue(preview.contains("N13_RUNTIME"));
        assertTrue(preview.contains("/next/n13-record-detail.js"));
        assertTrue(preview.contains("path.startsWith(\"/record/\") ? \"业务记录 · \""));

        assertTrue(legacy.contains("/record/{businessNo}"));
        assertTrue(legacy.contains("return \"record-detail\""));
        assertFalse(legacy.contains("redirect:/next/record"));
    }

    @Test
    void recordPreviewUsesOnlyExistingPublicReadApis() throws Exception {
        String runtime = read("resources/static/next/n13-record-detail.js");
        String api = read("java/com/xinghuo/controller/PublicApiController.java");

        assertTrue(runtime.contains("/api/records/${encodeURIComponent(businessNo)}"));
        assertTrue(runtime.contains("method: 'GET'"));
        assertTrue(runtime.contains("credentials: 'same-origin'"));
        assertTrue(runtime.contains("cache: 'no-store'"));
        assertTrue(runtime.contains("/api/records/${encodeURIComponent(businessNo)}/image"));
        assertTrue(api.contains("@GetMapping(value={\"/records/{businessNo}\"})"));
        assertTrue(api.contains("return this.records.get(businessNo)"));

        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
        assertFalse(runtime.contains("/api/payment"));
        assertFalse(runtime.contains("/api/refund"));
        assertFalse(runtime.contains("/api/console/"));
    }

    @Test
    void publicBusinessNumberRulesAndStaleResponseProtectionMatchExistingService() throws Exception {
        String runtime = read("resources/static/next/n13-record-detail.js");
        String service = read("java/com/xinghuo/service/RecordService.java");

        assertTrue(runtime.contains("/^(XH[A-Z0-9]{10}|XH-[A-Z2-9]{24})$/"));
        assertTrue(service.contains("value.matches(\"XH[A-Z0-9]{10}\")"));
        assertTrue(service.contains("value.matches(\"XH-[A-Z2-9]{24}\")"));
        assertTrue(runtime.contains("const request = ++routeRequest"));
        assertTrue(runtime.contains("request !== routeRequest || routeNo() !== businessNo"));
        assertTrue(runtime.contains("window.addEventListener('popstate', syncRoute)"));
    }

    @Test
    void quoteSuccessStaysInsidePreviewAndRecordPageKeepsExplicitLegacyEscape() throws Exception {
        String runtime = read("resources/static/next/n13-record-detail.js");
        String quote = read("resources/static/next/n3.js");

        assertTrue(quote.contains("record.href=`/next/record/${encodeURIComponent(businessNo)}`"));
        assertFalse(quote.contains("record.href=`/record/${encodeURIComponent(businessNo)}`"));
        assertTrue(runtime.contains("legacy.href = `/record/${encodeURIComponent(businessNo)}`"));
        assertTrue(runtime.contains("legacy.dataset.n13Legacy = '1'"));
        assertTrue(runtime.contains("robots.content = 'noindex,nofollow'"));
        assertTrue(runtime.contains("canonical.href = `https://wydlgf.cn/record/${encodeURIComponent(text(record?.businessNo))}`"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));
        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
    }
}
