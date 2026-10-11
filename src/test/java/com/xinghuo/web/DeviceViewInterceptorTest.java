package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

class DeviceViewInterceptorTest {

    @Test
    void phoneCoreViewRoutesToMobileTemplate() {
        DeviceViewInterceptor interceptor = new DeviceViewInterceptor(new DeviceModeResolver());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_6 like Mac OS X) Mobile/15E148 Safari/604.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelAndView mav = new ModelAndView("index");

        interceptor.postHandle(request, response, new Object(), mav);

        assertEquals("mobile/index", mav.getViewName());
        assertEquals("mobile", mav.getModel().get("deviceMode"));
        assertEquals(Boolean.TRUE, mav.getModel().get("mobileView"));
    }

    @Test
    void desktopCoreViewRoutesToDesktopTemplate() {
        DeviceViewInterceptor interceptor = new DeviceViewInterceptor(new DeviceModeResolver());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/140 Safari/537.36");
        ModelAndView mav = new ModelAndView("services");

        interceptor.postHandle(request, new MockHttpServletResponse(), new Object(), mav);

        assertEquals("desktop/services", mav.getViewName());
        assertEquals("desktop", mav.getModel().get("deviceMode"));
    }

    @Test
    void sharedLowFrequencyViewKeepsOriginalTemplate() {
        DeviceViewInterceptor interceptor = new DeviceViewInterceptor(new DeviceModeResolver());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 (iPhone) Mobile/15E148");
        ModelAndView mav = new ModelAndView("contact");

        interceptor.postHandle(request, new MockHttpServletResponse(), new Object(), mav);

        assertEquals("contact", mav.getViewName());
        assertEquals("mobile", mav.getModel().get("deviceMode"));
    }
}
