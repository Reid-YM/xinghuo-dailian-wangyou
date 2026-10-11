package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendControlCenterV3ContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void backendWhitelistsOnlySafeV3LayoutControls() throws Exception {
        String service = Files.readString(MAIN.resolve("java/com/xinghuo/service/FrontendControlService.java"));

        for (String key : new String[]{
                "homeModuleOrder", "homeHeroHeight", "homeCardScale", "homeLauncherVisible",
                "homeHotVisible", "homeServicesVisible", "homeToolVisible", "homeCasesVisible",
                "homeFaqVisible", "mobileDockServicesLabel", "mobileDockGuidesLabel",
                "mobileDockRecordsLabel", "mobileDockContactLabel", "mobileDockQuoteLabel"
        }) {
            assertTrue(service.contains("\"" + key + "\""), "missing V3 control key " + key);
        }
        assertTrue(service.contains("normalizeModuleOrder"));
        assertTrue(service.contains("HOME_MODULE_IDS"));
        assertTrue(service.contains("LinkedHashSet"));
        assertFalse(service.contains("homeContactVisible"),
                "quick-contact visibility must remain owned by the existing homeQuickContactVisible setting");
        assertFalse(service.contains("customCss"));
        assertFalse(service.contains("customJavaScript"));
    }

    @Test
    void runtimeOwnsDeviceSpecificOrderingSizingVisibilityAndDockLabels() throws Exception {
        String runtime = Files.readString(MAIN.resolve("resources/static/assets/frontend-controls-runtime.js"));

        assertTrue(runtime.contains("DESKTOP_HOME_MODULES"));
        assertTrue(runtime.contains("MOBILE_HOME_MODULES"));
        assertTrue(runtime.contains("why: '.xh-desktop-proof-grid'"));
        assertTrue(runtime.contains("why: '.xh-mobile-proof-grid'"));
        assertTrue(runtime.contains("function resolveHomeModule"));
        assertTrue(runtime.contains("candidate.closest('section')"));
        assertTrue(runtime.contains("applyHomeModuleOrder"));
        assertTrue(runtime.contains("applyHomeSizing"));
        assertTrue(runtime.contains("applyHomeVisibility"));
        assertTrue(runtime.contains("main.appendChild(node)"));
        assertTrue(runtime.contains("--xh-home-card-padding"));
        assertTrue(runtime.contains("mobileDockServicesLabel"));
        assertTrue(runtime.contains("mobileDockQuoteLabel"));
        assertTrue(runtime.contains("if (!bool(control.homeCasesVisible))"));
        assertTrue(runtime.contains("if (!bool(control.homeGameEntryVisible))"));
        assertFalse(runtime.contains("control.homeContactVisible"));
        assertFalse(runtime.contains("main section[aria-label=\"为什么选择星火\"]"),
                "V3 ordering must not depend on localized aria-label copy");
    }

    @Test
    void adminExposesV3AsDeviceScopedVisualControls() throws Exception {
        String template = Files.readString(MAIN.resolve("resources/templates/admin-frontend-controls.html"));
        String admin = Files.readString(MAIN.resolve("resources/static/assets/admin-frontend-controls.js"));

        assertTrue(template.contains("前端控制中心 V3"));
        assertTrue(template.contains("href=\"#layout\""));
        assertTrue(template.contains("admin-frontend-controls.js?v=20261011fc30"));
        assertTrue(admin.contains("HOME_MODULES"));
        assertTrue(admin.contains("desktop:"));
        assertTrue(admin.contains("mobile:"));
        assertTrue(admin.contains("resetHomeModuleOrder"));
        assertTrue(admin.contains("data-order-move"));
        assertTrue(admin.contains("homeHeroHeight"));
        assertTrue(admin.contains("homeCardScale"));
        assertTrue(admin.contains("mobileDockQuoteLabel"));
        assertFalse(admin.contains("name=\"homeContactVisible\""));
    }

    @Test
    void publicShellUsesFreshV3RuntimeCacheKey() throws Exception {
        String site = Files.readString(MAIN.resolve("resources/templates/fragments/site.html"));
        assertTrue(site.contains("frontend-controls-runtime.js?v=20261011fc30"));
        assertFalse(site.contains("frontend-controls-runtime.js?v=20261011fc20"));
    }
}
