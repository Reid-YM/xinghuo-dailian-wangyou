package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LegacyFrontendRetirementContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void sharedFragmentOwnsCanonicalLegacyVnextStyleOrder() throws IOException {
        String site = read("src/main/resources/templates/fragments/site.html");
        String[] assets = {
                "site-experience-v2.css",
                "vnext-public-finish-v4.css",
                "vnext-final-qa-fix-v5.css",
                "vnext-mobile-focus-v7.css",
                "vnext-mobile-polish-v8.css"
        };

        int previous = -1;
        for (String asset : assets) {
            int current = site.indexOf(asset);
            assertTrue(current > previous, asset + " must exist in canonical order");
            previous = current;
        }
    }

    @Test
    void siteShellDoesNotInjectCanonicalStyles() throws IOException {
        String shell = read("src/main/resources/static/assets/site-shell.js");
        assertFalse(shell.contains("vnext-mobile-focus-v7.css"));
        assertFalse(shell.contains("vnext-mobile-polish-v8.css"));
        assertFalse(shell.contains("ensureMobileFocusStyles"));
        assertFalse(shell.contains("data-vnext-mobile-focus"));
        assertFalse(shell.contains("data-vnext-mobile-polish"));
    }

    @Test
    void retirementPlanDocumentsDeletionGate() throws IOException {
        String plan = read("docs/legacy-frontend-retirement.md");
        assertTrue(plan.contains("One owner per responsibility"));
        assertTrue(plan.contains("Chromium + WebKit"));
        assertTrue(plan.contains("production smoke"));
        assertTrue(plan.contains("rollback path"));
    }
}
