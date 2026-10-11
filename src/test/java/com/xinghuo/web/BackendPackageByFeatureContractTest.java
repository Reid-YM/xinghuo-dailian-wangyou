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
        assertFalse(Files.exists(MAIN.resolve("model")), "global model package must stay retired");
    }

    @Test
    void productionCodeDoesNotImportRetiredHorizontalPackages() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("import com.xinghuo.controller."), file + " imports retired controller package");
                assertFalse(source.contains("import com.xinghuo.repository."), file + " imports retired repository package");
                assertFalse(source.contains("import com.xinghuo.service."), file + " imports retired service package");
                assertFalse(source.contains("import com.xinghuo.model."), file + " imports retired model package");
            }
        }
    }

    @Test
    void apiExceptionAdviceUsesFeatureWebScopesWithoutTakingOverAccountApis() throws IOException {
        String apiAdvice = Files.readString(MAIN.resolve("exception/ApiExceptionHandler.java"));
        String accountAdvice = Files.readString(MAIN.resolve("account/web/AccountExceptionHandler.java"));
        int adviceStart = apiAdvice.indexOf("@RestControllerAdvice(");
        int classStart = apiAdvice.indexOf("public class ApiExceptionHandler");
        assertTrue(adviceStart >= 0 && classStart > adviceStart, "API advice scope must be explicit");
        String adviceScope = apiAdvice.substring(adviceStart, classStart);

        assertTrue(adviceScope.contains("basePackages={"));
        for (String feature : List.of("quote", "order", "catalog", "guide", "seo", "siteconfig", "upload")) {
            assertTrue(adviceScope.contains("\"com.xinghuo." + feature + ".web\""), feature + " web APIs must keep shared error mapping");
        }
        assertFalse(adviceScope.contains("com.xinghuo.account.web"), "account APIs have their own exception advice");
        assertFalse(adviceScope.contains("assignableTypes"), "shared advice must not depend on retired controller classes");
        assertFalse(apiAdvice.contains("import com.xinghuo.controller."));
        assertTrue(accountAdvice.contains("@RestControllerAdvice(basePackages = \"com.xinghuo.account.web\")"));
    }

    @Test
    void applicationAndDomainLayersStayPersistenceAndProtocolIndependent() throws IOException {
        for (String feature : CORE_FEATURES) {
            for (String layer : List.of("application", "domain")) {
                Path directory = MAIN.resolve(feature).resolve(layer);
                try (Stream<Path> files = Files.walk(directory)) {
                    for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                        String source = Files.readString(file);
                        assertFalse(source.contains("import com.xinghuo.dto"), file + " depends on protocol DTOs");
                        assertFalse(source.contains("import com.xinghuo.model"), file + " depends on global persistence entities");
                        assertFalse(source.contains("import com.xinghuo.repository"), file + " depends on retired repositories");
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
                "sitesetting/infrastructure/SiteSettingRepository.java",
                "upload/infrastructure/UploadAssetRepository.java",
                "taxonomy/infrastructure/GameCatalogRepository.java",
                "taxonomy/infrastructure/ServiceCategoryCatalogRepository.java")) {
            assertTrue(Files.isRegularFile(MAIN.resolve(path)), path + " must stay feature-owned");
        }
    }
}
