package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ToolStationBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void toolStationFeatureHasExplicitBoundaries() {
        Path base = ROOT.resolve("src/main/java/com/xinghuo/toolstation");
        assertTrue(Files.isDirectory(base.resolve("application")));
        assertTrue(Files.isDirectory(base.resolve("domain")));
        assertTrue(Files.isDirectory(base.resolve("infrastructure")));
        assertTrue(Files.isDirectory(base.resolve("web")));
    }

    @Test
    void applicationAndDomainStayJsonStorageAndWebIndependent() throws IOException {
        Path application = ROOT.resolve("src/main/java/com/xinghuo/toolstation/application");
        try (Stream<Path> files = Files.list(application)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("com.fasterxml.jackson"));
                assertFalse(source.contains("java.nio.file"));
                assertFalse(source.contains("org.springframework.core.io"));
                assertFalse(source.contains("com.xinghuo.dto"));
                assertFalse(source.contains("com.xinghuo.model"));
                assertFalse(source.contains("com.xinghuo.repository"));
                assertFalse(source.contains("jakarta.servlet"));
                assertFalse(source.contains("jakarta.persistence"));
            }
        }

        String domain = read("src/main/java/com/xinghuo/toolstation/domain/package-info.java");
        assertFalse(domain.contains("com.fasterxml.jackson"));
        assertFalse(domain.contains("java.nio.file"));
        assertFalse(domain.contains("org.springframework"));
        assertFalse(domain.contains("jakarta.servlet"));
        assertFalse(domain.contains("jakarta.persistence"));
    }

    @Test
    void publicAndAdminConsumeToolStationApplicationAndWebBoundaries() throws IOException {
        String publicApi = read("src/main/java/com/xinghuo/toolstation/web/PublicToolStationController.java");
        assertTrue(publicApi.contains("package com.xinghuo.toolstation.web;"));
        assertTrue(publicApi.contains("com.xinghuo.toolstation.application.ToolStationApplicationService"));
        assertTrue(publicApi.contains("@RequestMapping(value={\"/api/tool-station\"})"));
        assertTrue(publicApi.contains("toolStationMapper.toResponse"));
        assertFalse(publicApi.contains("import com.xinghuo.service.ToolStationService;"));

        String admin = read("src/main/java/com/xinghuo/toolstation/web/AdminToolStationController.java");
        assertTrue(admin.contains("package com.xinghuo.toolstation.web;"));
        assertTrue(admin.contains("com.xinghuo.toolstation.application.ToolStationApplicationService"));
        assertTrue(admin.contains("@RequestMapping(value={\"/api/admin/tool-station\"})"));
        assertTrue(admin.contains("ToolStationWebMapper toolStationMapper"));
        assertTrue(admin.contains("toolStationMapper.toJson"));
        assertTrue(admin.contains("toolStationMapper.toResponse"));
        assertTrue(admin.contains("SiteImageUploadResponse"));
        assertFalse(admin.contains("import com.xinghuo.service.ToolStationService;"));

        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/PublicApiController.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/AdminApiController.java")));
    }

    @Test
    void webOwnsJsonNodeAndInfrastructureOwnsFilePersistence() throws IOException {
        String mapper = read("src/main/java/com/xinghuo/toolstation/web/ToolStationWebMapper.java");
        assertTrue(mapper.contains("com.fasterxml.jackson.databind.JsonNode"));
        assertTrue(mapper.contains("com.fasterxml.jackson.databind.ObjectMapper"));

        String adapter = read("src/main/java/com/xinghuo/toolstation/infrastructure/ToolStationFileAdapter.java");
        assertTrue(adapter.contains("implements ToolStationPort"));
        assertTrue(adapter.contains("com.fasterxml.jackson.databind.ObjectMapper"));
        assertTrue(adapter.contains("java.nio.file.Files"));
        assertTrue(adapter.contains("org.springframework.core.io.ClassPathResource"));
        assertTrue(adapter.contains("static/assets/tool-station-games.json"));
        assertTrue(adapter.contains(".bak"));
        assertTrue(adapter.contains("工具站至少需要保留一个游戏版本"));
        assertFalse(adapter.contains("com.xinghuo.dto"));
        assertFalse(adapter.contains("com.xinghuo.service.ToolStationService"));
    }

    @Test
    void legacyToolStationServiceStaysRetired() throws IOException {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/service/ToolStationService.java")));
        try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("com.xinghuo.service.ToolStationService"),
                        file + " still references retired ToolStationService");
            }
        }
    }
}
