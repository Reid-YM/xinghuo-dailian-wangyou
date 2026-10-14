package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xinghuo.model.GameCatalog;
import com.xinghuo.model.ServiceItem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class GameCategoryLabelContractTest {

    @Test
    void gamePageRendersUserFacingServiceCategoryLabels() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/game.html"));

        assertTrue(template.contains("categoryCode=${#strings.toLowerCase(s.category)}"));
        assertTrue(template.contains("categoryCode == 'weapon' ? '武器升级'"));
        assertTrue(template.contains("categoryCode == 'camo' ? '迷彩任务'"));
        assertTrue(template.contains("categoryCode == 'rank' ? '排位 / 段位'"));
        assertTrue(template.contains("categoryCode == 'activity' ? '活动任务'"));
        assertTrue(template.contains("categoryCode == 'dmz' ? 'DMZ 任务'"));
        assertTrue(template.contains("categoryCode == 'other' ? '其他任务'"));
        assertFalse(template.contains("<span class=\"tag\" th:text=\"${s.category}\""),
                "raw internal category codes such as WEAPON/CAMO must not be server-rendered publicly");
    }

    @Test
    void gamePageRendersServiceCardWithoutTemplateError() {
        GameCatalog game = GameCatalog.builder()
                .code("CODMW4").name("使命召唤 MW4").seoSlug("cod-mw4").build();
        ServiceItem service = ServiceItem.builder()
                .slug("weapon-leveling").name("武器升级服务").category("WEAPON")
                .shortDesc("按进度确认").priceMode(ServiceItem.PriceMode.QUOTE)
                .durationText("按进度确认").status(ServiceItem.Status.AVAILABLE).build();

        Context context = new Context();
        context.setVariable("game", game);
        context.setVariable("gameServices", List.of(service));
        context.setVariable("canonicalUrl", "https://wydlgf.cn/game/cod-mw4");
        context.setVariable("gameOgImage", "https://wydlgf.cn/og.png");
        context.setVariable("breadcrumbJsonLd", "{}");
        context.setVariable("acceptingOrders", true);

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        String html = engine.process("game", context);
        assertTrue(html.contains("href=\"/service/weapon-leveling\""));
        assertTrue(html.contains("<span class=\"tag\">武器升级</span>"));
        assertTrue(html.contains("<span class=\"availability\">可接单</span>"));
    }
}
