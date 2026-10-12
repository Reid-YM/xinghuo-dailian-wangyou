package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ServiceDetailTitleRenderTest {

    @Test
    void serviceDetailUsesDeduplicatedDisplayTitleAcrossSeoSurfaces() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/service-detail.html"));

        assertTrue(template.contains("gameToken=${gameName != null && gameName.contains(' ') ? gameName.substring(gameName.lastIndexOf(' ') + 1) : gameName}"),
                "service detail must derive the trailing game token such as MW4");
        assertTrue(template.contains("service.name.startsWith(gameName + ' ')"),
                "full game-name prefixes must be deduplicated");
        assertTrue(template.contains("service.name.startsWith(gameToken + ' ')"),
                "short game-token prefixes such as MW4 must be deduplicated");
        assertTrue(template.contains("serviceDisplayH1=${gameName + ' ' + serviceDisplayName}"),
                "the public heading must be rebuilt from the deduplicated service name");
        assertTrue(template.contains("serviceDisplayTitle=${serviceDisplayH1 + ' | 星火网游代练平台'}"),
                "the SEO title must use the deduplicated heading");

        assertTrue(template.contains("<title th:text=\"${serviceDisplayTitle}\""));
        assertTrue(template.contains("<meta property=\"og:title\" th:content=\"${serviceDisplayTitle}\""));
        assertTrue(template.contains("<meta name=\"twitter:title\" th:content=\"${serviceDisplayTitle}\""));
        assertTrue(template.contains("<h1 th:text=\"${serviceDisplayH1}\""));
        assertTrue(template.contains("\"name\":[[${serviceDisplayH1}]]"));

        assertFalse(template.contains("<title th:text=\"${serviceTitle}\""),
                "legacy duplicated controller title must not drive the page title");
        assertFalse(template.contains("<h1 th:text=\"${serviceH1}\""),
                "legacy duplicated controller heading must not drive the H1");
    }
}
