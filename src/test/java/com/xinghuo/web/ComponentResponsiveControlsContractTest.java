package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ComponentResponsiveControlsContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void backendExposesDesktopAndMobileComponentControls() throws Exception {
        String migration = Files.readString(ROOT.resolve("src/main/resources/db/migration/V60__responsive_component_controls.sql"));
        String updateContract = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/siteconfig/web/SiteConfigUpdate.java"));
        String responseContract = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/siteconfig/web/SiteConfigResponse.java"));
        String writes = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/siteconfig/infrastructure/SiteConfigWritePersistenceAdapter.java"));

        assertTrue(migration.contains("home_service_mobile_limit"));
        assertTrue(migration.contains("home_service_desktop_columns"));
        assertTrue(migration.contains("trust_faq_home_mobile_limit"));
        assertTrue(Files.readString(ROOT.resolve("src/main/resources/db/migration/V61__mobile_background_image_controls.sql")).contains("hero_background_mobile_image"));
        assertTrue(updateContract.contains("int homeServiceMobileLimit"));
        assertTrue(updateContract.contains("String serviceCardMobileDensity"));
        assertTrue(updateContract.contains("String heroBackgroundMobileImage"));
        assertTrue(responseContract.contains("String heroBackgroundMobileImage"));
        assertTrue(writes.contains("setHomeServiceMobileLimit"));
        assertTrue(writes.contains("setHeroBackgroundMobileImage"));
        assertTrue(writes.contains("normalizeDensity"));
    }

    @Test
    void adminAndPublicAssetsUseResponsiveComponentFields() throws Exception {
        String adminHtml = Files.readString(ROOT.resolve("src/main/resources/templates/admin.html"));
        String adminJs = Files.readString(ROOT.resolve("src/main/resources/static/assets/admin.js"));
        String publicJs = Files.readString(ROOT.resolve("src/main/resources/static/assets/public.js"));
        String pagesJs = Files.readString(ROOT.resolve("src/main/resources/static/assets/pages.js"));
        String css = Files.readString(ROOT.resolve("src/main/resources/static/assets/site-image-controls.css"));

        assertTrue(adminHtml.contains("name=\"homeServiceMobileLimit\""));
        assertTrue(adminHtml.contains("name=\"heroBackgroundMobileImage\""));
        assertTrue(adminHtml.contains("id=\"uploadHeroBackgroundMobile\""));
        assertTrue(adminHtml.contains("name=\"homeContactMobileLimit\""));
        assertTrue(adminJs.contains("trustCaseHomeMobileLimit"));
        assertTrue(adminJs.contains("BACKGROUND_MOBILE"));
        assertTrue(publicJs.contains("heroBackgroundMobileImage"));
        assertTrue(publicJs.contains("componentNumber(config, 'homeServiceLimit', 'homeServiceMobileLimit'"));
        assertTrue(publicJs.contains("componentNumber(config, 'homeContactLimit', 'homeContactMobileLimit'"));
        assertTrue(pagesJs.contains("XH.applyServiceGridControls"));
        assertTrue(css.contains("--component-card-columns"));
        assertTrue(css.contains("[data-card-density=\"compact\"]"));
    }
}
