package com.xinghuo.web;

import com.xinghuo.web.DeviceModeResolver.DeviceMode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class DeviceViewInterceptor implements HandlerInterceptor {
    private static final Set<String> SPLIT_VIEWS = Set.of(
            "index", "services", "service-detail", "quote", "records", "record-detail"
    );

    private final DeviceModeResolver resolver;

    public DeviceViewInterceptor(DeviceModeResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {
        DeviceMode mode = resolver.resolve(request);
        response.addHeader("Vary", "User-Agent");
        response.addHeader("Vary", "Sec-CH-UA-Mobile");

        if (modelAndView == null) {
            return;
        }
        modelAndView.addObject("deviceMode", mode.key());
        modelAndView.addObject("mobileView", mode == DeviceMode.MOBILE);
        modelAndView.addObject("desktopView", mode == DeviceMode.DESKTOP);

        String viewName = modelAndView.getViewName();
        if (viewName == null
                || viewName.startsWith("redirect:")
                || viewName.startsWith("forward:")
                || viewName.contains("/")
                || !SPLIT_VIEWS.contains(viewName)) {
            return;
        }
        modelAndView.setViewName(mode.key() + "/" + viewName);
    }
}
