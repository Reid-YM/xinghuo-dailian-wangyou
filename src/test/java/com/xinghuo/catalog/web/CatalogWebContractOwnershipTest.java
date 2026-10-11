package com.xinghuo.catalog.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CatalogWebContractOwnershipTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void catalogServiceContractsAreOwnedByCatalogWeb() throws IOException {
        Path web = ROOT.resolve("src/main/java/com/xinghuo/catalog/web");
        String request = Files.readString(web.resolve("ServiceUpsert.java"));
        String publicResponse = Files.readString(web.resolve("ServiceResponse.java"));
        String adminResponse = Files.readString(web.resolve("AdminServiceResponse.java"));
        String uploadResponse = Files.readString(web.resolve("SiteImageUploadResponse.java"));
        String publicMapper = Files.readString(web.resolve("CatalogWebMapper.java"));
        String adminMapper = Files.readString(web.resolve("CatalogAdminWebMapper.java"));
        String publicController = Files.readString(web.resolve("PublicCatalogServiceController.java"));
        String adminController = Files.readString(web.resolve("AdminCatalogServiceController.java"));

        assertTrue(request.contains("@NotBlank @Size(max=32) @NotBlank @Size(max=32) String gameCode"));
        assertTrue(request.contains("@NotBlank @Pattern(regexp=\"[a-z0-9-]{2,100}\") @NotBlank @Pattern(regexp=\"[a-z0-9-]{2,100}\") String slug"));
        assertTrue(request.contains("@Size(max=500) @Size(max=500) String reason"));
        assertTrue(publicResponse.contains("BigDecimal priceFixed"));
        assertTrue(publicResponse.contains("String coverImage"));
        assertTrue(adminResponse.contains("BigDecimal hotScore"));
        assertTrue(adminResponse.contains("long version"));
        assertTrue(uploadResponse.contains("record SiteImageUploadResponse(String url)"));
        assertFalse(publicMapper.contains("com.xinghuo.dto.ApiDtos"));
        assertFalse(adminMapper.contains("com.xinghuo.dto.ApiDtos"));
        assertFalse(publicController.contains("com.xinghuo.dto.ApiDtos"));
        assertFalse(adminController.contains("com.xinghuo.dto.ApiDtos"));
        assertTrue(adminController.contains("SiteImageUploadResponse uploadServiceCover"));
    }
}
