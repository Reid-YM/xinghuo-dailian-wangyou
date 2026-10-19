package com.xinghuo.account.web;

import com.xinghuo.account.model.Account;
import com.xinghuo.account.service.AccountException;
import com.xinghuo.account.service.AccountSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 账号鉴权拦截器。
 *
 * <p>三件事：
 * <ol>
 *   <li>解析当前登录账号并挂到 request attribute，供控制器读取；</li>
 *   <li>对标注 {@link RequireAccount} 的处理器做登录校验：API 返回 401 JSON，
 *       页面跳转登录页（带 next 参数回到原地址）；</li>
 *   <li>对写操作校验自定义头 X-Requested-With，作为 CSRF 的纵深防御 ——
 *       SecurityConfig 对 /api/account/** 关闭了 CSRF，虽然 SameSite=Lax
 *       已经能挡住跨站表单提交，但多一道廉价防线是值得的。</li>
 * </ol>
 */
@Component
public class AccountAuthInterceptor implements HandlerInterceptor {

    /** 当前账号在 request 上的属性名。 */
    public static final String ATTR_ACCOUNT = "xhAccount";

    /** 自定义头，跨站请求无法在不触发 CORS 预检的情况下携带它。 */
    private static final String FETCH_HEADER = "X-Requested-With";
    private static final String FETCH_VALUE = "fetch";

    private final AccountSessionService sessions;

    public AccountAuthInterceptor(AccountSessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String path = request.getRequestURI();
        Optional<Account> account = sessions.current(request);

        if (account.isPresent()) {
            request.setAttribute(ATTR_ACCOUNT, account.get());
        }

        // 写操作要求自定义头。放在登录校验之前：未登录的跨站写请求也应被直接拒绝。
        if (isWriteMethod(request.getMethod()) && !FETCH_VALUE.equals(request.getHeader(FETCH_HEADER))) {
            throw AccountException.forbidden("CSRF_HEADER_MISSING",
                    "请求缺少 " + FETCH_HEADER + " 头");
        }

        if (account.isPresent() || !requiresAccount(handler)) {
            return true;
        }

        // 未登录
        if (path.startsWith("/api/")) {
            throw AccountException.unauthenticated();
        }
        String next = request.getRequestURI();
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            next = next + "?" + query;
        }
        response.sendRedirect("/account/login?" + ("1".equals(request.getParameter("legacy")) ? "legacy=1&" : "") + "next=" + java.net.URLEncoder
                .encode(next, java.nio.charset.StandardCharsets.UTF_8));
        return false;
    }

    private boolean isWriteMethod(String method) {
        return "POST".equals(method) || "PUT".equals(method)
                || "PATCH".equals(method) || "DELETE".equals(method);
    }

    private boolean requiresAccount(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }
        return handlerMethod.hasMethodAnnotation(RequireAccount.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireAccount.class);
    }

    /** 控制器读取当前账号。 */
    public static Optional<Account> currentAccount(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_ACCOUNT);
        return value instanceof Account account ? Optional.of(account) : Optional.empty();
    }

    /** 控制器读取当前账号，未登录直接抛 401。 */
    public static Account requireAccount(HttpServletRequest request) {
        return currentAccount(request).orElseThrow(AccountException::unauthenticated);
    }
}
