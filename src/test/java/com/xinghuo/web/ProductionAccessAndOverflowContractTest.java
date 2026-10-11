package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionAccessAndOverflowContractTest {
    private static final Path ROOT = Path.of(".");
    private static final Path MAIN = Path.of("src/main");

    @Test
    void adminRootIsExplicitlyProtectedAndStorefrontDeviceInterceptorCannotReachAdmin() throws Exception {
        String security = Files.readString(MAIN.resolve("java/com/xinghuo/security/SecurityConfig.java"));
        String deviceConfig = Files.readString(MAIN.resolve("java/com/xinghuo/web/DeviceViewConfig.java"));

        assertTrue(security.contains(".requestMatchers(\"/admin\")"));
        assertTrue(security.contains(".requestMatchers(\"/admin/**\", \"/api/admin/**\")"));
        assertTrue(security.contains(".loginPage(\"/admin/login\")"));
        assertTrue(security.contains(".defaultSuccessUrl(\"/admin\", true)"));

        assertTrue(deviceConfig.contains(".addPathPatterns("));
        assertTrue(deviceConfig.contains("\"/service/**\""));
        assertFalse(deviceConfig.contains("\"/admin\""));
        assertFalse(deviceConfig.contains("\"/account\""));
        assertFalse(deviceConfig.contains("\"/console\""));
        assertFalse(deviceConfig.contains("\"/api/**\""));
    }

    @Test
    void adminAndApisCannotBlockOnExternalGeoLookup() throws Exception {
        String locale = Files.readString(MAIN.resolve("java/com/xinghuo/config/SmartLocaleResolver.java"));
        String geo = Files.readString(MAIN.resolve("java/com/xinghuo/security/GeoLanguageResolver.java"));

        assertTrue(locale.contains("if (this.skipGeoLookup(request))"));
        assertTrue(locale.contains("path.equals(\"/admin\")"));
        assertTrue(locale.contains("path.startsWith(\"/admin/\")"));
        assertTrue(locale.contains("path.startsWith(\"/api/\")"));
        assertTrue(locale.contains("path.startsWith(\"/account/\")"));
        assertTrue(locale.contains("path.startsWith(\"/console/\")"));
        assertTrue(geo.contains("FAILURE_CACHE_TTL_MS"));
        assertTrue(geo.contains("UNKNOWN_COUNTRY"));
        assertTrue(geo.contains("this.cache.put(ip, new Entry(country"));
    }

    @Test
    void finalOverflowLayerLoadsAfterDeviceAndMobileV4StylesAcrossAdminSurfaces() throws Exception {
        String site = Files.readString(MAIN.resolve("resources/templates/fragments/site.html"));
        String guard = Files.readString(MAIN.resolve("resources/static/assets/site-overflow-guard.css"));
        String adminFixes = Files.readString(MAIN.resolve("resources/static/assets/admin-brand-layout-fixes.css"));

        int desktop = site.indexOf("desktop-storefront.css?v=20261011ds20");
        int mobile = site.indexOf("mobile-storefront.css?v=20261011ds20");
        int mobileV4 = site.indexOf("mobile-home-v4.css?v=20261012mv401");
        int overflow = site.indexOf("site-overflow-guard.css?v=20261011of01");
        assertTrue(desktop >= 0 && mobile >= 0 && mobileV4 > mobile && overflow > desktop && overflow > mobileV4);
        assertTrue(adminFixes.startsWith("@import url('/assets/site-overflow-guard.css?v=20261011of01');"));

        assertTrue(guard.contains("overflow-x: clip !important"));
        assertTrue(guard.contains("body[data-page=\"home\"] #homeHero"));
        assertTrue(guard.contains("body.admin-solid-ui .admin-shell"));
        assertTrue(guard.contains(".table-wrap"));
        assertTrue(guard.contains("@media (max-width: 1180px)"));
        assertTrue(guard.contains("@media (max-width: 900px)"));
    }

    @Test
    void deploymentSmokeRefusesReleaseWhenAdminEntryIsBroken() throws Exception {
        String smoke = Files.readString(ROOT.resolve("scripts/smoke_test.sh"));
        assertTrue(smoke.contains("check_code /admin/login 200"));
        assertTrue(smoke.contains("check_redirect /admin /admin/login"));
    }
}
