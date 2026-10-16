package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

class XingweiSiteTemplateTest {
    private String render(String file,Boolean mode)throws Exception{
        var engine=new SpringTemplateEngine();engine.setTemplateResolver(new StringTemplateResolver());
        var context=new Context(Locale.SIMPLIFIED_CHINESE);context.setVariable("xingweiMode",mode);context.setVariable("username","test");
        var token=new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","test-token");context.setVariable("csrf",token);context.setVariable("_csrf",token);
        return engine.process(Files.readString(Path.of("src/main/resources/templates/"+file)),context);
    }
    @Test
    void frontendControlTemplateRendersNewModeAndRetainsLegacyDefault()throws Exception{
        String next=render("admin-frontend-controls.html",true),legacy=render("admin-frontend-controls.html",null);
        assertTrue(next.contains("data-xingwei-mode=\"true\""));assertTrue(next.contains("href=\"/admin/xingwei/site\""));
        assertTrue(next.contains("href=\"/admin/xingwei\""));assertTrue(legacy.contains("data-xingwei-mode=\"false\""));
        assertFalse(legacy.contains("href=\"/admin/xingwei/site\""));assertTrue(legacy.contains("href=\"/admin\""));
        assertTrue(next.contains("content=\"test-token\""));
    }
    @Test
    void siteTemplateRendersCsrfFieldsWithoutBrowserOrDatabase()throws Exception{
        String html=render("admin-xingwei-site.html",true);assertTrue(html.contains("content=\"test-token\""));assertTrue(html.contains("id=\"siteSave\""));
    }
}
