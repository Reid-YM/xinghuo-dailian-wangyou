package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SiteConfigRuntimeBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void siteConfigRuntimeFeatureHasExplicitBoundaries() {
        Path base = ROOT.resolve("src/main/java/com/xinghuo/siteconfig");
        assertTrue(Files.isDirectory(base.resolve("application")));
        assertTrue(Files.isDirectory(base.resolve("domain")));
        assertTrue(Files.isDirectory(base.resolve("infrastructure")));
        assertTrue(Files.isDirectory(base.resolve("web")));
    }

    @Test
    void applicationAndDomainStayPersistenceIndependent() throws IOException {
        for (String layer : new String[] {"application", "domain"}) {
            Path directory = ROOT.resolve("src/main/java/com/xinghuo/siteconfig/" + layer);
            try (Stream<Path> files = Files.list(directory)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file);
                    assertFalse(source.contains("com.xinghuo.dto"));
                    assertFalse(source.contains("com.xinghuo.model"));
                    assertFalse(source.contains("com.xinghuo.repository"));
                    assertFalse(source.contains("com.xinghuo.service"));
                    assertFalse(source.contains("jakarta.persistence"));
                    assertFalse(source.contains("jakarta.servlet"));
                }
            }
        }
    }

    @Test
    void runtimePersistenceAdapterOwnsRuntimeJpaAccess() throws IOException {
        String adapter = read("src/main/java/com/xinghuo/siteconfig/infrastructure/SiteRuntimePersistenceAdapter.java");
        assertTrue(adapter.contains("implements SiteRuntimePort"));
        assertTrue(adapter.contains("com.xinghuo.repository.SiteConfigRepository"));
        assertTrue(adapter.contains("com.xinghuo.model.SiteConfig"));
        assertTrue(adapter.contains("findById(1L)"));
        assertTrue(adapter.contains("SiteOperatingStatusCode.valueOf(config.getServiceStatus().name())"));
        assertTrue(adapter.contains("config.getStatusShortText()"));
        assertTrue(adapter.contains("config.getPrimaryButtonLabel()"));
        assertTrue(adapter.contains("@Transactional(readOnly = true)"));
    }

    @Test
    void publicQueryPersistenceAdapterOwnsProjectionParsingAndPricing() throws IOException {
        String adapter = read("src/main/java/com/xinghuo/siteconfig/infrastructure/SiteConfigPublicPersistenceAdapter.java");
        assertTrue(adapter.contains("implements SiteConfigPublicPort"));
        assertTrue(adapter.contains("com.xinghuo.repository.SiteConfigRepository"));
        assertTrue(adapter.contains("com.xinghuo.repository.ServiceItemRepository"));
        assertTrue(adapter.contains("displayStartingPrice"));
        assertTrue(adapter.contains("parseContactChannels"));
        assertTrue(adapter.contains("parseTrustCases"));
        assertTrue(adapter.contains("parseTrustFaq"));
        assertTrue(adapter.contains("@Transactional(readOnly = true)"));
        assertFalse(adapter.contains("com.xinghuo.dto"));
    }

    @Test
    void publicRuntimeDecisionsUseApplicationBoundary() throws IOException {
        String publicApi = read("src/main/java/com/xinghuo/controller/PublicApiController.java");
        assertTrue(publicApi.contains("com.xinghuo.siteconfig.application.SiteRuntimeApplicationService"));
        assertTrue(publicApi.contains("runtimeConfig.quoteEnabled()"));
        assertTrue(publicApi.contains("runtimeConfig.orderQueryEnabled()"));
        assertFalse(publicApi.contains("config.isQuoteEnabled()"));
        assertFalse(publicApi.contains("config.isOrderQueryEnabled()"));

        String page = read("src/main/java/com/xinghuo/controller/PageController.java");
        assertTrue(page.contains("com.xinghuo.siteconfig.application.SiteRuntimeApplicationService"));
        assertTrue(page.contains("runtimeConfig.acceptingOrders()"));
        assertFalse(page.contains("com.xinghuo.service.SiteConfigService"));
        assertFalse(page.contains("config.publicConfig()"));
    }

    @Test
    void publicSiteConfigEndpointUsesQueryBoundaryAndWebMapper() throws IOException {
        String publicApi = read("src/main/java/com/xinghuo/controller/PublicApiController.java");
        assertTrue(publicApi.contains("com.xinghuo.siteconfig.application.SiteConfigPublicApplicationService"));
        assertTrue(publicApi.contains("com.xinghuo.siteconfig.web.SiteConfigWebMapper"));
        assertTrue(publicApi.contains("SiteConfigWebMapper.toResponse(this.siteConfig.get())"));
        assertFalse(publicApi.contains("com.xinghuo.service.SiteConfigService"));
        assertFalse(publicApi.contains("config.publicConfig()"));

        String mapper = read("src/main/java/com/xinghuo/siteconfig/web/SiteConfigWebMapper.java");
        assertTrue(mapper.contains("com.xinghuo.dto.ApiDtos"));
        assertTrue(mapper.contains("ApiDtos.SiteConfigResponse"));
    }

    @Test
    void legacyProjectionStillSupportsAdminWritesForThisPhase() throws IOException {
        String legacy = read("src/main/java/com/xinghuo/service/SiteConfigService.java");
        assertTrue(legacy.contains("runtimeConfig.operatingStatus()"));
        assertFalse(legacy.contains("com.xinghuo.gamestatus.application.GameStatusApplicationService"));
        assertFalse(legacy.contains("com.xinghuo.gamestatus.application.GameStatusView"));
        assertTrue(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/service/SiteConfigService.java")));

        String admin = read("src/main/java/com/xinghuo/controller/AdminApiController.java");
        assertTrue(admin.contains("com.xinghuo.service.SiteConfigService"));
        assertTrue(admin.contains("config.publicConfig()"));
    }
}
