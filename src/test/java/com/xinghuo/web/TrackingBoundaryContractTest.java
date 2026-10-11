package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class TrackingBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void trackingFeatureHasExplicitBoundaries() {
        Path base = ROOT.resolve("src/main/java/com/xinghuo/tracking");
        assertTrue(Files.isDirectory(base.resolve("application")));
        assertTrue(Files.isDirectory(base.resolve("domain")));
        assertTrue(Files.isDirectory(base.resolve("infrastructure")));
        assertTrue(Files.isDirectory(base.resolve("web")));
    }

    @Test
    void applicationAndDomainStayPersistenceAndProtocolIndependent() throws IOException {
        Path application = ROOT.resolve("src/main/java/com/xinghuo/tracking/application");
        try (Stream<Path> files = Files.list(application)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("com.xinghuo.dto"));
                assertFalse(source.contains("com.xinghuo.model"));
                assertFalse(source.contains("com.xinghuo.repository"));
                assertFalse(source.contains("JdbcTemplate"));
                assertFalse(source.contains("jakarta.servlet"));
                assertFalse(source.contains("jakarta.persistence"));
            }
        }

        String domain = read("src/main/java/com/xinghuo/tracking/domain/package-info.java");
        assertFalse(domain.contains("org.springframework"));
        assertFalse(domain.contains("jakarta.persistence"));
        assertFalse(domain.contains("jakarta.servlet"));
        assertFalse(domain.contains("com.xinghuo.repository"));
        assertFalse(domain.contains("com.xinghuo.service"));
    }

    @Test
    void publicApiConsumesTrackingApplicationAndWebBoundary() throws IOException {
        String controller = read("src/main/java/com/xinghuo/tracking/web/PublicTrackingController.java");
        assertTrue(controller.contains("package com.xinghuo.tracking.web;"));
        assertTrue(controller.contains("com.xinghuo.tracking.application.TrackingApplicationService"));
        assertTrue(controller.contains("@RequestMapping(value={\"/api/track\"})"));
        assertTrue(controller.contains("@RequestBody TrackRequest body"));
        assertTrue(controller.contains("TrackingWebMapper.toCommand"));
        assertTrue(controller.contains("HashUtil.sha256(RequestUtil.clientIp(req))"));
        assertTrue(controller.contains("ResponseEntity.noContent().build()"));
        assertFalse(controller.contains("com.xinghuo.dto.ApiDtos"));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/PublicApiController.java")));
    }

    @Test
    void adminTrackingUsesApplicationBoundaryAndFeatureWebController() throws IOException {
        String controller = read("src/main/java/com/xinghuo/tracking/web/AdminTrackingController.java");
        assertTrue(controller.contains("TrackingAdminApplicationService"));
        assertTrue(controller.contains("/api/admin/tracking"));
        assertTrue(controller.contains("/summary"));
        assertFalse(controller.contains("JdbcTemplate"));
        assertFalse(controller.contains("tracking_events"));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/AdminTrackingController.java")));

        String application = read("src/main/java/com/xinghuo/tracking/application/TrackingAdminApplicationService.java");
        assertTrue(application.contains("Math.max(1, Math.min(90, days))"));
        assertTrue(application.contains("TrackingSummaryPort"));
    }

    @Test
    void infrastructureOwnsTrackingWriteAndAdminSummaryPersistence() throws IOException {
        String writeMapper = read("src/main/java/com/xinghuo/tracking/web/TrackingWebMapper.java");
        assertTrue(writeMapper.contains("TrackRequest"));
        assertFalse(writeMapper.contains("ApiDtos.TrackRequest"));
        assertTrue(writeMapper.contains("TrackingCommand"));

        String write = read("src/main/java/com/xinghuo/tracking/infrastructure/TrackingPersistenceAdapter.java");
        assertTrue(write.contains("implements TrackingPort"));
        assertTrue(write.contains("private final TrackingEventRepository repo;"));
        assertTrue(write.contains("TrackingEvent.builder()"));
        assertFalse(write.contains("com.xinghuo.repository.TrackingEventRepository"));
        assertFalse(write.contains("com.xinghuo.model.TrackingEvent"));
        assertTrue(write.contains("hero_quote_click"));
        assertTrue(write.contains("rule_open"));
        assertFalse(write.contains("com.xinghuo.dto"));
        assertFalse(write.contains("com.xinghuo.service.TrackingService"));

        String summary = read("src/main/java/com/xinghuo/tracking/infrastructure/TrackingSummaryJdbcAdapter.java");
        assertTrue(summary.contains("implements TrackingSummaryPort"));
        assertTrue(summary.contains("JdbcTemplate"));
        assertTrue(summary.contains("tracking_events"));
        assertTrue(summary.contains("quote_started"));
        assertTrue(summary.contains("order_search_success"));
        assertTrue(summary.contains("Math.round(count * 1000.0 / first) / 10.0"));
        assertTrue(summary.contains("GROUP BY d ORDER BY d"));
    }

    @Test
    void trackingWebContractPreservesValidationShape() throws IOException {
        String request = read("src/main/java/com/xinghuo/tracking/web/TrackRequest.java");
        assertTrue(request.contains("String eventName"));
        assertTrue(request.contains("String path"));
        assertTrue(request.contains("String refValue"));
        assertTrue(request.contains("@NotBlank @Size(max=64) @NotBlank @Size(max=64)"));
        assertTrue(request.contains("@Size(max=255) @Size(max=255)"));
    }

    @Test
    void legacyTrackingServiceAndGlobalAdminControllerStayRetired() throws IOException {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/service/TrackingService.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/AdminTrackingController.java")));
        assertTrue(read("src/main/java/com/xinghuo/tracking/application/TrackingCommand.java")
                .contains("record TrackingCommand"));
        assertTrue(read("src/main/java/com/xinghuo/tracking/application/TrackingApplicationService.java")
                .contains("TrackingPort"));
        assertTrue(read("src/main/java/com/xinghuo/tracking/application/TrackingSummaryView.java")
                .contains("record TrackingSummaryView"));

        try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("com.xinghuo.service.TrackingService"),
                        file + " still references retired TrackingService");
            }
        }
    }
}
