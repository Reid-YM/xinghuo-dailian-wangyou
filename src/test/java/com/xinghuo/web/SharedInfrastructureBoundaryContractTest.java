package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SharedInfrastructureBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void auditAndUploadImplementationsLiveOutsideLegacyServicePackage() throws Exception {
        String audit = Files.readString(ROOT.resolve(
                "src/main/java/com/xinghuo/audit/infrastructure/AuditService.java"));
        String upload = Files.readString(ROOT.resolve(
                "src/main/java/com/xinghuo/upload/infrastructure/UploadService.java"));

        assertTrue(audit.contains("class AuditService"));
        assertTrue(audit.contains("AuditLog.builder()"));
        assertTrue(audit.contains("RequestUtil.clientIp"));
        assertFalse(audit.contains("package com.xinghuo.service"));

        assertTrue(upload.contains("class UploadService"));
        assertTrue(upload.contains("MAX_PIXELS"));
        assertTrue(upload.contains("ImageIO.createImageInputStream"));
        assertTrue(upload.contains("loadPublicSiteAsset"));
        assertFalse(upload.contains("package com.xinghuo.service"));
    }

    @Test
    void compatibilityBridgesContainNoBusinessImplementation() throws Exception {
        String auditBridge = Files.readString(ROOT.resolve(
                "src/main/java/com/xinghuo/service/AuditService.java"));
        String uploadBridge = Files.readString(ROOT.resolve(
                "src/main/java/com/xinghuo/service/UploadService.java"));

        assertTrue(auditBridge.contains("extends com.xinghuo.audit.infrastructure.AuditService"));
        assertTrue(uploadBridge.contains("extends com.xinghuo.upload.infrastructure.UploadService"));
        assertTrue(auditBridge.contains("@Deprecated(forRemoval = true)"));
        assertTrue(uploadBridge.contains("@Deprecated(forRemoval = true)"));
        assertFalse(auditBridge.contains("AuditLog.builder()"));
        assertFalse(uploadBridge.contains("ImageIO"));
        assertFalse(uploadBridge.contains("MAX_PIXELS"));
    }

    @Test
    void migratedConsumersBypassCompatibilityBridges() throws Exception {
        for (String path : new String[] {
                "src/main/java/com/xinghuo/siteconfig/web/SiteConfigWriteWebService.java",
                "src/main/java/com/xinghuo/controller/MobileVisualConfigController.java",
                "src/main/java/com/xinghuo/gamestatus/infrastructure/GameStatusPersistenceAdapter.java",
                "src/main/java/com/xinghuo/feedback/infrastructure/FeedbackPersistenceAdapter.java",
                "src/main/java/com/xinghuo/slot/infrastructure/SlotPersistenceAdapter.java",
                "src/main/java/com/xinghuo/catalog/infrastructure/CatalogPersistenceAdapter.java",
                "src/main/java/com/xinghuo/order/infrastructure/OrderPersistenceAdapter.java",
                "src/main/java/com/xinghuo/quote/infrastructure/QuotePersistenceAdapter.java",
                "src/main/java/com/xinghuo/account/web/AccountApiController.java"
        }) {
            String source = Files.readString(ROOT.resolve(path));
            assertFalse(source.contains("com.xinghuo.service.AuditService"), path);
            assertFalse(source.contains("com.xinghuo.service.UploadService"), path);
        }
    }
}
