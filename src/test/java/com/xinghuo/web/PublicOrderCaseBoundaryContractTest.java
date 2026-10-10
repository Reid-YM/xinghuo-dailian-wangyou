package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PublicOrderCaseBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void publicOrderCasesAreOwnedByOrderWebBoundary() throws Exception {
        String controller = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/order/web/PublicOrderCaseController.java"));
        String legacyPublicApi = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/controller/PublicApiController.java"));

        assertTrue(controller.contains("package com.xinghuo.order.web;"));
        assertTrue(controller.contains("com.xinghuo.order.application.OrderApplicationService"));
        assertTrue(controller.contains("@RequestMapping(value={\"/api/public/cases\"})"));
        assertTrue(controller.contains("OrderWebMapper.toCaseResponses(this.orders.publicCases())"));
        assertFalse(legacyPublicApi.contains("@GetMapping(value={\"/public/cases\"})"));
    }
}
