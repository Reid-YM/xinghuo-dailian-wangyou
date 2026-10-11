package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FrontendControlBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void frontendControlFeatureHasExplicitBoundaries() {
        Path base = ROOT.resolve("src/main/java/com/xinghuo/frontendcontrol");
        assertTrue(Files.isDirectory(base.resolve("application")));
        assertTrue(Files.isDirectory(base.resolve("domain")));
        assertTrue(Files.isDirectory(base.resolve("infrastructure")));
        assertTrue(Files.isDirectory(base.resolve("web")));
    }

    @Test
    void applicationAndDomainStayPersistenceAndServletIndependent() throws IOException {
        Path application = ROOT.resolve("src/main/java/com/xinghuo/frontendcontrol/application");
        try (Stream<Path> files = Files.list(application)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("com.xinghuo.model"));
                assertFalse(source.contains("com.xinghuo.repository"));
                assertFalse(source.contains("jakarta.servlet"));
                assertFalse(source.contains("jakarta.persistence"));
            }
        }
        String domain = read("src/main/java/com/xinghuo/frontendcontrol/domain/package-info.java");
        assertFalse(domain.contains("org.springframework"));
        assertFalse(domain.contains("jakarta.persistence"));
        assertFalse(domain.contains("jakarta.servlet"));
        assertFalse(domain.contains("com.xinghuo.repository"));
        assertFalse(domain.contains("com.xinghuo.service"));
    }

    @Test
    void controllersLiveInFeatureWebBoundaryAndKeepRoutes() throws IOException {
        String admin = read("src/main/java/com/xinghuo/frontendcontrol/web/FrontendControlApiController.java");
        String page = read("src/main/java/com/xinghuo/frontendcontrol/web/FrontendControlPageController.java");
        String publicApi = read("src/main/java/com/xinghuo/frontendcontrol/web/FrontendControlPublicController.java");

        assertTrue(admin.contains("/api/admin/frontend-controls"));
        assertTrue(page.contains("/admin/frontend-controls"));
        assertTrue(publicApi.contains("/api/public/frontend-controls"));
        assertTrue(admin.contains("FrontendControlAdminApplicationService"));
        assertTrue(admin.contains("FrontendControlAuditContext"));
        assertTrue(admin.contains("FrontendControlWebMapper.toCommand"));
        assertTrue(publicApi.contains("FrontendControlApplicationService"));
        assertFalse(admin.contains("AuditLogRepository"));
        assertFalse(admin.contains("ObjectMapper"));
        assertFalse(admin.contains("com.xinghuo.model.AuditLog"));

        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/FrontendControlApiController.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/FrontendControlPageController.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/FrontendControlPublicController.java")));
    }

    @Test
    void applicationOwnsWhitelistValidationAndAtomicBatchPort() throws IOException {
        String application = read("src/main/java/com/xinghuo/frontendcontrol/application/FrontendControlApplicationService.java");
        assertTrue(application.contains("themeAccentColor"));
        assertTrue(application.contains("javascript") || application.contains("isSafeHref"));
        assertTrue(application.contains("port.putAll(normalized)"));
        assertTrue(application.contains("主题数值超出允许范围"));

        String port = read("src/main/java/com/xinghuo/frontendcontrol/application/FrontendControlPort.java");
        assertTrue(port.contains("void putAll(Map<String, String> values)"));
        assertFalse(port.contains("SiteSettingRepository"));
    }

    @Test
    void persistenceAdaptersUseFeatureOwnedSettingsAndAuditRepositories() throws IOException {
        String settings = read("src/main/java/com/xinghuo/frontendcontrol/infrastructure/FrontendControlPersistenceAdapter.java");
        assertTrue(settings.contains("implements FrontendControlPort"));
        assertTrue(settings.contains("com.xinghuo.sitesetting.infrastructure.SiteSettingRepository"));
        assertTrue(settings.contains("com.xinghuo.sitesetting.infrastructure.SiteSetting"));
        assertFalse(settings.contains("com.xinghuo.repository.SiteSettingRepository"));
        assertFalse(settings.contains("com.xinghuo.model.SiteSetting"));
        assertTrue(settings.contains("@Transactional"));
        assertTrue(settings.contains("void putAll"));

        String audit = read("src/main/java/com/xinghuo/frontendcontrol/infrastructure/FrontendControlAuditPersistenceAdapter.java");
        assertTrue(audit.contains("implements FrontendControlAuditPort"));
        assertTrue(audit.contains("com.xinghuo.audit.infrastructure.AuditLogRepository"));
        assertTrue(audit.contains("com.xinghuo.audit.infrastructure.AuditLog"));
        assertFalse(audit.contains("com.xinghuo.repository.AuditLogRepository"));
        assertFalse(audit.contains("com.xinghuo.model.AuditLog"));
        assertTrue(audit.contains("FRONTEND_CONTROLS_UPDATE"));
        assertTrue(audit.contains("SITE_SETTINGS"));
        assertFalse(audit.contains("jakarta.servlet"));
    }

    @Test
    void legacyFrontendControlServiceAndGlobalControllersStayRetired() throws IOException {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/service/FrontendControlService.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/FrontendControlApiController.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/FrontendControlPageController.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/FrontendControlPublicController.java")));
        try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("com.xinghuo.service.FrontendControlService"),
                        file + " still references retired FrontendControlService");
            }
        }
    }
}
