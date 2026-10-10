package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SeoGuideBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void seoUsesGuideQueryApplicationBoundary() throws IOException {
        String source = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/controller/SeoController.java"));

        assertTrue(source.contains("com.xinghuo.guide.application.GuideQueryApplicationService"));
        assertTrue(source.contains("guides.publicList(null, null, 200)"));
        assertFalse(source.contains("GuideContentService"));
    }
}
