package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class UploadBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void uploadFeatureHasExplicitBoundaries() {
        Path base = ROOT.resolve("src/main/java/com/xinghuo/upload");
        assertTrue(Files.isDirectory(base.resolve("application")));
        assertTrue(Files.isDirectory(base.resolve("domain")));
        assertTrue(Files.isDirectory(base.resolve("infrastructure")));
        assertTrue(Files.isDirectory(base.resolve("web")));
    }

    @Test
    void applicationAndDomainStayPersistenceAndWebIndependent() throws Exception {
        for (String layer : new String[] {"application", "domain"}) {
            try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java/com/xinghuo/upload/" + layer))) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file);
                    assertFalse(source.contains("com.xinghuo.model"), file + " must not expose persistence models");
                    assertFalse(source.contains("com.xinghuo.repository"), file + " must not depend on repositories");
                    assertFalse(source.contains("org.springframework.web"), file + " must not depend on Spring web types");
                    assertFalse(source.contains("org.springframework.core.io"), file + " must not expose Spring resources");
                    assertFalse(source.contains("jakarta.servlet"), file + " must not depend on servlet APIs");
                    assertFalse(source.contains("MultipartFile"), file + " must not depend on multipart protocol types");
                }
            }
        }
    }

    @Test
    void infrastructureAdapterOwnsLegacyUploadAndEntityTranslation() throws Exception {
        String adapter = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/upload/infrastructure/UploadApplicationAdapter.java"));
        assertTrue(adapter.contains("implements UploadPort"));
        assertTrue(adapter.contains("UploadService"));
        assertTrue(adapter.contains("UploadAsset.Kind.QUOTE_SCREENSHOT"));
        assertTrue(adapter.contains("new UploadStored(stored.getId())"));
        assertTrue(adapter.contains("uploads.loadPublicSiteAsset(assetId)"));
        assertTrue(adapter.contains("uploads.load(assetId, ownerType, ownerId)"));
        assertTrue(adapter.contains("input.readAllBytes()"));
    }

    @Test
    void legacyUploadServiceKeepsMultipartCompatibilityAndAddsApplicationInput() throws Exception {
        String service = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/upload/infrastructure/UploadService.java"));
        assertTrue(service.contains("storeImage(MultipartFile file, UploadAsset.Kind kind)"));
        assertTrue(service.contains("storeImage(byte[] content, UploadAsset.Kind kind)"));
        assertTrue(service.contains("return this.storeImage(file.getBytes(), kind)"));
        assertTrue(service.contains("MAX_PIXELS = 40000000L"));
        assertTrue(service.contains("MAX_EDGE = 8192"));
        assertTrue(service.contains("仅支持 JPG/PNG/WEBP 图片"));
        assertTrue(service.contains("图片尺寸过大，请压缩后重试"));
    }
}
