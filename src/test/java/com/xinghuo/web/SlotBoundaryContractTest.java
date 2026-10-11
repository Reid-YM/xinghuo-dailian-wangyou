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
        for (String layer : new String[] {"application", "domain"}) {
            Path directory = ROOT.resolve("src/main/java/com/xinghuo/slot/" + layer);
            try (Stream<Path> files = Files.walk(directory)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file);
                    assertFalse(source.contains("com.xinghuo.dto"), file + " must not depend on protocol DTOs");
                    assertFalse(source.contains("com.xinghuo.model"), file + " must not depend on persistence entities");
                    assertFalse(source.contains("com.xinghuo.repository"), file + " must not depend on retired repositories");
                    assertFalse(source.contains("jakarta.servlet"), file + " must not depend on servlet APIs");
                    assertFalse(source.contains("jakarta.persistence"), file + " must not depend on JPA");
                    assertFalse(source.contains("org.springframework.data."), file + " must not depend on Spring Data");
                }
            }
        }
    }

    @Test
    void adminControllerConsumesSlotApplicationAndWebBoundary() throws IOException {
        String controller = read("src/main/java/com/xinghuo/slot/web/AdminSlotController.java");
        assertTrue(controller.contains("package com.xinghuo.slot.web;"));
        assertTrue(controller.contains("SlotApplicationService"));
        assertTrue(controller.contains("SlotWebMapper.toSlotResponses"));
        assertTrue(controller.contains("SlotWebMapper.toCommand"));
        assertTrue(controller.contains("@RequestMapping(value={\"/api/admin/slots\"})"));
        assertFalse(controller.contains("HotSlotRepository"));
        assertFalse(controller.contains("ServiceItemRepository"));
        assertFalse(controller.contains("AuditService"));
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
        assertTrue(adapter.contains("private final HotSlotRepository repo;"));
        assertTrue(adapter.contains("com.xinghuo.catalog.infrastructure.ServiceItemRepository"));
        assertTrue(adapter.contains("com.xinghuo.taxonomy.application.TaxonomyApplicationService"));
        assertTrue(adapter.contains("com.xinghuo.audit.infrastructure.AuditService"));
        assertTrue(adapter.contains("SLOT_UPDATE"));
        assertFalse(adapter.contains("com.xinghuo.dto"));
        assertFalse(adapter.contains("com.xinghuo.repository."));
        assertFalse(adapter.contains("com.xinghuo.service."));
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
    }
}
