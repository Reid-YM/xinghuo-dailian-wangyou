package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class LegacyTemplateCleanlinessTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @Test
    void publicTemplatesDoNotExposeRetiredDirectCustomerServiceLinks() throws IOException {
        for (Path template : templateFiles()) {
            String content = Files.readString(template);
            assertFalse(content.contains("AAC9878"), () -> template + " still exposes the retired WeChat ID");
            assertFalse(content.contains("work.weixin.qq.com/kfid/"),
                    () -> template + " still bypasses the server-side customer-service router");
        }
    }

    @Test
    void templatesDoNotLinkToRetiredPages() throws IOException {
        for (Path template : templateFiles()) {
            String content = Files.readString(template);
            assertFalse(content.contains("href=\"/console"), () -> template + " still links to the retired console UI");
            assertFalse(content.contains("href=\"/order\""), () -> template + " still links to the legacy order lookup page");
            assertFalse(content.contains("href=\"/loadouts"), () -> template + " still links to the retired loadout page");
        }
    }

    private static List<Path> templateFiles() throws IOException {
        try (var paths = Files.walk(TEMPLATES)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".html"))
                    .toList();
        }
    }
}
