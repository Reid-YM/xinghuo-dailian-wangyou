package com.xinghuo.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Switch views only after the original handler and its authentication checks have succeeded. */
@Configuration
public class FrontendMigrationConfig implements WebMvcConfigurer {
    private final boolean enabled;
    public FrontendMigrationConfig(@Value("${app.frontend-next.default-enabled:false}") boolean enabled) {this.enabled=enabled;}
    @Override public void addInterceptors(InterceptorRegistry registry) {registry.addInterceptor(new Replacement(enabled));}
    public static class Replacement implements HandlerInterceptor {
        private static final Map<String,String> ROUTES=Map.ofEntries(
            Map.entry("/","/next"),Map.entry("/services","/next/services"),Map.entry("/quote","/next/quote"),
            Map.entry("/account","/next/account"),Map.entry("/account/login","/next/account"),
            Map.entry("/account/register","/next/account"),Map.entry("/account/reset","/next/account"),
            Map.entry("/account/settings","/next/account/settings"),Map.entry("/account/requests","/next/account/requests"),
            Map.entry("/account/orders","/next/account/orders"),Map.entry("/account/progress","/next/account/progress"));
        private final boolean enabled;
        public Replacement(boolean enabled){this.enabled=enabled;}
        @Override public void postHandle(HttpServletRequest request,HttpServletResponse response,Object handler,ModelAndView view) {
            if(!enabled || view==null || response.isCommitted() || !("GET".equals(request.getMethod())||"HEAD".equals(request.getMethod())) || "1".equals(request.getParameter("legacy")))return;
            String name=view.getViewName();
            if(name==null || name.startsWith("redirect:") || name.startsWith("forward:"))return;
            String path=request.getRequestURI().substring(request.getContextPath().length());
            String target=ROUTES.get(path);if(target==null)return;
            response.setHeader("Cache-Control","no-store");response.setHeader("X-Xinghuo-Frontend","next");
            view.setViewName("forward:"+target);
        }
    }
}
