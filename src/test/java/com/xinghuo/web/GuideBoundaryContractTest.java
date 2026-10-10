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
    void guideFeatureHasExplicitBoundariesAndNoFlatBusinessClasses() throws Exception {
        Path base = ROOT.resolve("src/main/java/com/xinghuo/guide");
        assertTrue(Files.isDirectory(base.resolve("application")));
        assertTrue(Files.isDirectory(base.resolve("domain")));
        assertTrue(Files.isDirectory(base.resolve("infrastructure")));
        assertTrue(Files.isDirectory(base.resolve("web")));
        try (Stream<Path> files = Files.list(base)) {
            assertFalse(files.anyMatch(path -> path.toString().endsWith(".java")),
                    "guide root must contain boundary directories only");
        }
    }

    @Test
    void applicationAndDomainStayPersistenceAndProtocolIndependent() throws Exception {
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
                assertFalse(source.contains("com.xinghuo.audit"), file.toString());
            }
        }
        String domain = read("src/main/java/com/xinghuo/guide/domain/package-info.java");
        assertFalse(domain.contains("org.springframework"));
        assertFalse(domain.contains("jakarta.persistence"));
        assertFalse(domain.contains("jakarta.servlet"));
        assertFalse(domain.contains("com.xinghuo.repository"));
    }

    @Test
    void queryJdbcAdapterOwnsReadPersistenceAndPublicVisibilityRules() throws Exception {
        String adapter = read("src/main/java/com/xinghuo/guide/infrastructure/GuideQueryJdbcAdapter.java");
        assertTrue(adapter.contains("implements GuideQueryPort"));
        assertTrue(adapter.contains("JdbcTemplate"));
        assertTrue(adapter.contains("g.status = 'PUBLISHED'"));
        assertTrue(adapter.contains("view_count = view_count + 1"));
        assertTrue(adapter.contains("publicOnly ? \" AND s.is_active = 1\""));
        assertFalse(adapter.contains("jakarta.servlet"));
    }

    @Test
    void writeJdbcAdapterOwnsMutationsValidationImportAndAudit() throws Exception {
        String adapter = read("src/main/java/com/xinghuo/guide/infrastructure/GuideWriteJdbcAdapter.java");
        assertTrue(adapter.contains("implements GuideWritePort"));
        assertTrue(adapter.contains("JdbcTemplate"));
        assertTrue(adapter.contains("AuditService"));
        assertTrue(adapter.contains("GUIDE_CREATE"));
        assertTrue(adapter.contains("GUIDE_UPDATE"));
        assertTrue(adapter.contains("GUIDE_STATUS"));
        assertTrue(adapter.contains("GUIDE_ARCHIVE"));
        assertTrue(adapter.contains("INSERT INTO game_guides"));
        assertTrue(adapter.contains("DELETE FROM guide_sections"));
        assertTrue(adapter.contains("发布攻略前至少需要一个内容段落"));
        assertTrue(adapter.contains("推荐商品不属于当前游戏"));
        assertFalse(adapter.contains("GuideAdminRequests"));
    }

    @Test
    void publicAndPageReadsUseGuideQueryApplicationBoundary() throws Exception {
        String publicController = read("src/main/java/com/xinghuo/guide/web/PublicGuideController.java");
        String pageController = read("src/main/java/com/xinghuo/guide/web/GuidePageController.java");
        for (String source : new String[] {publicController, pageController}) {
            assertTrue(source.contains("GuideQueryApplicationService"));
            assertFalse(source.contains("JdbcTemplate"));
        }
    }

    @Test
    void adminReadsAndWritesUseApplicationAndWebBoundaries() throws Exception {
        String admin = read("src/main/java/com/xinghuo/guide/web/AdminGuideController.java");
        assertTrue(admin.contains("GuideQueryApplicationService queries"));
        assertTrue(admin.contains("GuideWriteApplicationService writes"));
        assertTrue(admin.contains("GuideAdminRequests.GuideUpsert"));
        assertTrue(admin.contains("GuideWriteWebMapper.toCommand"));
        assertTrue(admin.contains("return queries.adminList()"));
        assertTrue(admin.contains("return queries.adminDetail(id)"));
        assertFalse(admin.contains("JdbcTemplate"));
        assertFalse(admin.contains("GuideContentService"));
        assertFalse(admin.contains("GuideModels"));
        assertFalse(admin.contains("jakarta.servlet"));
    }

    @Test
    void bootstrapUsesApplicationBoundariesOnly() throws Exception {
        String bootstrap = read("src/main/java/com/xinghuo/guide/infrastructure/GuideBootstrapConfig.java");
        assertTrue(bootstrap.contains("GuideQueryApplicationService queries"));
        assertTrue(bootstrap.contains("GuideWriteApplicationService writes"));
        assertTrue(bootstrap.contains("queries.adminList().isEmpty()"));
        assertTrue(bootstrap.contains("writes.importGames"));
        assertFalse(bootstrap.contains("GuideContentService"));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/guide/GuideBootstrapConfig.java")));
    }

    @Test
    void legacyGuideTypesStayRetiredAcrossProductionSources() throws Exception {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/guide/GuideContentService.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/guide/GuideModels.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/guide/PublicGuideController.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/guide/AdminGuideController.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/guide/GuidePageController.java")));
        try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("GuideContentService"),
                        file + " still references retired GuideContentService");
                assertFalse(source.contains("com.xinghuo.guide.GuideModels"),
                        file + " still references retired GuideModels");
            }
        }
    }
}
