package com.xinghuo.account.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册账号模块的拦截器。
 *
 * <p>多个 WebMvcConfigurer 是叠加生效的，因此新增本配置不会影响基线已有的配置。
 */
@Configuration
public class AccountWebConfig implements WebMvcConfigurer {

    private final AccountAuthInterceptor authInterceptor;

    public AccountWebConfig(AccountAuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 刻意不做 excludePathPatterns。
        //
        // 曾经的写法是把 /api/account/login、/register 等公开端点排除在外，
        // 结果是这些最需要保护的写接口完全绕过了拦截器里的 CSRF 自定义头校验 ——
        // 公开端点恰恰是跨站伪造与刷接口的首要目标。
        //
        // 现在所有账号路径都经过拦截器：是否需要登录由 @RequireAccount 注解决定，
        // 而 CSRF 头校验对全部写方法一律生效。
        //
        // 页面路径用 /account/** 通配而不是逐个列举：曾经因为逐个列举而漏掉了
        // 新增的 /account/submit，导致未登录用户能直接打开需要登录的页面。
        // 通配后新增页面只要标注 @RequireAccount 就会自动受保护。
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/account/**", "/account/**", "/api/console/**", "/console/**");
    }
}
