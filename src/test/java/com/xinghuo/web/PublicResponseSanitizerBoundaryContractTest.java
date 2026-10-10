package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PublicResponseSanitizerBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void sanitizerIsPathScopedAndIndependentFromTheGlobalPublicController() throws Exception {
        String source = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/publicapi/web/PublicResponseSanitizerAdvice.java"));

        assertTrue(source.contains("package com.xinghuo.publicapi.web;"));
        assertTrue(source.contains("@RestControllerAdvice\n"));
        assertFalse(source.contains("assignableTypes"));
        assertTrue(source.contains("\"/api/site-config\""));
        assertTrue(source.contains("\"/api/services\""));
        assertTrue(source.contains("\"/api/catalog\""));
        assertTrue(source.contains("\"/api/quote\""));
        assertTrue(source.contains("SANITIZED_PATHS.contains(request.getURI().getPath())"));
        assertTrue(source.contains("\"id\""));
        assertTrue(source.contains("\"internalNote\""));
        assertTrue(source.contains("\"version\""));
        assertTrue(source.contains("\"sourceQuoteId\""));
        assertTrue(source.contains("\"customerIpHash\""));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/PublicResponseSanitizerAdvice.java")));
    }
}
