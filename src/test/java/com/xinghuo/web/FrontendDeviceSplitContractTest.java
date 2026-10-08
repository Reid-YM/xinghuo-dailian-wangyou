package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xinghuo.config.FrontendDeviceViewInterceptor;
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
    void iphoneUsesIndependentMobileTemplateTree() throws Exception {
        var request = new MockHttpServletRequest("GET", "/");
        request.addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit Mobile/15E148");
        var modelAndView = new ModelAndView("index");

        new FrontendDeviceViewInterceptor().postHandle(request, new MockHttpServletResponse(), new Object(), modelAndView);

        assertEquals("mobile/index", modelAndView.getViewName());
        assertEquals("mobile", request.getAttribute("frontendMode"));
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
        var adminView = new ModelAndView("admin");
        interceptor.postHandle(admin, new MockHttpServletResponse(), new Object(), adminView);
        assertEquals("admin", adminView.getViewName());
        assertEquals("desktop", admin.getAttribute("frontendMode"));
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
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/static/assets/mobile-app.js")));
        assertFalse(TEMPLATES.resolve("index.html").equals(Path.of("src/main/resources/templates/index.html")));
    }
}
