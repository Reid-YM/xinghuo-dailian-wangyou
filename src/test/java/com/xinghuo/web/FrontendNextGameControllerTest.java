package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.GameCatalog;
import com.xinghuo.service.TaxonomyService;
import org.springframework.web.util.HtmlUtils;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FrontendNextGameControllerTest {
    static GameCatalog game() {
        return GameCatalog.builder().code("CODMW4").name("测试游戏</script><script>unsafe</script>")
                .seoSlug("test-game").seoTitle("游戏标题\"<测试>").seoDescription("游戏描述\"<测试>")
                .coverImage("/test-cover.png").active(true).build();
    }
    @Test void gameMetadataIsPresentBeforeJavascriptAndEscapedIncludingStructuredData() throws Exception {
        var taxonomy = mock(TaxonomyService.class);
        when(taxonomy.publicGameBySeoSlug("test-game")).thenReturn(game());
        var mvc = MockMvcBuilders.standaloneSetup(new FrontendNextGameController(new FrontendNextPreviewController(), taxonomy)).build();
        var response = mvc.perform(get("/next/game/test-game")).andExpect(status().isOk()).andReturn().getResponse();
        String html = response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(html.contains("<title>" + HtmlUtils.htmlEscape("游戏标题\"<测试>") + "</title>"));
        assertTrue(html.contains("name=\"description\" content=\"" + HtmlUtils.htmlEscape("游戏描述\"<测试>") + "\""));
        assertTrue(html.contains("rel=\"canonical\" href=\"https://wydlgf.cn/game/test-game\""));
        assertTrue(html.contains("property=\"og:image\" content=\"https://wydlgf.cn/test-cover.png\""));
        assertTrue(html.contains("name=\"robots\" content=\"noindex,follow\""));
        var match = Pattern.compile("<script type=\"application/ld\\+json\">(.*?)</script>").matcher(html);
        assertTrue(match.find());
        String json = match.group(1);
        var breadcrumb = new ObjectMapper().readTree(json).get("itemListElement");
        assertEquals(3, breadcrumb.size());
        assertEquals(game().getName(), breadcrumb.get(2).get("name").asText());
        assertFalse(response.getContentAsString().contains("<script>unsafe</script>"));
        assertEquals("no-store", response.getHeader("Cache-Control"));
        verify(taxonomy).publicGameBySeoSlug("test-game");
    }
    @Test void originalPublicVisibilityCheckRejectsMissingAndInactiveGames() throws Exception {
        var taxonomy = mock(TaxonomyService.class);
        when(taxonomy.publicGameBySeoSlug("missing")).thenThrow(new NotFoundException());
        var mvc = MockMvcBuilders.standaloneSetup(new FrontendNextGameController(new FrontendNextPreviewController(), taxonomy)).build();
        mvc.perform(get("/next/game/missing")).andExpect(status().isNotFound());
    }
}
