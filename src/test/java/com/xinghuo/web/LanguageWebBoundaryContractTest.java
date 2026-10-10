package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LanguageWebBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void languageSwitchEntryPointIsFeatureOwnedAndKeepsRedirectGuards() throws Exception {
        String source = Files.readString(ROOT.resolve("src/main/java/com/xinghuo/language/web/LanguageController.java"));

        assertTrue(source.contains("package com.xinghuo.language.web;"));
        assertTrue(source.contains("@GetMapping(value={\"/language\"})"));
        assertTrue(source.contains("new Cookie(COOKIE_NAME, normalized)"));
        assertTrue(source.contains("cookie.setSecure(true)"));
        assertTrue(source.contains("cookie.setAttribute(\"SameSite\", \"Lax\")"));
        assertTrue(source.contains("uri.isAbsolute() || value.startsWith(\"//\")"));
        assertTrue(source.contains("return value.startsWith(\"/\") ? value : \"/\";"));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/com/xinghuo/controller/LanguageController.java")));
    }
}
