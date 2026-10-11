package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SharedInfrastructureBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void auditAndUploadImplementationsAreOwnedSpringInfrastructureServices() throws Exception {
        String audit = Files.readString(ROOT.resolve(
                "src/main/java/com/xinghuo/audit/infrastructure/AuditService.java"));
        String upload = Files.readString(ROOT.resolve(
                "src/main/java/com/xinghuo/upload/infrastructure/UploadService.java"));

        assertTrue(audit.contains("@Service"));
        assertTrue(audit.contains("AuditLog.builder()"));
        assertTrue(audit.contains("RequestUtil.clientIp"));
        assertFalse(audit.contains("package com.xinghuo.service"));

        assertTrue(upload.contains("@Service"));
        assertTrue(upload.contains("MAX_PIXELS"));
        assertTrue(upload.contains("ImageIO.createImageInputStream"));
        assertTrue(upload.contains("loadPublicSiteAsset"));
        assertFalse(upload.contains("package com.xinghuo.service"));
    }

    @Test
    void legacySharedServicePackageIsFullyRetired() throws Exception {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/service/AuditService.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/service/UploadService.java")));

        Path sourceRoot = ROOT.resolve("src/main/java");
        try (var files = Files.walk(sourceRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("com.xinghuo.service.AuditService"), file.toString());
                assertFalse(source.contains("com.xinghuo.service.UploadService"), file.toString());
            }
        }
    }

    @Test
    void currentProductionConsumersCannotReferenceRetiredSharedServiceTypes() throws Exception {
        Path sourceRoot = ROOT.resolve("src/main/java");
        try (var files = Files.walk(sourceRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("import com.xinghuo.service.AuditService;"), file.toString());
                assertFalse(source.contains("import com.xinghuo.service.UploadService;"), file.toString());
            }
        }
    }
}
