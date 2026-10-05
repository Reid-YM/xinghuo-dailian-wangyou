package com.xinghuo.account.service;

import org.springframework.http.HttpStatus;

/**
 * 账号模块的业务异常。
 *
 * <p>刻意不复用基线 jar 里的 {@code com.xinghuo.exception.*}：整个账号模块
 * 保持自包含，只依赖框架类，这样它既能覆盖进基线 jar 编译，也能脱离
 * 基线单独编译校验。
 */
public class AccountException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public AccountException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static AccountException badRequest(String code, String message) {
        return new AccountException(code, message, HttpStatus.BAD_REQUEST);
    }

    public static AccountException unauthorized(String code, String message) {
        return new AccountException(code, message, HttpStatus.UNAUTHORIZED);
    }

    public static AccountException forbidden(String code, String message) {
        return new AccountException(code, message, HttpStatus.FORBIDDEN);
    }

    public static AccountException notFound(String code, String message) {
        return new AccountException(code, message, HttpStatus.NOT_FOUND);
    }

    public static AccountException conflict(String code, String message) {
        return new AccountException(code, message, HttpStatus.CONFLICT);
    }

    public static AccountException rateLimited(String message) {
        return new AccountException("RATE_LIMITED", message, HttpStatus.TOO_MANY_REQUESTS);
    }

    public static AccountException serverError(String code, String message) {
        return new AccountException(code, message, HttpStatus.BAD_GATEWAY);
    }

    /** 未登录。前端据此跳转登录页。 */
    public static AccountException unauthenticated() {
        return unauthorized("UNAUTHENTICATED", "请先登录");
    }
}
