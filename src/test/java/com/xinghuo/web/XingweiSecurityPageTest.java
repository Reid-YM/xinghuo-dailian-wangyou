package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class XingweiSecurityPageTest {
    @Test
    void rendersCurrentAuthorizationAndDistinctAccountPathsWithCsrfLogout() {
        var resolver = new ClassLoaderTemplateResolver(); resolver.setPrefix("templates/"); resolver.setSuffix(".html");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        var context = new Context(); context.setVariables(Map.of("username", "<operator>", "accessMode", "SHADOW_RBAC", "dataScope", "SELF", "csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")));
        String html = engine.process("admin-xingwei-security", context);
        assertTrue(html.contains("&lt;operator&gt;")); assertFalse(html.contains("<operator>"));
        assertTrue(html.contains("SHADOW_RBAC")); assertTrue(html.contains("SELF"));
        assertTrue(html.contains("员工后台账号")); assertTrue(html.contains("网站会员管理"));
        assertTrue(html.contains("name=\"_csrf\"")); assertTrue(html.contains("action=\"/logout\""));
        assertFalse(html.contains("type=\"password\"")); assertFalse(html.contains("/reset-password"));
    }
}
