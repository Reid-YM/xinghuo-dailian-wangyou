package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SlotBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void slotFeatureHasExplicitBoundaries() {
        Path base = ROOT.resolve("src/main/java/com/xinghuo/slot");
        assertTrue(Files.isDirectory(base.resolve("application")));
        assertTrue(Files.isDirectory(base.resolve("domain")));
        assertTrue(Files.isDirectory(base.resolve("infrastructure")));
        assertTrue(Files.isDirectory(base.resolve("web")));
    }

    @Test
    void applicationAndDomainStayFrameworkAndPersistenceIndependent() throws IOException {
        Path application = ROOT.resolve("src/main/java/com/xinghuo/slot/application");
        try (Stream<Path> files = Files.list(application)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("com.xinghuo.dto"), file + " must not depend on protocol DTOs");
                assertFalse(source.contains("com.xinghuo.model"), file + " must not depend on persistence entities");
                assertFalse(source.contains("com.xinghuo.repository"), file + " must not depend on repositories");
                assertFalse(source.contains("jakarta.servlet"), file + " must not depend on servlet APIs");
                assertFalse(source.contains("jakarta.persistence"), file + " must not depend on JPA");
            }
        }

        String domain = read("src/main/java/com/xinghuo/slot/domain/package-info.java");
        assertFalse(domain.contains("org.springframework"));
        assertFalse(domain.contains("jakarta.persistence"));
        assertFalse(domain.contains("jakarta.servlet"));
        assertFalse(domain.contains("com.xinghuo.repository"));
        assertFalse(domain.contains("com.xinghuo.service"));
    }

    @Test
    void adminConsumesSlotApplicationAndWebBoundary() throws IOException {
        String controller = read("src/main/java/com/xinghuo/controller/AdminApiController.java");
        assertTrue(controller.contains("com.xinghuo.slot.application.SlotApplicationService"));
        assertTrue(controller.contains("com.xinghuo.slot.web.SlotWebMapper"));
        assertTrue(controller.contains("SlotWebMapper.toSlotResponses"));
        assertTrue(controller.contains("SlotWebMapper.toCommand"));
        assertFalse(controller.contains("import com.xinghuo.service.SlotService;"));
    }

    @Test
    void webLayerOwnsEstablishedSlotProtocolDtos() throws IOException {
        String mapper = read("src/main/java/com/xinghuo/slot/web/SlotWebMapper.java");
        assertTrue(mapper.contains("ApiDtos.SlotUpdate"));
        assertTrue(mapper.contains("ApiDtos.SlotResponse"));
        assertTrue(mapper.contains("ApiDtos.ServiceResponse"));
        assertTrue(mapper.contains("UpdateSlotCommand"));
        assertTrue(mapper.contains("SlotView"));
        assertTrue(mapper.contains("SlotPublicServiceView"));
    }

    @Test
    void persistenceAdapterOwnsSelectionPersistenceValidationAndAudit() throws IOException {
        String adapter = read("src/main/java/com/xinghuo/slot/infrastructure/SlotPersistenceAdapter.java");
        assertTrue(adapter.contains("implements SlotPort"));
        assertTrue(adapter.contains("com.xinghuo.repository.HotSlotRepository"));
        assertTrue(adapter.contains("com.xinghuo.repository.ServiceItemRepository"));
        assertTrue(adapter.contains("com.xinghuo.taxonomy.application.TaxonomyApplicationService"));
        assertTrue(adapter.contains("com.xinghuo.service.AuditService"));
        assertTrue(adapter.contains("SLOT_UPDATE"));
        assertFalse(adapter.contains("com.xinghuo.dto"));
        assertFalse(adapter.contains("com.xinghuo.service.SlotService"));
    }

    @Test
    void applicationContractsAreExplicitAndLegacySlotServiceStaysRetired() throws IOException {
        assertTrue(read("src/main/java/com/xinghuo/slot/application/SlotView.java")
                .contains("record SlotView"));
        assertTrue(read("src/main/java/com/xinghuo/slot/application/SlotPublicServiceView.java")
                .contains("record SlotPublicServiceView"));
        assertTrue(read("src/main/java/com/xinghuo/slot/application/UpdateSlotCommand.java")
                .contains("record UpdateSlotCommand"));
        assertTrue(read("src/main/java/com/xinghuo/slot/application/SlotApplicationService.java")
                .contains("SlotPort"));

        Path legacy = ROOT.resolve("src/main/java/com/xinghuo/service/SlotService.java");
        assertFalse(Files.exists(legacy));
        try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("com.xinghuo.service.SlotService"),
                        file + " still references retired SlotService");
            }
        }
    }
}
