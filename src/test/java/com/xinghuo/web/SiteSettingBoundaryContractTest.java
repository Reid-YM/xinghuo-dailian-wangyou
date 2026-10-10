package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SiteSettingBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void siteSettingFeatureHasExplicitBoundaries() {
        Path base = ROOT.resolve("src/main/java/com/xinghuo/sitesetting");
        assertTrue(Files.isDirectory(base.resolve("application")));
        assertTrue(Files.isDirectory(base.resolve("domain")));
        assertTrue(Files.isDirectory(base.resolve("infrastructure")));
        assertTrue(Files.isDirectory(base.resolve("web")));
    }

    @Test
    void applicationAndDomainStayPersistenceIndependent() throws IOException {
        Path application = ROOT.resolve("src/main/java/com/xinghuo/sitesetting/application");
        try (Stream<Path> files = Files.list(application)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("com.xinghuo.model"));
                assertFalse(source.contains("com.xinghuo.repository"));
                assertFalse(source.contains("jakarta.persistence"));
                assertFalse(source.contains("jakarta.servlet"));
            }
        }

        String domain = read("src/main/java/com/xinghuo/sitesetting/domain/package-info.java");
        assertFalse(domain.contains("com.xinghuo.model"));
        assertFalse(domain.contains("com.xinghuo.repository"));
        assertFalse(domain.contains("jakarta.persistence"));
        assertFalse(domain.contains("jakarta.servlet"));
    }

    @Test
    void persistenceAdapterOwnsSiteSettingJpaAccessAndAtomicBatchWrite() throws IOException {
        String adapter = read("src/main/java/com/xinghuo/sitesetting/infrastructure/SiteSettingPersistenceAdapter.java");
        assertTrue(adapter.contains("implements SiteSettingPort"));
        assertTrue(adapter.contains("com.xinghuo.repository.SiteSettingRepository"));
        assertTrue(adapter.contains("com.xinghuo.model.SiteSetting"));
        assertTrue(adapter.contains("public void putAll"));
        assertTrue(adapter.contains("@Transactional"));
    }

    @Test
    void allKnownConsumersUseSiteSettingApplicationBoundary() throws IOException {
        String globalUi = read("src/main/java/com/xinghuo/controller/GlobalUiSettingsController.java");
        assertTrue(globalUi.contains("com.xinghuo.sitesetting.application.SiteSettingApplicationService"));
        assertTrue(globalUi.contains("com.xinghuo.sitesetting.web.SiteSettingWebMapper"));
        assertFalse(globalUi.contains("com.xinghuo.service.SiteSettingService"));

        String publicApi = read("src/main/java/com/xinghuo/controller/PublicApiController.java");
        assertTrue(publicApi.contains("com.xinghuo.sitesetting.application.SiteSettingApplicationService"));
        assertFalse(publicApi.contains("com.xinghuo.service.SiteSettingService"));

        String siteConfigWrites = read("src/main/java/com/xinghuo/siteconfig/application/SiteConfigWriteApplicationService.java");
        assertTrue(siteConfigWrites.contains("com.xinghuo.sitesetting.application.SiteSettingApplicationService"));
        assertTrue(siteConfigWrites.contains("siteSettings.put(\"brandName\""));
        assertTrue(siteConfigWrites.contains("siteSettings.put(\"legalEntityName\""));
        assertTrue(siteConfigWrites.contains("siteSettings.put(\"wechat\""));
        assertTrue(siteConfigWrites.contains("siteSettings.put(\"serviceHours\""));
        assertTrue(siteConfigWrites.contains("siteSettings.put(\"siteTitleSuffix\""));
        assertFalse(siteConfigWrites.contains("com.xinghuo.service.SiteSettingService"));
    }

    @Test
    void legacySiteSettingServiceStaysRetired() throws IOException {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/service/SiteSettingService.java")));
        try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("com.xinghuo.service.SiteSettingService"),
                        file + " still references retired SiteSettingService");
            }
        }
    }
}
