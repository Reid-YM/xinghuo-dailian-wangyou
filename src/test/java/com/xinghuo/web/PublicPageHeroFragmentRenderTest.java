package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class PublicPageHeroFragmentRenderTest {

    @Test
    void servicesAndTrustRenderSharedCompactQuoteHero() {
        TemplateEngine engine = templateEngine();

        Context servicesContext = new Context();
        servicesContext.setVariable("hallServices", List.of());
        servicesContext.setVariable("acceptingOrders", true);
        String services = engine.process("services", servicesContext);
        assertTrue(services.contains("class=\"page-hero page-hero-compact service-browser-hero\""));
        assertTrue(services.contains("● 多游戏服务大厅 · 按当前进度确认报价"));
        assertTrue(services.contains("COD MW4 / DMZ / 战区 任务服务"));
        assertTrue(services.contains("可靠手打 · 纯绿服务 · 进度可查 · 下单前确认需求"));
        assertTrue(services.contains("href=\"/quote\""));
        assertFalse(services.contains("compactQuoteHero"));

        String trust = engine.process("trust", new Context());
        assertTrue(trust.contains("class=\"page-hero page-hero-compact\""));
        assertTrue(trust.contains("服务口径、备案信息与常见问题"));
        assertTrue(trust.contains("href=\"/quote\""));
        assertTrue(trust.contains("href=\"/rules\""));
        assertFalse(trust.contains("compactQuoteHero"));
    }

    private TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
