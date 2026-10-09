package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xinghuo.config.FrontendV6AssetFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class FrontendV6AssetInjectionTest {

    @Test
    void publicHtmlLoadsV6AssetsExactlyOnce() throws Exception {
        var filter = new FrontendV6AssetFilter();
        FilterChain html = (request, response) -> {
            var http = (jakarta.servlet.http.HttpServletResponse) response;
            http.setContentType("text/html;charset=UTF-8");
            http.getWriter().write("<!doctype html><html><head><title>x</title></head><body>ok</body></html>");
        };

        var request = new MockHttpServletRequest("GET", "/services");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, html);
        String body = response.getContentAsString();

        assertTrue(body.contains("<meta name=\"xh-ui\" content=\"v6\">"));
        assertTrue(body.contains("frontend-v6.css?v=20261009v6a"));
        assertTrue(body.contains("frontend-v6-geometry.css?v=20261009v6b"));
        assertTrue(body.contains("frontend-v6.js?v=20261009v6a"));
        assertTrue(body.indexOf("frontend-v6.css") == body.lastIndexOf("frontend-v6.css"));
        assertTrue(body.indexOf("frontend-v6-geometry.css") == body.lastIndexOf("frontend-v6-geometry.css"));
    }

    @Test
    void adminApiAndStaticAssetsNeverReceivePublicV6Layer() throws Exception {
        var filter = new FrontendV6AssetFilter();
        FilterChain html = (request, response) -> {
            var http = (jakarta.servlet.http.HttpServletResponse) response;
            http.setContentType("text/html;charset=UTF-8");
            http.getWriter().write("<!doctype html><html><head></head><body>admin</body></html>");
        };

        for (String uri : new String[]{"/admin", "/admin/login", "/api/site-config", "/assets/app.css", "/uploads/a.png"}) {
            var request = new MockHttpServletRequest("GET", uri);
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, html);
            assertFalse(response.getContentAsString().contains("frontend-v6.css"), uri);
            assertFalse(response.getContentAsString().contains("frontend-v6-geometry.css"), uri);
        }
    }
}
