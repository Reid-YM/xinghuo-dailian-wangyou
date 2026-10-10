package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PublicOrderQueryBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void publicOrderQueryIsOwnedByOrderWebBoundary() throws Exception {
        String controller = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/order/web/PublicOrderController.java"));
        String legacyPublicApi = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/controller/PublicApiController.java"));

        assertTrue(controller.contains("package com.xinghuo.order.web;"));
        assertTrue(controller.contains("@RequestMapping(value={\"/api/orders\"})"));
        assertTrue(controller.contains("@GetMapping(value={\"/{orderNo}\"})"));
        assertTrue(controller.contains("runtimeConfig.orderQueryEnabled()"));
        assertTrue(controller.contains("pub:order"));
        assertTrue(controller.contains("REQUEST_BUCKET_PER_MINUTE = 60"));
        assertTrue(controller.contains("Duration.ofMinutes(1L)"));
        assertTrue(controller.contains("OrderWebMapper.toResponse(this.orders.getPublic(orderNo))"));
        assertFalse(legacyPublicApi.contains("@GetMapping(value={\"/orders/{orderNo}\"})"));
        assertTrue(legacyPublicApi.contains("@GetMapping(value={\"/orders/{orderNo}/image\"})"));
    }
}
