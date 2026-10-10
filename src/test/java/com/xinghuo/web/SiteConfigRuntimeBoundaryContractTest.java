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
    void persistenceAdapterOwnsSiteConfigJpaAccess() throws IOException {
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
    void legacyPublicProjectionDelegatesOperatingStatusToRuntimeBoundary() throws IOException {
        String legacy = read("src/main/java/com/xinghuo/service/SiteConfigService.java");
        assertTrue(legacy.contains("com.xinghuo.siteconfig.application.SiteRuntimeApplicationService"));
        assertTrue(legacy.contains("runtimeConfig.operatingStatus()"));
        assertFalse(legacy.contains("com.xinghuo.gamestatus.application.GameStatusApplicationService"));
        assertFalse(legacy.contains("com.xinghuo.gamestatus.application.GameStatusView"));
        assertFalse(legacy.contains("mw4Status("));
        assertFalse(legacy.contains("mw4ShortText("));
        assertFalse(legacy.contains("mw4CtaLabel("));
    }

    @Test
    void largePublicConfigProjectionRemainsOnLegacyServiceForThisPhase() throws IOException {
        String publicApi = read("src/main/java/com/xinghuo/controller/PublicApiController.java");
        assertTrue(publicApi.contains("com.xinghuo.service.SiteConfigService"));
        assertTrue(publicApi.contains("config.publicConfig()"));
        assertTrue(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/service/SiteConfigService.java")));
    }
}
