package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class LegacyTemplateCleanlinessTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @Test
    void publicTemplatesDoNotExposeRetiredDirectCustomerServiceLinks() throws IOException {
        for (Path template : publicTemplateFiles()) {
            String content = Files.readString(template);
            assertFalse(content.contains("AAC9878"), () -> template + " still exposes the retired WeChat ID");
            assertFalse(content.contains("work.weixin.qq.com/kfid/"),
                    () -> template + " still bypasses the server-side customer-service router");
            assertFalse(content.contains("/customer-service/start?topic="),
                    () -> template + " still uses the legacy customer-service query route");
        }
    }

    @Test
    void publicTemplatesDoNotLinkToRetiredPages() throws IOException {
        for (Path template : publicTemplateFiles()) {
            String content = Files.readString(template);
            assertFalse(content.contains("href=\"/console"), () -> template + " still links to the retired console UI");
            assertFalse(content.contains("href=\"/order\""), () -> template + " still links to the legacy order lookup page");
            assertFalse(content.contains("href=\"/loadouts"), () -> template + " still links to the retired loadout page");
        }
    }

    @Test
    void sharedHeaderUsesAccountEntryInsteadOfForcingLoginPage() throws IOException {
        String content = Files.readString(TEMPLATES.resolve("fragments/site.html"));
        assertTrue(content.contains("href=\"/account\" data-account-entry"),
                "shared header must use /account so the server can respect the existing login session");
        assertFalse(content.contains("href=\"/account/login\" data-account-entry"),
                "shared header must not force already logged-in users back to the login page");
    }

    private static List<Path> publicTemplateFiles() throws IOException {
        try (var paths = Files.walk(TEMPLATES)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".html"))
                    .filter(path -> !path.getFileName().toString().startsWith("admin"))
                    .toList();
        }
    }
}
