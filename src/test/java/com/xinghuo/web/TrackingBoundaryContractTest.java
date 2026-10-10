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
        String controller = read("src/main/java/com/xinghuo/controller/PublicApiController.java");
        assertTrue(controller.contains("com.xinghuo.tracking.application.TrackingApplicationService"));
        assertTrue(controller.contains("com.xinghuo.tracking.web.TrackingWebMapper"));
        assertTrue(controller.contains("TrackingWebMapper.toCommand"));
        assertFalse(controller.contains("import com.xinghuo.service.TrackingService;"));
    }

    @Test
    void webMapperOwnsTrackRequestAndInfrastructureOwnsAllowListPersistence() throws IOException {
        String mapper = read("src/main/java/com/xinghuo/tracking/web/TrackingWebMapper.java");
        assertTrue(mapper.contains("ApiDtos.TrackRequest"));
        assertTrue(mapper.contains("TrackingCommand"));

        String adapter = read("src/main/java/com/xinghuo/tracking/infrastructure/TrackingPersistenceAdapter.java");
        assertTrue(adapter.contains("implements TrackingPort"));
        assertTrue(adapter.contains("com.xinghuo.repository.TrackingEventRepository"));
        assertTrue(adapter.contains("com.xinghuo.model.TrackingEvent"));
        assertTrue(adapter.contains("hero_quote_click"));
        assertTrue(adapter.contains("rule_open"));
        assertFalse(adapter.contains("com.xinghuo.dto"));
        assertFalse(adapter.contains("com.xinghuo.service.TrackingService"));
    }

    @Test
    void legacyTrackingServiceStaysRetired() throws IOException {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/service/TrackingService.java")));
        assertTrue(read("src/main/java/com/xinghuo/tracking/application/TrackingCommand.java")
                .contains("record TrackingCommand"));
        assertTrue(read("src/main/java/com/xinghuo/tracking/application/TrackingApplicationService.java")
                .contains("TrackingPort"));

        try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("com.xinghuo.service.TrackingService"),
                        file + " still references retired TrackingService");
            }
        }
    }
}
