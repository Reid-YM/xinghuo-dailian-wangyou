package com.xinghuo.taxonomy.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TaxonomyWebContractOwnershipTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void taxonomyContractsAreOwnedByTaxonomyWeb() throws IOException {
        Path web = ROOT.resolve("src/main/java/com/xinghuo/taxonomy/web");
        String catalogName = Files.readString(web.resolve("CatalogNameRequest.java"));
        String profile = Files.readString(web.resolve("GameProfileUpdateRequest.java"));
        String bulk = Files.readString(web.resolve("GameBulkUpdateRequest.java"));
        String catalogResponse = Files.readString(web.resolve("CatalogResponse.java"));
        String gameResponse = Files.readString(web.resolve("GameOptionResponse.java"));
        String mapper = Files.readString(web.resolve("TaxonomyWebMapper.java"));
        String publicController = Files.readString(web.resolve("PublicTaxonomyCatalogController.java"));
        String adminController = Files.readString(web.resolve("AdminTaxonomyController.java"));

        assertTrue(catalogName.contains("@NotBlank @Size(max=80) @NotBlank @Size(max=80) String name"));
        assertTrue(profile.contains("@Pattern(regexp=\"[a-z0-9-]{2,120}\") @Pattern(regexp=\"[a-z0-9-]{2,120}\") String seoSlug"));
        assertTrue(bulk.contains("@NotEmpty @Size(max=100) @NotEmpty @Size(max=100) List<@Size(max=32) String> codes"));
        assertTrue(catalogResponse.contains("List<GameOptionResponse> games"));
        assertTrue(catalogResponse.contains("List<GamePlatformResponse> gamePlatforms"));
        assertTrue(gameResponse.contains("List<String> platformCodes"));
        assertFalse(mapper.contains("com.xinghuo.dto.ApiDtos"));
        assertFalse(publicController.contains("com.xinghuo.dto.ApiDtos"));
        assertFalse(adminController.contains("ApiDtos.CatalogResponse"));
        assertFalse(adminController.contains("ApiDtos.GameOptionResponse"));
        assertFalse(adminController.contains("ApiDtos.CategoryOptionResponse"));
        assertFalse(adminController.contains("ApiDtos.CatalogNameRequest"));
        assertFalse(adminController.contains("ApiDtos.GameProfileUpdateRequest"));
        assertTrue(adminController.contains("ApiDtos.SiteImageUploadResponse"));
    }
}
