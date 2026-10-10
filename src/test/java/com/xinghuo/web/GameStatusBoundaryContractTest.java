package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class GameStatusBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void gameStatusFeatureHasExplicitBoundaries() {
        Path base = ROOT.resolve("src/main/java/com/xinghuo/gamestatus");
        assertTrue(Files.isDirectory(base.resolve("application")));
        assertTrue(Files.isDirectory(base.resolve("domain")));
        assertTrue(Files.isDirectory(base.resolve("infrastructure")));
        assertTrue(Files.isDirectory(base.resolve("web")));
    }

    @Test
    void applicationAndDomainStayPersistenceAndProtocolIndependent() throws IOException {
        Path application = ROOT.resolve("src/main/java/com/xinghuo/gamestatus/application");
        try (Stream<Path> files = Files.list(application)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("com.xinghuo.dto"));
                assertFalse(source.contains("com.xinghuo.model"));
                assertFalse(source.contains("com.xinghuo.repository"));
                assertFalse(source.contains("jakarta.servlet"));
                assertFalse(source.contains("jakarta.persistence"));
            }
        }

        String domain = read("src/main/java/com/xinghuo/gamestatus/domain/package-info.java");
        assertFalse(domain.contains("org.springframework"));
        assertFalse(domain.contains("jakarta.persistence"));
        assertFalse(domain.contains("jakarta.servlet"));
        assertFalse(domain.contains("com.xinghuo.repository"));
        assertFalse(domain.contains("com.xinghuo.service"));
    }

    @Test
    void publicAdminAndSiteConfigRuntimeConsumeGameStatusBoundary() throws IOException {
        String publicApi = read("src/main/java/com/xinghuo/gamestatus/web/PublicGameStatusController.java");
        assertTrue(publicApi.contains("package com.xinghuo.gamestatus.web;"));
        assertTrue(publicApi.contains("com.xinghuo.gamestatus.application.GameStatusApplicationService"));
        assertTrue(publicApi.contains("GameStatusWebMapper.toPublicResponse"));
        assertTrue(publicApi.contains("GameStatusWebMapper::toSummaryResponse"));
        assertTrue(publicApi.contains("@RequestMapping(value={\"/api/public/game-status\"})"));
        assertTrue(publicApi.contains("com.xinghuo.taxonomy.application.TaxonomyApplicationService"));
        assertFalse(publicApi.contains("import com.xinghuo.model.GameStatus;"));
        assertFalse(publicApi.contains("import com.xinghuo.service.GameStatusService;"));

        String legacyPublicApi = read("src/main/java/com/xinghuo/controller/PublicApiController.java");
        assertFalse(legacyPublicApi.contains("GameStatusApplicationService"));
        assertFalse(legacyPublicApi.contains("/public/game-status"));

        String admin = read("src/main/java/com/xinghuo/controller/AdminApiController.java");
        assertTrue(admin.contains("com.xinghuo.gamestatus.application.GameStatusApplicationService"));
        assertTrue(admin.contains("GameStatusWebMapper.toCommand"));
        assertTrue(admin.contains("GameStatusWebMapper.toAdminResponse"));
        assertFalse(admin.contains("import com.xinghuo.model.GameStatus;"));
        assertFalse(admin.contains("import com.xinghuo.service.GameStatusService;"));

        String runtime = read("src/main/java/com/xinghuo/siteconfig/application/SiteRuntimeApplicationService.java");
        assertTrue(runtime.contains("com.xinghuo.gamestatus.application.GameStatusApplicationService"));
        assertTrue(runtime.contains("com.xinghuo.gamestatus.application.GameStatusView"));
        assertTrue(runtime.contains("gameStatuses.resolve(\"CODMW4\", null)"));

        String publicQuery = read("src/main/java/com/xinghuo/siteconfig/application/SiteConfigPublicApplicationService.java");
        assertTrue(publicQuery.contains("SiteRuntimeApplicationService"));
        assertTrue(publicQuery.contains("runtimeConfig.operatingStatus()"));

        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/service/SiteConfigService.java")));
    }

    @Test
    void persistenceAdapterOwnsJpaValidationAndAudit() throws IOException {
        String adapter = read("src/main/java/com/xinghuo/gamestatus/infrastructure/GameStatusPersistenceAdapter.java");
        assertTrue(adapter.contains("implements GameStatusPort"));
        assertTrue(adapter.contains("com.xinghuo.repository.GameStatusRepository"));
        assertTrue(adapter.contains("com.xinghuo.model.GameStatus"));
        assertTrue(adapter.contains("com.xinghuo.audit.infrastructure.AuditService"));
        assertTrue(adapter.contains("GAME_STATUS_UPDATE"));
        assertFalse(adapter.contains("com.xinghuo.dto"));
        assertFalse(adapter.contains("com.xinghuo.service.GameStatusService"));
    }

    @Test
    void aliasesAndPublicStatusSemanticsStayOwnedByApplication() throws IOException {
        String code = read("src/main/java/com/xinghuo/gamestatus/application/GameStatusCode.java");
        assertTrue(code.contains("BUSY"));
        assertTrue(code.contains("RESERVATION"));
        assertTrue(code.contains("PREHEAT"));
        assertTrue(code.contains("SUSPENDED"));
        assertTrue(code.contains("CLOSED"));
        assertTrue(code.contains("acceptingOrders"));
    }

    @Test
    void legacyGameStatusServiceStaysRetired() throws IOException {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/service/GameStatusService.java")));
        try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("com.xinghuo.service.GameStatusService"),
                        file + " still references retired GameStatusService");
            }
        }
    }
}
