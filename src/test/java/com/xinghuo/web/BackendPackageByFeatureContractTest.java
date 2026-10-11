package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class BackendPackageByFeatureContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    private static final Path MAIN = ROOT.resolve("src/main/java/com/xinghuo");
    private static final List<String> CORE_FEATURES = List.of(
            "quote", "order", "catalog", "record", "dashboard", "feedback", "taxonomy", "slot", "siteconfig");

    @Test
    void coreFeatureModulesHaveExplicitBoundaries() {
        for (String feature : CORE_FEATURES) {
            Path base = MAIN.resolve(feature);
            assertTrue(Files.isDirectory(base.resolve("application")), feature + " application boundary must exist");
            assertTrue(Files.isDirectory(base.resolve("domain")), feature + " domain boundary must exist");
            assertTrue(Files.isDirectory(base.resolve("infrastructure")), feature + " infrastructure boundary must exist");
            assertTrue(Files.isDirectory(base.resolve("web")), feature + " web boundary must exist");
        }
    }

    @Test
    void retiredHorizontalPackagesStayPhysicallyRetired() {
        assertFalse(Files.exists(MAIN.resolve("controller")), "global controller package must stay retired");
        assertFalse(Files.exists(MAIN.resolve("repository")), "global repository package must stay retired");
        assertFalse(Files.exists(MAIN.resolve("service")), "global service package must stay retired");
    }

    @Test
    void productionCodeDoesNotReferenceRetiredHorizontalPackages() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("com.xinghuo.controller."), file + " references retired controller package");
                assertFalse(source.contains("com.xinghuo.repository."), file + " references retired repository package");
                assertFalse(source.contains("com.xinghuo.service."), file + " references retired service package");
            }
        }
    }

    @Test
    void applicationAndDomainLayersStayPersistenceAndProtocolIndependent() throws IOException {
        for (String feature : CORE_FEATURES) {
            for (String layer : List.of("application", "domain")) {
                Path directory = MAIN.resolve(feature).resolve(layer);
                try (Stream<Path> files = Files.walk(directory)) {
                    for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                        String source = Files.readString(file);
                        assertFalse(source.contains("com.xinghuo.dto"), file + " depends on protocol DTOs");
                        assertFalse(source.contains("com.xinghuo.model"), file + " depends on global persistence entities");
                        assertFalse(source.contains("com.xinghuo.repository"), file + " depends on retired repositories");
                        assertFalse(source.contains("jakarta.persistence"), file + " depends on JPA");
                        assertFalse(source.contains("jakarta.servlet"), file + " depends on servlet APIs");
                        assertFalse(source.contains("org.springframework.data."), file + " depends on Spring Data");
                    }
                }
            }
        }
    }

    @Test
    void migratedRepositoriesAreOwnedByFeatureInfrastructure() {
        for (String path : List.of(
                "security/infrastructure/AdminUserRepository.java",
                "quote/infrastructure/QuoteRepository.java",
                "order/infrastructure/CustomerOrderRepository.java",
                "catalog/infrastructure/ServiceItemRepository.java",
                "siteconfig/infrastructure/SiteConfigRepository.java",
                "upload/infrastructure/UploadAssetRepository.java",
                "taxonomy/infrastructure/GameCatalogRepository.java",
                "taxonomy/infrastructure/ServiceCategoryCatalogRepository.java")) {
            assertTrue(Files.isRegularFile(MAIN.resolve(path)), path + " must stay feature-owned");
        }
    }
}
