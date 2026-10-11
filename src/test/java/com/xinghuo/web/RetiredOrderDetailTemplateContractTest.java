package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RetiredOrderDetailTemplateContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void legacyOrderRouteUsesCanonicalRecordDetailTemplate() throws Exception {
        String controller = Files.readString(MAIN.resolve("java/com/xinghuo/controller/PageController.java"));
        assertTrue(controller.contains("@GetMapping(value={\"/order/{orderNo}\"})"));
        assertTrue(controller.contains("String legacyOrderDetail(@PathVariable String orderNo, Model m)"));
        assertTrue(controller.contains("m.addAttribute(\"businessNo\", orderNo);"));
        assertTrue(controller.contains("return \"record-detail\";"));
    }

    @Test
    void duplicatedOrderDetailTemplateStaysRetired() {
        assertFalse(
                Files.exists(MAIN.resolve("resources/templates/order-detail.html")),
                "legacy order-detail.html duplicated the shared shell and must not be restored");
    }
}
