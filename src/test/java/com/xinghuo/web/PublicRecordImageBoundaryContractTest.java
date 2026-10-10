package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PublicRecordImageBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void recordProgressImageIsOwnedByRecordWebAndUploadApplicationBoundaries() throws Exception {
        String controller = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/record/web/PublicRecordController.java"));

        assertTrue(controller.contains("@GetMapping(value={\"/{businessNo}/image\"})"));
        assertTrue(controller.contains("com.xinghuo.order.application.OrderApplicationService"));
        assertTrue(controller.contains("com.xinghuo.upload.application.UploadApplicationService"));
        assertTrue(controller.contains("runtimeConfig.orderQueryEnabled()"));
        assertTrue(controller.contains("pub:recordimg"));
        assertTrue(controller.contains("orders.progressAsset(businessNo)"));
        assertTrue(controller.contains("uploads.loadOwnedAsset(asset.assetId(), \"ORDER\", asset.storageOwnerId())"));
        assertTrue(controller.contains("CacheControl.noStore()"));
        assertFalse(controller.contains("com.xinghuo.upload.infrastructure"));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/PublicApiController.java")));
    }
}
