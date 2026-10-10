package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FinalLegacyRetirementContractTest {
    private static final Path ROOT = Path.of(".");

    private static final List<String> RETIRED_ASSETS = List.of(
            "admin-guides.css",
            "admin-guides-refine.css",
            "site-shell.css",
            "site-surface-refine.css",
            "site-surface-refine.js");

    @Test
    void provenDeadLegacyAssetsArePhysicallyRemoved() {
        Path assets = ROOT.resolve("src/main/resources/static/assets");
        for (String retired : RETIRED_ASSETS) {
            assertFalse(Files.exists(assets.resolve(retired)), retired + " must stay retired");
        }

        assertTrue(Files.exists(assets.resolve("admin-guides-workspace.css")));
        assertTrue(Files.exists(assets.resolve("admin-light-theme.css")));
        assertTrue(Files.exists(assets.resolve("site-experience-v2.css")));
        assertTrue(Files.exists(assets.resolve("site-shell.js")));
    }

    @Test
    void runtimeSourcesDoNotReferenceRetiredAssets() throws IOException {
        Path main = ROOT.resolve("src/main");
        try (Stream<Path> files = Files.walk(main)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String source;
                try {
                    source = Files.readString(file);
                } catch (Exception ignored) {
                    continue;
                }
                for (String retired : RETIRED_ASSETS) {
                    assertFalse(source.contains(retired), file + " still references retired asset " + retired);
                }
            }
        }
    }

    @Test
    void obsoleteQuoteMigrationTypeIsRemovedAfterOwnedApplicationContractLanded() throws IOException {
        Path obsolete = ROOT.resolve("src/main/java/com/xinghuo/quote/domain/QuoteSubmissionMetadata.java");
        Path command = ROOT.resolve("src/main/java/com/xinghuo/quote/application/QuoteSubmissionCommand.java");
        assertFalse(Files.exists(obsolete));
        assertTrue(Files.exists(command));

        try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("QuoteSubmissionMetadata"),
                        file + " still references obsolete quote migration metadata");
            }
        }
    }

    @Test
    void catalogLegacyServiceIsRemovedAfterOwnedPortAndPersistenceAdapterLanded() throws IOException {
        Path legacyService = ROOT.resolve("src/main/java/com/xinghuo/service/ServiceCatalogService.java");
        Path legacyAdapter = ROOT.resolve("src/main/java/com/xinghuo/catalog/infrastructure/LegacyCatalogAdapter.java");
        Path legacyPort = ROOT.resolve("src/main/java/com/xinghuo/catalog/application/CatalogLegacyPort.java");
        Path port = ROOT.resolve("src/main/java/com/xinghuo/catalog/application/CatalogPort.java");
        Path adapter = ROOT.resolve("src/main/java/com/xinghuo/catalog/infrastructure/CatalogPersistenceAdapter.java");

        assertFalse(Files.exists(legacyService));
        assertFalse(Files.exists(legacyAdapter));
        assertFalse(Files.exists(legacyPort));
        assertTrue(Files.exists(port));
        assertTrue(Files.exists(adapter));

        String application = Files.readString(ROOT.resolve(
                "src/main/java/com/xinghuo/catalog/application/CatalogApplicationService.java"));
        assertTrue(application.contains("CatalogPort"));
        assertFalse(application.contains("CatalogLegacyPort"));
    }
}
