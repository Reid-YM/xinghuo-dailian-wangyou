package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** The human confirmation POST must work with Spring Security's CSRF guard still enabled. */
class GuestAccessConfirmationContractTest {

    @Test
    void confirmationFormSubmitsTheCsrfToken() throws Exception {
        String html;
        try (var stream = new ClassPathResource("templates/guest-access.html").getInputStream()) {
            html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(html.contains("<form method=\"post\">"));
        assertTrue(html.contains("th:name=\"\u0024{_csrf.parameterName}\""));
        assertTrue(html.contains("th:value=\"\u0024{_csrf.token}\""));
    }
}
