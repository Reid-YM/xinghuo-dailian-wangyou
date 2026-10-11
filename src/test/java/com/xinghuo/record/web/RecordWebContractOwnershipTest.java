package com.xinghuo.record.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RecordWebContractOwnershipTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void recordResponseIsOwnedByRecordWeb() throws IOException {
        Path main = ROOT.resolve("src/main/java/com/xinghuo");
        String response = Files.readString(main.resolve("record/web/RecordPublicResponse.java"));
        String controller = Files.readString(main.resolve("record/web/PublicRecordController.java"));
        String mapper = Files.readString(main.resolve("record/web/RecordWebMapper.java"));

        assertTrue(response.contains("String businessNo"));
        assertTrue(response.contains("String stage"));
        assertTrue(response.contains("String status"));
        assertTrue(response.contains("String title"));
        assertTrue(response.contains("String expectedTime"));
        assertTrue(response.contains("Integer progressPercent"));
        assertTrue(response.contains("String progressText"));
        assertTrue(response.contains("Instant createdAt"));
        assertTrue(response.contains("Instant updatedAt"));
        assertTrue(response.contains("boolean hasProgressImage"));
        assertTrue(response.contains("String customerNote"));
        assertFalse(controller.contains("ApiDtos.RecordPublicResponse"));
        assertFalse(mapper.contains("ApiDtos.RecordPublicResponse"));
    }
}
