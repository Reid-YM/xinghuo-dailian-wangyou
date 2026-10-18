package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class FrontendNextAssetVersionTest {
    @Test void htmlUsesActualContentFingerprintsForAllRuntimesAndDoesNotCacheTheManifest() throws Exception {
        var response=new FrontendNextPreviewController().frontendNext();
        assertEquals("no-store",response.getHeaders().getCacheControl());
        for(String name:new String[]{"shell.js","n2.js","n3.js","n4.js","n4-mini.js","n5.js","n5-actions.js","n6-account-security.js","n7-auth.js","n8-integration.js"}) {
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of("src/main/resources/static/next/"+name))));
            assertTrue(response.getBody().contains("src=\"/next/"+name+"?v="+hash+"\""),name);
        }
        for(String name:new String[]{"site-image-controls.js","site-image-controls.css","guide-community.js","guide-community.css"}) {
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of("src/main/resources/static/assets/"+name))));
            assertTrue(response.getBody().contains("/assets/"+name+"?v="+hash));
        }
    }
    @Test void independentlyLoadedStagesHaveTheirOwnContentDigests() throws Exception {
        String html = new FrontendNextPreviewController().frontendNext().getBody();
        var attribute = java.util.regex.Pattern.compile("data-stage-versions=\"([^\"]+)\"").matcher(html);
        assertTrue(attribute.find());
        var manifest = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                org.springframework.web.util.HtmlUtils.htmlUnescape(attribute.group(1)));
        for (String name : new String[]{"n23-guides-center.js", "n24-mastery.js",
                "n25-submission-workbench.js", "n26-records-center.js", "n26-record-history-bridge.js",
                "n27-support-trust.js", "n28-legal-single-source.js", "game-detail.js"}) {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(Path.of("src/main/resources/static/next/" + name))));
            assertEquals(digest, manifest.path("/next/" + name).asText(), name);
        }
    }

}
