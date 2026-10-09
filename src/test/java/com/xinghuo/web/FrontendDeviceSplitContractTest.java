package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xinghuo.config.FrontendDeviceViewInterceptor;
import com.xinghuo.config.MobileFrontendAssetFilter;
import jakarta.servlet.FilterChain;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

class FrontendDeviceSplitContractTest {
    private static final Path TEMPLATES = Path.of("src/main/resources/templates/mobile");

    @Test
    void iphoneUsesIndependentMobileTemplateTreeAndPublicHtmlVariesByUserAgent() throws Exception {
        var request = new MockHttpServletRequest("GET", "/");
        request.addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit Mobile/15E148");
        var response = new MockHttpServletResponse();
        var modelAndView = new ModelAndView("index");
        var interceptor = new FrontendDeviceViewInterceptor();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        interceptor.postHandle(request, response, new Object(), modelAndView);

        assertEquals("mobile/index", modelAndView.getViewName());
        assertEquals("mobile", request.getAttribute("frontendMode"));
        assertTrue(response.getHeaders("Vary").contains("User-Agent"));
    }

    @Test
    void androidPhoneUsesMobileButDesktopAndAdminDoNot() throws Exception {
        var interceptor = new FrontendDeviceViewInterceptor();

        var android = new MockHttpServletRequest("GET", "/services");
        android.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit Chrome Mobile Safari");
        var androidView = new ModelAndView("services");
        interceptor.postHandle(android, new MockHttpServletResponse(), new Object(), androidView);
        assertEquals("mobile/services", androidView.getViewName());

        var desktop = new MockHttpServletRequest("GET", "/services");
        desktop.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit Chrome Safari");
        var desktopView = new ModelAndView("services");
        interceptor.postHandle(desktop, new MockHttpServletResponse(), new Object(), desktopView);
        assertEquals("services", desktopView.getViewName());
        assertEquals("desktop", desktop.getAttribute("frontendMode"));

        var admin = new MockHttpServletRequest("GET", "/admin");
        admin.addHeader("User-Agent", "Mozilla/5.0 (iPhone) Mobile");
        var adminResponse = new MockHttpServletResponse();
        var adminView = new ModelAndView("admin");
        interceptor.preHandle(admin, adminResponse, new Object());
        interceptor.postHandle(admin, adminResponse, new Object(), adminView);
        assertEquals("admin", adminView.getViewName());
        assertEquals("desktop", admin.getAttribute("frontendMode"));
        assertFalse(adminResponse.getHeaders("Vary").contains("User-Agent"));
    }

    @Test
    void mobileResponseLoadsMobileAssetsButDesktopDoesNot() throws Exception {
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
        assertTrue(mobileHtml.contains("name=\"xh-frontend\" content=\"mobile\""));
        assertTrue(mobileHtml.contains("/assets/mobile-app.css?v=20261009m2"));
        assertTrue(mobileHtml.contains("/assets/mobile-compact.css?v=20261009mc1"));
        assertTrue(mobileHtml.contains("/assets/mobile-screenshot-fixes.css?v=20261009msf2"));
        assertTrue(mobileHtml.contains("/assets/mobile-flat-overflow.css?v=20261009mfo1"));
        assertTrue(mobileHtml.contains("/assets/mobile-runtime-fixes.css?v=20261009mrf1"));
        assertTrue(mobileHtml.contains("/assets/mobile-app.js?v=20261009m2"));

        var desktopRequest = new MockHttpServletRequest("GET", "/services");
        desktopRequest.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X) Chrome Safari");
        var desktopResponse = new MockHttpServletResponse();
        filter.doFilter(desktopRequest, desktopResponse, html);
        String desktopHtml = desktopResponse.getContentAsString();
        assertFalse(desktopHtml.contains("mobile-app.css"));
        assertFalse(desktopHtml.contains("mobile-compact.css"));
        assertFalse(desktopHtml.contains("mobile-screenshot-fixes.css"));
        assertFalse(desktopHtml.contains("mobile-flat-overflow.css"));
        assertFalse(desktopHtml.contains("mobile-runtime-fixes.css"));
        assertFalse(desktopHtml.contains("mobile-app.js"));
    }

    @Test
    void mobileTemplateSetAndAssetsExistIndependently() {
        List<String> required = List.of(
                "index.html", "services.html", "service-detail.html", "quote.html", "records.html",
                "record-detail.html", "contact.html", "trust.html", "rules.html", "privacy.html",
                "account-login.html", "account-register.html", "account.html", "account-settings.html",
                "guides.html", "guide.html", "game.html", "mw4-camo-tracker.html"
        );
        required.forEach(file -> assertTrue(Files.isRegularFile(TEMPLATES.resolve(file)), "missing mobile template: " + file));
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/static/assets/mobile-app.css")));
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/static/assets/mobile-compact.css")));
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/static/assets/mobile-screenshot-fixes.css")));
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/static/assets/mobile-flat-overflow.css")));
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/static/assets/mobile-runtime-fixes.css")));
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/static/assets/mobile-app.js")));
        assertFalse(TEMPLATES.resolve("index.html").equals(Path.of("src/main/resources/templates/index.html")));
    }
}
