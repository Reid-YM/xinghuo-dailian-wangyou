package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiIntegrationsContractTest {
    @Test
    void integrationPageUsesPermissionsAndKeepsSecretsServerSide() throws Exception {
        String controller = Files.readString(Path.of("src/main/java/com/xinghuo/controller/XingweiIntegrationsController.java"));
        String template = Files.readString(Path.of("src/main/resources/templates/admin-xingwei-integrations.html"));

        assertTrue(controller.contains("@PreAuthorize(\"@xingweiPermissionGuard.hasAny(authentication, 'employee:view', 'payment:view')\")"));
        assertTrue(controller.contains("permissions.has(authentication, \"employee:view\")"));
        assertTrue(controller.contains("permissions.has(authentication, \"payment:view\")"));
        assertTrue(template.contains("${weCom != null}"));
        assertTrue(template.contains("${payment != null}"));
        assertFalse(template.contains("weCom.appSecret"));
        assertFalse(template.contains("payment.apiV3Key"));
    }
}
