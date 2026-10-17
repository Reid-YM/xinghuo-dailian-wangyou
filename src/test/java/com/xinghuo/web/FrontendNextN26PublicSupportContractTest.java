package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN26PublicSupportContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerExposesSupportRoutesAndFingerprintsRuntime() throws Exception {
        String preview = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        assertTrue(preview.contains("\"/next/records\""));
        assertTrue(preview.contains("\"/next/contact\""));
        assertTrue(preview.contains("\"/next/trust\""));
        assertTrue(preview.contains("N26_RUNTIME"));
        assertTrue(preview.contains("/next/n26-public-support.js"));
        assertTrue(preview.contains("\"next/n26-public-support.js\""));
        assertTrue(preview.contains("\"业务记录 · \" + brand"));
        assertTrue(preview.contains("\"一对一咨询 · \" + brand"));
        assertTrue(preview.contains("\"信任与 FAQ · \" + brand"));
        assertTrue(preview.contains("noindex,follow"));
    }

    @Test
    void recordsPreviewSharesLegacyBrowserHistoryAndPublicReadApi() throws Exception {
        String runtime = read("resources/static/next/n26-public-support.js");
        String legacy = read("resources/static/assets/public.js");

        assertTrue(runtime.contains("const RECORDS_KEY = 'xh_client_records_v1'"));
        assertTrue(legacy.contains("const RECORDS_KEY = 'xh_client_records_v1'"));
        assertTrue(runtime.contains("/^XH[A-Z0-9]{10}$/"));
        assertTrue(runtime.contains("/^XH-[A-Z2-9]{24}$/"));
        assertTrue(runtime.contains("localStorage.getItem(RECORDS_KEY)"));
        assertTrue(runtime.contains("localStorage.setItem(RECORDS_KEY"));
        assertTrue(runtime.contains("fetch(`/api/records/${encodeURIComponent(businessNo)}`"));
        assertTrue(runtime.contains("method: 'GET'"));
        assertTrue(runtime.contains("credentials: 'same-origin'"));
        assertTrue(runtime.contains("cache: 'no-store'"));
        assertTrue(runtime.contains("`/next/record/${encodeURIComponent(businessNo)}?from=records`"));
    }

    @Test
    void contactAndTrustUseExistingPublicConfigurationWithoutWriteRequests() throws Exception {
        String runtime = read("resources/static/next/n26-public-support.js");
        String config = read("java/com/xinghuo/service/SiteConfigService.java");

        assertTrue(runtime.contains("window.XHFrontendShell"));
        assertTrue(runtime.contains("configurationReady"));
        assertTrue(runtime.contains("fetch('/api/site-config'"));
        assertTrue(runtime.contains("contactChannels"));
        assertTrue(runtime.contains("wecomKefuUrl"));
        assertTrue(runtime.contains("taobaoUrl"));
        assertTrue(runtime.contains("xianyuUrl"));
        assertTrue(runtime.contains("trustCases"));
        assertTrue(runtime.contains("trustFaq"));
        assertTrue(config.contains("this.parseTrustCases(c)"));
        assertTrue(config.contains("this.parseTrustFaq(c)"));
        assertTrue(config.contains("this.parseContactChannels(c)"));

        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
    }

    @Test
    void supportLinksRemainInsideNextAndExplicitLegacyEscapesStayAvailable() throws Exception {
        String runtime = read("resources/static/next/n26-public-support.js");
        String continuity = read("resources/static/next/n14-route-continuity.js");

        assertTrue(runtime.contains("'/records?legacy=1'"));
        assertTrue(runtime.contains("'/contact?legacy=1'"));
        assertTrue(runtime.contains("'/trust?legacy=1'"));
        assertTrue(runtime.contains("legacy.dataset.n26Legacy = '1'"));
        assertTrue(runtime.contains("recordLink.href = '/next/records'"));
        assertTrue(runtime.contains("`/next/record/${encodeURIComponent(normalizeRecordNo(row.businessNo))}`"));
        assertTrue(continuity.contains("link.dataset.n26Legacy === '1'"));
        assertTrue(continuity.contains("nextUrl(url, '/next/records')"));
        assertTrue(continuity.contains("nextUrl(url, '/next/contact')"));
        assertTrue(continuity.contains("nextUrl(url, '/next/trust')"));
    }

    @Test
    void supportPagesUseLegacyCanonicalsAndSafeDomRendering() throws Exception {
        String runtime = read("resources/static/next/n26-public-support.js");

        assertTrue(runtime.contains("records: ['业务记录', '/records', 'noindex,follow']"));
        assertTrue(runtime.contains("contact: ['一对一咨询', '/contact', 'noindex,follow']"));
        assertTrue(runtime.contains("trust: ['信任与 FAQ', '/trust', 'noindex,follow']"));
        assertTrue(runtime.contains("canonical.href = `https://wydlgf.cn${canonicalPath}`"));
        assertTrue(runtime.contains("robots.content = robotsValue"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));
    }
}
