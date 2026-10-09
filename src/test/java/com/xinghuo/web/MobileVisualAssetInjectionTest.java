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
    void sharedMobilePagesLoadSharedBundleWithoutLegacyVisualRuntimeButDesktopDoesNot() throws Exception {
        var filter = new MobileFrontendAssetFilter();
        FilterChain html = (request, response) -> {
            var http = (jakarta.servlet.http.HttpServletResponse) response;
            http.setContentType("text/html;charset=UTF-8");
            http.getWriter().write("<!doctype html><html><head><title>x</title></head><body>ok</body></html>");
        };

        var mobileRequest = new MockHttpServletRequest("GET", "/services");
        mobileRequest.addHeader("User-Agent", "Mozilla/5.0 (iPhone) Mobile");
        var mobileResponse = new MockHttpServletResponse();
        filter.doFilter(mobileRequest, mobileResponse, html);
        String mobileHtml = mobileResponse.getContentAsString();
        assertTrue(mobileHtml.contains("mobile-app.css?v=20261009m2"));
        assertTrue(mobileHtml.contains("mobile-compact.css?v=20261009mc1"));
        assertTrue(mobileHtml.contains("mobile-runtime-fixes.css?v=20261009mrf1"));
        assertTrue(mobileHtml.contains("mobile-app.js?v=20261009m2"));
        assertFalse(mobileHtml.contains("mobile-visual-runtime.js"));
        assertFalse(mobileHtml.contains("mobile-home-v4.css"));
        assertFalse(mobileHtml.contains("mobile-home-shell-v1.css"));

        var desktopRequest = new MockHttpServletRequest("GET", "/services");
        desktopRequest.addHeader("User-Agent", "Mozilla/5.0 (Macintosh) Chrome Safari");
        var desktopResponse = new MockHttpServletResponse();
        filter.doFilter(desktopRequest, desktopResponse, html);
        String desktopHtml = desktopResponse.getContentAsString();
        assertFalse(desktopHtml.contains("mobile-app.css"));
        assertFalse(desktopHtml.contains("mobile-compact.css"));
        assertFalse(desktopHtml.contains("mobile-runtime-fixes.css"));
        assertFalse(desktopHtml.contains("mobile-app.js"));
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
