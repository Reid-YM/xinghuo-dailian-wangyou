package com.xinghuo.audit.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AuditWebContractOwnershipTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void auditResponseIsOwnedByAuditWeb() throws IOException {
        Path main = ROOT.resolve("src/main/java/com/xinghuo");
        String response = Files.readString(main.resolve("audit/web/AuditResponse.java"));
        String controller = Files.readString(main.resolve("audit/web/AdminAuditController.java"));

        assertTrue(response.contains("Long id"));
        assertTrue(response.contains("String action"));
        assertTrue(response.contains("String targetType"));
        assertTrue(response.contains("String targetId"));
        assertTrue(response.contains("String operatorName"));
        assertTrue(response.contains("String reason"));
        assertTrue(response.contains("String ipAddress"));
        assertTrue(response.contains("Instant createdAt"));
        assertFalse(controller.contains("ApiDtos.AuditResponse"));
        assertFalse(controller.contains("import com.xinghuo.dto.ApiDtos"));
    }
}
