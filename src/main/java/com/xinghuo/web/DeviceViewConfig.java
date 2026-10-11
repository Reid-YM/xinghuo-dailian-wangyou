package com.xinghuo.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DeviceViewConfig implements WebMvcConfigurer {
    private final DeviceViewInterceptor interceptor;

    public DeviceViewConfig(DeviceViewInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                // Device view switching belongs to the public storefront only. Keeping
                // the interceptor off admin/account/console/API routes prevents device
                // presentation state from leaking into authenticated control surfaces.
                .addPathPatterns(
                        "/",
                        "/services",
                        "/service/**",
                        "/quote",
                        "/records",
                        "/record/**");
    }
}
