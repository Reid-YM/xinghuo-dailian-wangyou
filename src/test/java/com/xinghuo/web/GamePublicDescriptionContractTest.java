package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GamePublicDescriptionContractTest {

    @Test
    void gamePageUsesSeoOrUserFacingDescriptionInsteadOfInternalTaxonomyDescription() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/game.html"));

        assertTrue(template.contains("publicGameDescription=${game.seoDescription ?: game.name + ' 可接任务、活动、等级、迷彩、排位等服务；查看参考价格与预计时间，提交需求后由客服按当前进度确认报价。'}"));
        assertTrue(template.contains("<meta name=\"description\" th:content=\"${publicGameDescription}\""));
        assertTrue(template.contains("<meta property=\"og:description\" th:content=\"${publicGameDescription}\""));
        assertTrue(template.contains("<p th:text=\"${publicGameDescription}\""));

        assertFalse(template.contains("game.seoDescription ?: game.description"),
                "Open Graph must not fall back to internal taxonomy description");
        assertFalse(template.contains("game.description ?: game.genres"),
                "hero copy must not expose internal taxonomy description");
        assertFalse(template.contains("游戏与版本维度，用于一级业务归属"),
                "internal taxonomy wording must never be hardcoded into the public page");
    }
}
