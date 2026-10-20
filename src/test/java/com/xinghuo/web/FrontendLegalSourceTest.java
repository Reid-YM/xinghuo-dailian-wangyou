package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class FrontendLegalSourceTest {
    @Test void publishedDocumentsRenderOnlyTheirRealContentWithoutLegacyShell() throws Exception {
        var templates=new ClassLoaderTemplateResolver();
        templates.setPrefix("templates/");templates.setSuffix(".html");templates.setCharacterEncoding("UTF-8");
        var engine=new SpringTemplateEngine();engine.setTemplateResolver(templates);
        var views=new ThymeleafViewResolver();views.setTemplateEngine(engine);views.setCharacterEncoding("UTF-8");
        var mvc=MockMvcBuilders.standaloneSetup(new FrontendNextN28LegalController(new FrontendNextPreviewController()))
            .setViewResolvers(views).build();
        for(String document:List.of("rules","privacy","rules-en","privacy-en")) {
            var result=mvc.perform(get("/api/public/legal/"+document))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(header().string("X-Robots-Tag","noindex,nofollow")).andReturn();
            String html=result.getResponse().getContentAsString();
            assertTrue(html.contains("rules-card"));assertTrue(html.contains("<h1>"));
            assertTrue(html.contains(document.startsWith("rules") ? (document.endsWith("-en") ? "Refund" : "取消与退款") : (document.endsWith("-en") ? "Retention Periods" : "询价信息（未成交）")),document);
            assertFalse(html.contains("<script"));assertFalse(html.contains("<header"));assertFalse(html.contains("<footer"));
            assertFalse(html.contains("th:replace"));
        }
        mvc.perform(get("/api/public/legal/unknown")).andExpect(status().isNotFound());
    }
}
