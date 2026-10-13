package com.xinghuo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class LanguageControllerSeoTest {

    private final LanguageController controller = new LanguageController();

    @Test
    void englishSwitchUsesStableEnglishLegalUrls() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertEquals("redirect:/rules-en",
                controller.switchLanguage("en", "/rules", response));
        assertEquals("redirect:/privacy-en",
                controller.switchLanguage("en", "/privacy", response));
    }

    @Test
    void chineseSwitchUsesStableChineseLegalUrls() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertEquals("redirect:/rules",
                controller.switchLanguage("zh", "/rules-en", response));
        assertEquals("redirect:/privacy",
                controller.switchLanguage("zh", "/privacy-en", response));
    }

    @Test
    void unrelatedSafeReturnPathIsPreserved() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertEquals("redirect:/services",
                controller.switchLanguage("en", "/services", response));
    }

    @Test
    void languageSwitchRedirectIsNotIndexable() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.switchLanguage("en", "/services", response);

        assertEquals("noindex, nofollow", response.getHeader("X-Robots-Tag"));
    }
}
