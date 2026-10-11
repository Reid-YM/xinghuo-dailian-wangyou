package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MobileVisualBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void mobileVisualFeatureHasExplicitBoundaries() {
        Path base = ROOT.resolve("src/main/java/com/xinghuo/mobilevisual");
        assertTrue(Files.isDirectory(base.resolve("application")));
        assertTrue(Files.isDirectory(base.resolve("domain")));
        assertTrue(Files.isDirectory(base.resolve("infrastructure")));
        assertTrue(Files.isDirectory(base.resolve("web")));
    }

    @Test
    void applicationAndDomainStayPersistenceAndProtocolIndependent() throws IOException {
        Path application = ROOT.resolve("src/main/java/com/xinghuo/mobilevisual/application");
        try (Stream<Path> files = Files.list(application)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("com.xinghuo.dto"));
                assertFalse(source.contains("com.xinghuo.model"));
                assertFalse(source.contains("com.xinghuo.repository"));
                assertFalse(source.contains("jakarta.servlet"));
                assertFalse(source.contains("jakarta.persistence"));
            }
        }

        String domain = read("src/main/java/com/xinghuo/mobilevisual/domain/package-info.java");
        assertFalse(domain.contains("org.springframework"));
        assertFalse(domain.contains("jakarta.persistence"));
        assertFalse(domain.contains("jakarta.servlet"));
        assertFalse(domain.contains("com.xinghuo.repository"));
        assertFalse(domain.contains("com.xinghuo.service"));
    }

    @Test
    void controllerLivesInFeatureWebBoundaryAndKeepsRoutesAndValidation() throws IOException {
        String controller = read("src/main/java/com/xinghuo/mobilevisual/web/MobileVisualConfigController.java");
        assertTrue(controller.contains("/api/public/mobile-visual-config"));
        assertTrue(controller.contains("/api/admin/mobile-visual-config"));
        assertTrue(controller.contains("MobileVisualConfigApplicationService publicConfigs"));
        assertTrue(controller.contains("MobileVisualConfigAdminApplicationService adminConfigs"));
        assertTrue(controller.contains("MobileVisualConfigWebMapper.toCommand"));
        assertTrue(controller.contains("MobileVisualConfigWebMapper.toResponse"));
        assertTrue(controller.contains("@NotNull @Min(0) @Max(100)"));
        assertTrue(controller.contains("@NotNull @Min(0) @Max(24)"));
        assertTrue(controller.contains("@Size(max = 500)"));
        assertTrue(controller.contains("request.getAttribute(\"requestId\")"));
        assertTrue(controller.contains("request.getHeader(\"X-Request-Id\")"));
        assertFalse(controller.contains("AuditService"));
        assertFalse(controller.contains("AuditLogRepository"));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/MobileVisualConfigController.java")));
    }

    @Test
    void settingsPersistenceAdapterUsesFeatureOwnedSitePersistence() throws IOException {
        String adapter = read("src/main/java/com/xinghuo/mobilevisual/infrastructure/MobileVisualConfigPersistenceAdapter.java");
        assertTrue(adapter.contains("implements MobileVisualConfigPort"));
        assertTrue(adapter.contains("com.xinghuo.sitesetting.infrastructure.SiteSettingRepository"));
        assertTrue(adapter.contains("com.xinghuo.siteconfig.infrastructure.SiteConfigRepository"));
        assertTrue(adapter.contains("com.xinghuo.sitesetting.infrastructure.SiteSetting"));
        assertTrue(adapter.contains("com.xinghuo.siteconfig.infrastructure.SiteConfig"));
        assertFalse(adapter.contains("com.xinghuo.repository.SiteSettingRepository"));
        assertFalse(adapter.contains("com.xinghuo.repository.SiteConfigRepository"));
        assertFalse(adapter.contains("com.xinghuo.model.SiteSetting"));
        assertFalse(adapter.contains("com.xinghuo.model.SiteConfig"));
        assertTrue(adapter.contains("heroBackgroundMobileOpacity"));
        assertTrue(adapter.contains("heroBackgroundMobileBlur"));
        assertTrue(adapter.contains("clamp"));
        assertFalse(adapter.contains("com.xinghuo.service.SiteConfigService"));
        assertFalse(adapter.contains("com.xinghuo.service.MobileVisualConfigService"));
        assertFalse(adapter.contains("com.xinghuo.dto"));
    }

    @Test
    void auditPersistenceAdapterUsesSharedAuditInfrastructure() throws IOException {
        String adapter = read("src/main/java/com/xinghuo/mobilevisual/infrastructure/MobileVisualConfigAuditPersistenceAdapter.java");
        assertTrue(adapter.contains("implements MobileVisualConfigAuditPort"));
        assertTrue(adapter.contains("com.xinghuo.audit.infrastructure.AuditLogRepository"));
        assertTrue(adapter.contains("com.xinghuo.audit.infrastructure.AuditLog"));
        assertFalse(adapter.contains("com.xinghuo.repository.AuditLogRepository"));
        assertFalse(adapter.contains("com.xinghuo.model.AuditLog"));
        assertTrue(adapter.contains("MOBILE_VISUAL_CONFIG_UPDATE"));
        assertTrue(adapter.contains("SITE_SETTINGS"));
        assertTrue(adapter.contains("mobile-background"));
        assertFalse(adapter.contains("jakarta.servlet"));
    }

    @Test
    void legacyMobileVisualServiceAndGlobalControllerStayRetired() throws IOException {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/service/MobileVisualConfigService.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/MobileVisualConfigController.java")));
        try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("com.xinghuo.service.MobileVisualConfigService"),
                        file + " still references retired MobileVisualConfigService");
            }
        }
    }
}
