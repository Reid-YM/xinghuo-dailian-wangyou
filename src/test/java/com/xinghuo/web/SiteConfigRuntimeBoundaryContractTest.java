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
    void siteConfigFeatureHasExplicitBoundaries() {
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
            try (Stream<Path> files = Files.walk(directory)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file);
                    assertFalse(source.contains("com.xinghuo.dto"));
                    assertFalse(source.contains("com.xinghuo.model"));
                    assertFalse(source.contains("com.xinghuo.repository"));
                    assertFalse(source.contains("com.xinghuo.service"));
                    assertFalse(source.contains("jakarta.persistence"));
                    assertFalse(source.contains("jakarta.servlet"));
                    assertFalse(source.contains("org.springframework.data."));
                }
            }
        }
    }

    @Test
    void runtimePersistenceAdapterOwnsRuntimeJpaAccess() throws IOException {
        String adapter = read("src/main/java/com/xinghuo/siteconfig/infrastructure/SiteRuntimePersistenceAdapter.java");
        assertTrue(adapter.contains("implements SiteRuntimePort"));
        assertTrue(adapter.contains("private final SiteConfigRepository repo;"));
        assertTrue(adapter.contains("SiteConfig config = repo.findById(1L).orElseThrow()"));
        assertTrue(adapter.contains("SiteOperatingStatusCode.valueOf(config.getServiceStatus().name())"));
        assertTrue(adapter.contains("config.getStatusShortText()"));
        assertTrue(adapter.contains("config.getPrimaryButtonLabel()"));
        assertTrue(adapter.contains("@Transactional(readOnly = true)"));
        assertFalse(adapter.contains("com.xinghuo.model.SiteConfig"));
        assertFalse(adapter.contains("com.xinghuo.repository.SiteConfigRepository"));
    }

    @Test
    void publicQueryPersistenceAdapterOwnsProjectionParsingAndPricing() throws IOException {
        String adapter = read("src/main/java/com/xinghuo/siteconfig/infrastructure/SiteConfigPublicPersistenceAdapter.java");
        assertTrue(adapter.contains("implements SiteConfigPublicPort"));
        assertTrue(adapter.contains("private final SiteConfigRepository repo;"));
        assertTrue(adapter.contains("com.xinghuo.catalog.infrastructure.ServiceItemRepository"));
        assertTrue(adapter.contains("displayStartingPrice"));
        assertTrue(adapter.contains("parseContactChannels"));
        assertTrue(adapter.contains("parseTrustCases"));
        assertTrue(adapter.contains("parseTrustFaq"));
        assertTrue(adapter.contains("@Transactional(readOnly = true)"));
        assertFalse(adapter.contains("com.xinghuo.dto"));
        assertFalse(adapter.contains("com.xinghuo.model.SiteConfig"));
        assertFalse(adapter.contains("com.xinghuo.repository."));
    }

    @Test
    void writePersistenceAdapterOwnsJpaMutationRules() throws IOException {
        String adapter = read("src/main/java/com/xinghuo/siteconfig/infrastructure/SiteConfigWritePersistenceAdapter.java");
        assertTrue(adapter.contains("implements SiteConfigWritePort"));
        assertTrue(adapter.contains("private final SiteConfigRepository repo;"));
        assertTrue(adapter.contains("SiteConfig config = requireConfig()"));
        assertTrue(adapter.contains("updateSiteImage"));
        assertTrue(adapter.contains("updateSection"));
        assertTrue(adapter.contains("serializeContactChannels"));
        assertTrue(adapter.contains("serializeTrustCases"));
        assertTrue(adapter.contains("serializeTrustFaq"));
        assertFalse(adapter.contains("com.xinghuo.model.SiteConfig"));
        assertFalse(adapter.contains("com.xinghuo.dto"));
        assertFalse(adapter.contains("jakarta.servlet"));
        assertFalse(adapter.contains("com.xinghuo.repository."));
        assertFalse(adapter.contains("com.xinghuo.service."));
    }

    @Test
    void publicRuntimeDecisionsUseSiteConfigApplicationBoundary() throws IOException {
        String publicQuote = read("src/main/java/com/xinghuo/quote/web/PublicQuoteController.java");
        assertTrue(publicQuote.contains("com.xinghuo.siteconfig.application.SiteRuntimeApplicationService"));
        assertTrue(publicQuote.contains("runtimeConfig.quoteEnabled()"));
        assertFalse(publicQuote.contains("config.isQuoteEnabled()"));

        String publicOrder = read("src/main/java/com/xinghuo/order/web/PublicOrderController.java");
        assertTrue(publicOrder.contains("com.xinghuo.siteconfig.application.SiteRuntimeApplicationService"));
        assertTrue(publicOrder.contains("runtimeConfig.orderQueryEnabled()"));
        assertFalse(publicOrder.contains("config.isOrderQueryEnabled()"));
    }

    @Test
    void publicSiteConfigEndpointUsesQueryBoundaryAndOwnedWebMapper() throws IOException {
        String publicApi = read("src/main/java/com/xinghuo/siteconfig/web/PublicSiteConfigController.java");
        assertTrue(publicApi.contains("package com.xinghuo.siteconfig.web;"));
        assertTrue(publicApi.contains("com.xinghuo.siteconfig.application.SiteConfigPublicApplicationService"));
        assertTrue(publicApi.contains("@RequestMapping(value={\"/api/site-config\"})"));
        assertTrue(publicApi.contains("SiteConfigWebMapper.toResponse(this.siteConfig.get())"));
        assertTrue(publicApi.contains("SiteConfigResponse config()"));
        assertFalse(publicApi.contains("com.xinghuo.dto.ApiDtos"));

        String mapper = read("src/main/java/com/xinghuo/siteconfig/web/SiteConfigWebMapper.java");
        assertFalse(mapper.contains("com.xinghuo.dto.ApiDtos"));
        assertFalse(mapper.contains("SlotWebMapper"));
        assertTrue(mapper.contains("new SiteConfigResponse("));
        assertTrue(mapper.contains("SlotPublicServiceView"));
    }

    @Test
    void siteConfigWebOwnsEstablishedProtocolContracts() throws IOException {
        String response = read("src/main/java/com/xinghuo/siteconfig/web/SiteConfigResponse.java");
        String update = read("src/main/java/com/xinghuo/siteconfig/web/SiteConfigUpdate.java");
        String service = read("src/main/java/com/xinghuo/siteconfig/web/ServiceResponse.java");
        assertTrue(response.contains("List<ServiceResponse> hotServices"));
        assertTrue(response.contains("String heroBackgroundMobileImage"));
        assertTrue(response.contains("String wecomKefuUrl"));
        assertTrue(update.contains("@NotNull @DecimalMin(value=\"0.00\") @NotNull @DecimalMin(value=\"0.00\") BigDecimal startingPrice"));
        assertTrue(update.contains("@Min(value=0L) @Max(value=24L) @Min(value=0L) @Max(value=24L) int heroBackgroundBlur"));
        assertTrue(update.contains("@Size(max=500) @Size(max=500) String reason"));
        assertTrue(service.contains("BigDecimal priceFixed"));
    }

    @Test
    void adminReadAndWriteFlowsUseSiteConfigFeatureBoundaries() throws IOException {
        String admin = read("src/main/java/com/xinghuo/siteconfig/web/AdminSiteConfigController.java");
        assertTrue(admin.contains("package com.xinghuo.siteconfig.web;"));
        assertTrue(admin.contains("SiteConfigPublicApplicationService"));
        assertTrue(admin.contains("SiteConfigWriteWebService"));
        assertTrue(admin.contains("SiteConfigWebMapper.toResponse(this.siteConfigQuery.get())"));
        assertTrue(admin.contains("configWrites.updateSection("));
        assertTrue(admin.contains("configWrites.update("));
        assertTrue(admin.contains("configWrites.updateSiteImage("));
        assertTrue(admin.contains("@RequestMapping(value={\"/api/admin/site-config\"})"));
        assertTrue(admin.contains("@RequestBody SiteConfigUpdate r"));
        assertFalse(admin.contains("com.xinghuo.dto.ApiDtos"));
        assertFalse(admin.contains("com.xinghuo.service.SiteConfigService"));
    }

    @Test
    void webWriteOrchestratorOwnsProtocolAuditAndTransactionalSnapshots() throws IOException {
        String web = read("src/main/java/com/xinghuo/siteconfig/web/SiteConfigWriteWebService.java");
        assertFalse(web.contains("com.xinghuo.dto.ApiDtos"));
        assertTrue(web.contains("com.xinghuo.audit.infrastructure.AuditService"));
        assertTrue(web.contains("jakarta.servlet.http.HttpServletRequest"));
        assertTrue(web.contains("@Transactional"));
        assertTrue(web.contains("SITE_IMAGE_UPDATE"));
        assertTrue(web.contains("SITE_CONFIG_UPDATE"));
        assertTrue(web.contains("SiteConfigResponse before = current()"));
        assertTrue(web.contains("SiteConfigWebMapper.toResponse(query.get())"));
    }

    @Test
    void legacySiteConfigServiceStaysRetired() throws IOException {
        Path legacy = ROOT.resolve("src/main/java/com/xinghuo/service/SiteConfigService.java");
        assertFalse(Files.exists(legacy));
        try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("com.xinghuo.service.SiteConfigService"),
                        file + " still references retired SiteConfigService");
            }
        }
    }
}
