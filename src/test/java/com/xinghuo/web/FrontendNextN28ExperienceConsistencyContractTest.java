package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN28ExperienceConsistencyContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerInjectsAndFingerprintsN28Runtime() throws Exception {
        String preview = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        assertTrue(preview.contains("N28_RUNTIME"));
        assertTrue(preview.contains("/next/n28-experience-consistency.js"));
        assertTrue(preview.contains("html.contains(N28_RUNTIME)"));
        assertTrue(preview.contains("\"next/n28-experience-consistency.js\""));
    }

    @Test
    void consistencyLayerAddsKeyboardMobileAndReducedMotionSupport() throws Exception {
        String runtime = read("resources/static/next/n28-experience-consistency.js");

        assertTrue(runtime.contains("link.textContent = '跳到主要内容'"));
        assertTrue(runtime.contains(":focus-visible"));
        assertTrue(runtime.contains("min-height:44px"));
        assertTrue(runtime.contains("font-size:16px"));
        assertTrue(runtime.contains("prefers-reduced-motion:reduce"));
        assertTrue(runtime.contains("forced-colors:active"));
        assertTrue(runtime.contains("ensureMainTarget"));
        assertTrue(runtime.contains("main.tabIndex = -1"));
    }

    @Test
    void loadingAndDynamicContentReceiveConsistentAccessibleSemantics() throws Exception {
        String runtime = read("resources/static/next/n28-experience-consistency.js");

        assertTrue(runtime.contains("node.setAttribute('aria-live', 'polite')"));
        assertTrue(runtime.contains("node.setAttribute('aria-busy', busy ? 'true' : 'false')"));
        assertTrue(runtime.contains("#n23GuideStatus"));
        assertTrue(runtime.contains("#n24Message"));
        assertTrue(runtime.contains("#trackerStatus"));
        assertTrue(runtime.contains("#n26RecordMessage"));
        assertTrue(runtime.contains("img:not([alt])"));
        assertTrue(runtime.contains("image.setAttribute('decoding', 'async')"));
        assertTrue(runtime.contains("a[target=\"_blank\"]"));
        assertTrue(runtime.contains("rel.add('noopener')"));
        assertTrue(runtime.contains("rel.add('noreferrer')"));
    }

    @Test
    void buttonNormalizationDoesNotBreakImplicitFormSubmitSemantics() throws Exception {
        String runtime = read("resources/static/next/n28-experience-consistency.js");

        assertTrue(runtime.contains("if (!button.closest('form')) button.type = 'button'"));
        assertTrue(runtime.contains("button[disabled]"));
        assertTrue(runtime.contains("aria-disabled"));
        assertFalse(runtime.contains("button.type = 'submit'"));
    }

    @Test
    void N28IsPresentationOnlyAndDoesNotIntroduceBusinessStateOrNetworkWrites() throws Exception {
        String runtime = read("resources/static/next/n28-experience-consistency.js");

        assertFalse(runtime.contains("fetch("));
        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));
    }
}
