package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xinghuo.model.ServiceItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class ServiceDetailTitleRenderTest {

    @Test
    void mw4PrefixedServiceNameDoesNotRepeatGameTokenInSeoTitleOrHeading() {
        ServiceItem service = ServiceItem.builder()
                .slug("mw4-tier-camo")
                .gameCode("CODMW4")
                .name("MW4 迷彩层级代练")
                .category("CAMO")
                .shortDesc("精通迷彩前置层级，按武器当前进度补差")
                .priceMode(ServiceItem.PriceMode.QUOTE)
                .priceMin(new BigDecimal("80.00"))
                .durationText("按进度确认")
                .build();

        Context context = new Context();
        context.setVariable("service", service);
        context.setVariable("gameName", "使命召唤 MW4");
        context.setVariable("serviceDescription", "使命召唤 MW4 精通迷彩前置层级，按武器当前进度补差。按进度报价，网页不收账号密码。");
        context.setVariable("canonicalUrl", "https://wydlgf.cn/service/mw4-tier-camo");
        context.setVariable("ogImage", "https://wydlgf.cn/og.png");
        context.setVariable("breadcrumbJsonLd", "{}");
        context.setVariable("faqJsonLd", "{}");
        context.setVariable("supportItems", Map.of());
        context.setVariable("quoteFactors", Map.of());
        context.setVariable("relatedServices", List.of());
        context.setVariable("serviceFaqs", List.of());
        context.setVariable("contactFallback", "AAC9878");
        context.setVariable("homeServices", List.of());

        String html = templateEngine().process("service-detail", context);

        assertTrue(html.contains("<title>使命召唤 MW4 迷彩层级代练 | 星火网游代练平台</title>"));
        assertTrue(html.contains("<h1>使命召唤 MW4 迷彩层级代练</h1>"));
        assertTrue(html.contains("\"name\":\"使命召唤 MW4 迷彩层级代练\""));
        assertFalse(html.contains("使命召唤 MW4 MW4 迷彩层级代练"));
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
