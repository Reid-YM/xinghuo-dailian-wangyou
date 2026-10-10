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
    void controllerConsumesApplicationAndWebBoundary() throws IOException {
        String controller = read("src/main/java/com/xinghuo/controller/MobileVisualConfigController.java");
        assertTrue(controller.contains("com.xinghuo.mobilevisual.application.MobileVisualConfigApplicationService"));
        assertTrue(controller.contains("com.xinghuo.mobilevisual.web.MobileVisualConfigWebMapper"));
        assertTrue(controller.contains("MobileVisualConfigWebMapper.toCommand"));
        assertTrue(controller.contains("MobileVisualConfigWebMapper.toResponse"));
        assertFalse(controller.contains("import com.xinghuo.service.MobileVisualConfigService;"));
    }

    @Test
    void persistenceAdapterOwnsFallbackAndSettingsPersistence() throws IOException {
        String adapter = read("src/main/java/com/xinghuo/mobilevisual/infrastructure/MobileVisualConfigPersistenceAdapter.java");
        assertTrue(adapter.contains("implements MobileVisualConfigPort"));
        assertTrue(adapter.contains("com.xinghuo.repository.SiteSettingRepository"));
        assertTrue(adapter.contains("com.xinghuo.repository.SiteConfigRepository"));
        assertTrue(adapter.contains("heroBackgroundMobileOpacity"));
        assertTrue(adapter.contains("heroBackgroundMobileBlur"));
        assertTrue(adapter.contains("clamp"));
        assertFalse(adapter.contains("com.xinghuo.service.SiteConfigService"));
        assertFalse(adapter.contains("com.xinghuo.service.MobileVisualConfigService"));
        assertFalse(adapter.contains("com.xinghuo.dto"));
    }

    @Test
    void legacyMobileVisualServiceStaysRetired() throws IOException {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/service/MobileVisualConfigService.java")));
        try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("com.xinghuo.service.MobileVisualConfigService"),
                        file + " still references retired MobileVisualConfigService");
            }
        }
    }
}
