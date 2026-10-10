package com.xinghuo.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogPagePresentationTest {
    @Test
    void faqJsonLdEscapesHtmlSensitiveCharacters() {
        CatalogPagePresentation presentation = new CatalogPagePresentation(null, new ObjectMapper());

        String json = presentation.faqJsonLd(List.of(
                new CatalogPagePresentation.FaqItem("</script>", "A&B")));

        assertThat(json)
                .doesNotContain("</script>")
                .contains("\\u003c/script\\u003e")
                .contains("A\\u0026B");
    }
}
