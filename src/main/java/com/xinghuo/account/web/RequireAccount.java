package com.xinghuo.account.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要登录的接口。
 *
 * <p>为什么不用 Spring Security 的 {@code @PreAuthorize("isAuthenticated()")}：
 * SecurityConfig 把 /api/account/** 设为 permitAll，而它的 SecurityContext 来自
 * HttpSession；本模块的登录态在自建会话表里，两者对接必须改 SecurityConfig
 * （基线覆盖类，风险高）。用自有的拦截器 + 注解，完全不动 SecurityConfig。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAccount {
}
