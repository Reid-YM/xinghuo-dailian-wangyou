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
    }
}
