package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Protect the public no-script fallback and the Chinese presentation copy. */
class FrontendNextPublicFallbackTest {

    @Test
    void publicRoutesHaveUsefulAndDistinctNoScriptContent() throws Exception {
        var controller = new FrontendNextPreviewController();
        String home = html(controller, "/");
        String services = html(controller, "/services");
        String guides = html(controller, "/guides");
        String quote = html(controller, "/quote");

        assertTrue(home.contains("<h1>星火游戏服务平台</h1>"));
        assertTrue(services.contains("<h1>游戏服务大厅</h1>"));
        assertTrue(guides.contains("<h1>游戏攻略中心</h1>"));
        assertTrue(quote.contains("<h1>提交服务询价</h1>"));
        for (String page : new String[]{home, services, guides, quote}) {
            assertTrue(page.contains("<noscript><section"));
            assertTrue(page.contains("<a href=\"/services\">服务大厅</a>"));
            assertTrue(page.contains("<a href=\"/contact\">联系方式</a>"));
            assertFalse(page.contains("请启用 JavaScript 以使用服务大厅、询价和个人中心。"));
        }
    }

    @Test
    void staticChineseFrontendHeadingsDoNotRegressToEnglish() throws Exception {
        String source = Files.readString(
                Path.of("src/main/resources/static/next/index.html"), StandardCharsets.UTF_8);
        for (String label : new String[]{"Service Center", "Get a Quote", "Service Summary"}) {
            assertFalse(source.contains("<div class=\"kicker\">" + label + "</div>"), label);
        }
        for (String label : new String[]{"服务项目", "提交需求", "需求摘要"}) {
            assertTrue(source.contains("<div class=\"kicker\">" + label + "</div>"), label);
        }
    }

    private static String html(FrontendNextPreviewController controller, String route)
            throws Exception {
        var request = new MockHttpServletRequest("GET", route);
        return controller.frontendNext(request).getBody();
    }
}
