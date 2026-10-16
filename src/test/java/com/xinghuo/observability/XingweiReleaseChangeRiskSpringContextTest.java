package com.xinghuo.observability;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class XingweiReleaseChangeRiskSpringContextTest {
    @Test
    void serviceCanBeCreatedByRealSpringConstructorInjection() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getBeanFactory().registerSingleton("objectMapper", new ObjectMapper());
            context.register(XingweiReleaseChangeRiskService.class);
            context.refresh();

            assertNotNull(context.getBean(XingweiReleaseChangeRiskService.class));
        }
    }
}
