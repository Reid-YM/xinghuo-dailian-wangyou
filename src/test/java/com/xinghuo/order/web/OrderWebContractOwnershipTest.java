package com.xinghuo.order.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OrderWebContractOwnershipTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void orderContractsAreOwnedByOrderWeb() throws IOException {
        Path web = ROOT.resolve("src/main/java/com/xinghuo/order/web");
        String create = Files.readString(web.resolve("CreateOrderRequest.java"));
        String update = Files.readString(web.resolve("UpdateOrderRequest.java"));
        String publicResponse = Files.readString(web.resolve("OrderPublicResponse.java"));
        String adminResponse = Files.readString(web.resolve("AdminOrderResponse.java"));
        String mapper = Files.readString(web.resolve("OrderWebMapper.java"));
        String publicController = Files.readString(web.resolve("PublicOrderController.java"));
        String adminController = Files.readString(web.resolve("AdminOrderController.java"));

        assertTrue(create.contains("@Min(value=0L) @Max(value=100L) @Min(value=0L) @Max(value=100L) Integer progressPercent"));
        assertTrue(create.contains("@NotBlank @Size(max=240) @NotBlank @Size(max=240) String progressText"));
        assertTrue(update.contains("@NotNull Long version"));
        assertTrue(update.contains("@Size(max=1200) @Size(max=1200) String internalNote"));
        assertTrue(publicResponse.contains("String businessNo"));
        assertTrue(publicResponse.contains("boolean hasProgressImage"));
        assertTrue(adminResponse.contains("UUID sourceQuoteId"));
        assertTrue(adminResponse.contains("long version"));
        assertFalse(mapper.contains("com.xinghuo.dto.ApiDtos"));
        assertFalse(publicController.contains("com.xinghuo.dto.ApiDtos"));
        assertFalse(adminController.contains("com.xinghuo.dto.ApiDtos"));
    }
}
