package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

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
    void gamePageCategoryExpressionRendersWithoutTemplateError() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/game.html"));
        int tagStart = template.indexOf("<span class=\"tag\" th:text=\"");
        assertTrue(tagStart >= 0);
        int expressionStart = tagStart + "<span class=\"tag\" th:text=\"".length();
        int expressionEnd = template.indexOf('"', expressionStart);
        String categoryExpression = template.substring(expressionStart, expressionEnd);

        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        Context context = new Context();
        context.setVariable("categoryCode", "weapon");
        assertTrue(engine.process("<span th:text=\"" + categoryExpression + "\"></span>", context)
                .contains(">武器升级</span>"));
        context.setVariable("categoryCode", "other");
        assertTrue(engine.process("<span th:text=\"" + categoryExpression + "\"></span>", context)
                .contains(">其他任务</span>"));
    }
}
