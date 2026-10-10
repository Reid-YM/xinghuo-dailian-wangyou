package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class GuideBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws Exception {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void guideFeatureHasExplicitBoundaries() {
        Path base = ROOT.resolve("src/main/java/com/xinghuo/guide");
        assertTrue(Files.isDirectory(base.resolve("application")));
        assertTrue(Files.isDirectory(base.resolve("domain")));
        assertTrue(Files.isDirectory(base.resolve("infrastructure")));
        assertTrue(Files.isDirectory(base.resolve("web")));
    }

    @Test
    void queryApplicationAndDomainStayPersistenceAndProtocolIndependent() throws Exception {
        Path application = ROOT.resolve("src/main/java/com/xinghuo/guide/application");
        try (Stream<Path> files = Files.list(application)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("JdbcTemplate"), file.toString());
                assertFalse(source.contains("com.xinghuo.repository"), file.toString());
                assertFalse(source.contains("com.xinghuo.model"), file.toString());
                assertFalse(source.contains("com.xinghuo.dto"), file.toString());
                assertFalse(source.contains("jakarta.servlet"), file.toString());
                assertFalse(source.contains("jakarta.persistence"), file.toString());
            }
        }
        String domain = read("src/main/java/com/xinghuo/guide/domain/package-info.java");
        assertFalse(domain.contains("org.springframework"));
        assertFalse(domain.contains("jakarta.persistence"));
        assertFalse(domain.contains("jakarta.servlet"));
        assertFalse(domain.contains("com.xinghuo.repository"));
    }

    @Test
    void jdbcAdapterOwnsGuideReadPersistenceAndPublicVisibilityRules() throws Exception {
        String adapter = read("src/main/java/com/xinghuo/guide/infrastructure/GuideQueryJdbcAdapter.java");
        assertTrue(adapter.contains("implements GuideQueryPort"));
        assertTrue(adapter.contains("JdbcTemplate"));
        assertTrue(adapter.contains("g.status = 'PUBLISHED'"));
        assertTrue(adapter.contains("view_count = view_count + 1"));
        assertTrue(adapter.contains("publicOnly ? \" AND s.is_active = 1\""));
        assertFalse(adapter.contains("GuideContentService"));
        assertFalse(adapter.contains("jakarta.servlet"));
    }

    @Test
    void publicAndPageReadsUseGuideQueryApplicationBoundary() throws Exception {
        String publicController = read("src/main/java/com/xinghuo/guide/web/PublicGuideController.java");
        String pageController = read("src/main/java/com/xinghuo/guide/web/GuidePageController.java");
        for (String source : new String[] {publicController, pageController}) {
            assertTrue(source.contains("GuideQueryApplicationService"));
            assertFalse(source.contains("GuideContentService"));
            assertFalse(source.contains("JdbcTemplate"));
        }
    }

    @Test
    void adminReadsUseQueryBoundaryWhileWritesRemainExplicitlyLegacyForNextPhase() throws Exception {
        String admin = read("src/main/java/com/xinghuo/guide/web/AdminGuideController.java");
        assertTrue(admin.contains("GuideQueryApplicationService queries"));
        assertTrue(admin.contains("GuideContentService writes"));
        assertTrue(admin.contains("return queries.adminList()"));
        assertTrue(admin.contains("return queries.adminDetail(id)"));
        assertTrue(admin.contains("return writes.create"));
        assertTrue(admin.contains("return writes.update"));
    }

    @Test
    void flatGuideControllersStayRetired() {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/guide/PublicGuideController.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/guide/AdminGuideController.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/guide/GuidePageController.java")));
    }
}
