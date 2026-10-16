package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.xinghuo.service.SiteConfigService;
import com.xinghuo.dto.ApiDtos;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class FrontendReplacementMetadataTest {
    @Test void forwardedDocumentKeepsCanonicalUrlsAndEscapesConfiguredSeoBeforeJavascriptRuns() throws Exception {
        var site=mock(ApiDtos.SiteConfigResponse.class);when(site.brandName()).thenReturn("品牌<测试>");when(site.seoTitle()).thenReturn("首页<标题>");when(site.seoDescription()).thenReturn("描述\"<script>");
        var config=mock(SiteConfigService.class);when(config.publicConfig()).thenReturn(site);
        var controller=new FrontendNextPreviewController();controller.configuration(config);
        var request=new MockHttpServletRequest("GET","/next/services");request.setAttribute(RequestDispatcher.FORWARD_REQUEST_URI,"/services");
        String html=controller.frontendNext(request).getBody();
        assertTrue(org.springframework.web.util.HtmlUtils.htmlUnescape(html).contains("<title>服务大厅 · 品牌<测试></title>"));assertTrue(html.contains("href=\"https://wydlgf.cn/services\""));
        assertTrue(html.contains("&quot;&lt;script&gt;"));assertFalse(html.contains("<script>"));
        request.setAttribute(RequestDispatcher.FORWARD_REQUEST_URI,"/account");
        assertTrue(controller.frontendNext(request).getBody().contains("name=\"robots\" content=\"noindex,nofollow\""));
    }
}
