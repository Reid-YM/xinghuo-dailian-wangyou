package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xinghuo.config.AdminVisualConfigAssetFilter;
import com.xinghuo.config.MobileFrontendAssetFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MobileVisualAssetInjectionTest {

    @Test
    void mobilePublicResponseLoadsDedicatedRuntimeAndCompactCssButDesktopDoesNot() throws Exception {
        var filter = new MobileFrontendAssetFilter();
        FilterChain html = (request, response) -> {
            var http = (jakarta.servlet.http.HttpServletResponse) response;
            http.setContentType("text/html;charset=UTF-8");
            http.getWriter().write("<!doctype html><html><head><title>x</title></head><body>ok</body></html>");
        };

        var mobileRequest = new MockHttpServletRequest("GET", "/");
        mobileRequest.addHeader("User-Agent", "Mozilla/5.0 (iPhone) Mobile");
        var mobileResponse = new MockHttpServletResponse();
        filter.doFilter(mobileRequest, mobileResponse, html);
        String mobileHtml = mobileResponse.getContentAsString();
        assertTrue(mobileHtml.contains("mobile-visual-runtime.js?v=20261010mv2"));
        assertTrue(mobileHtml.contains("mobile-compact.css?v=20261009mc1"));
        assertTrue(mobileHtml.contains("mobile-home-hero-r4.css?v=20261010r4ui1"));
        assertTrue(mobileHtml.contains("mobile-home-hero-r5.css?v=20261010r5ui1"));

        var desktopRequest = new MockHttpServletRequest("GET", "/");
        desktopRequest.addHeader("User-Agent", "Mozilla/5.0 (Macintosh) Chrome Safari");
        var desktopResponse = new MockHttpServletResponse();
        filter.doFilter(desktopRequest, desktopResponse, html);
        String desktopHtml = desktopResponse.getContentAsString();
        assertFalse(desktopHtml.contains("mobile-visual-runtime.js"));
        assertFalse(desktopHtml.contains("mobile-compact.css"));
        assertFalse(desktopHtml.contains("mobile-home-hero-r4.css"));
        assertFalse(desktopHtml.contains("mobile-home-hero-r5.css"));
    }

    @Test
    void adminPageLoadsDedicatedControlsButLoginPageDoesNot() throws Exception {
        var filter = new AdminVisualConfigAssetFilter();
        FilterChain html = (request, response) -> {
            var http = (jakarta.servlet.http.HttpServletResponse) response;
            http.setContentType("text/html;charset=UTF-8");
            http.getWriter().write("<!doctype html><html><head><title>admin</title></head><body>ok</body></html>");
        };

        var adminRequest = new MockHttpServletRequest("GET", "/admin");
        var adminResponse = new MockHttpServletResponse();
        filter.doFilter(adminRequest, adminResponse, html);
        assertTrue(adminResponse.getContentAsString().contains("admin-mobile-visual-config.js?v=20261009mv1"));

        var loginRequest = new MockHttpServletRequest("GET", "/admin/login");
        var loginResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest, loginResponse, html);
        assertFalse(loginResponse.getContentAsString().contains("admin-mobile-visual-config.js"));
    }
}
