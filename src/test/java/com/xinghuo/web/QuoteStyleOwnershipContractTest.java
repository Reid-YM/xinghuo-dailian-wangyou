package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class QuoteStyleOwnershipContractTest {
    private static final Path MAIN = Path.of("src/main/resources");

    @Test
    void quoteTemplateHasNoPageLevelStylePatch() throws Exception {
        String quote = Files.readString(MAIN.resolve("templates/quote.html"));
        assertFalse(quote.contains("<style>"), "quote page must not restore page-level inline CSS patches");
        assertFalse(quote.contains("body[data-page=\"quote\"] .consult-card"));
        assertFalse(quote.contains("body[data-page=\"quote\"] .quote-layout"));
    }

    @Test
    void experienceLayerOwnsQuoteLayoutAndStickySupportOffset() throws Exception {
        String css = Files.readString(MAIN.resolve("static/assets/site-experience-v2.css"));
        assertTrue(css.contains(".quote-layout{display:grid;grid-template-columns:minmax(0,1fr) minmax(280px,360px);gap:18px;align-items:start}"));
        assertTrue(css.contains(".consult-card{position:sticky;top:calc(var(--xh-header-height) + var(--xh-safe-top) + 18px)}"));
        assertTrue(css.contains("@media(max-width:900px)"));
        assertTrue(css.contains(".consult-card{position:relative;top:auto}"));
    }
}
