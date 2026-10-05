package com.xinghuo.account.web;

import com.xinghuo.account.dto.AccountDtos;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.service.AccountEmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 邮箱验证与找回密码。
 *
 * <p>端点均为写方法，因此同样受拦截器的 X-Requested-With 校验保护 ——
 * 前端统一走带该头的 request() 封装。
 */
@RestController
@RequestMapping("/api/account")
public class AccountEmailApiController {

    private final AccountEmailService emailService;

    public AccountEmailApiController(AccountEmailService emailService) {
        this.emailService = emailService;
    }

    /** 发送注册邮箱验证码（公开）。 */
    @PostMapping("/register/verify/request")
    public AccountDtos.OkResponse requestRegisterVerify(@Valid @RequestBody AccountDtos.EmailRequest body,
                                                        HttpServletRequest request) {
        emailService.requestRegisterCode(body.email(), request);
        return AccountDtos.OkResponse.of("验证码已发送，请在 30 分钟内完成注册");
    }

    /** 绑定邮箱并发送验证邮件（需登录）。 */
    @PostMapping("/email/verify/request")
    @RequireAccount
    public AccountDtos.OkResponse requestVerify(@Valid @RequestBody AccountDtos.EmailRequest body,
                                                HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        emailService.requestVerify(account, body.email(), request);
        return AccountDtos.OkResponse.of("验证码已发送，请在 30 分钟内完成验证");
    }

    @PostMapping("/email/verify/confirm")
    @RequireAccount
    public AccountDtos.OkResponse confirmVerify(@Valid @RequestBody AccountDtos.EmailVerifyCodeRequest body,
                                                HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        emailService.verifyEmailCode(account, body.code(), request);
        return AccountDtos.OkResponse.of("邮箱验证成功");
    }

    /** 消费验证令牌（兼容旧邮件链接）。 */
    @GetMapping("/email/verify")
    public AccountDtos.OkResponse verify(@RequestParam String token, HttpServletRequest request) {
        emailService.verifyEmail(token, request);
        return AccountDtos.OkResponse.of("邮箱验证成功");
    }

    /**
     * 申请重置密码（公开）。
     *
     * <p>无论邮箱是否已注册都返回同一句话，避免变成账号枚举接口。
     */
    @PostMapping("/password/reset/request")
    public AccountDtos.OkResponse requestReset(@Valid @RequestBody AccountDtos.EmailRequest body,
                                               HttpServletRequest request) {
        emailService.requestPasswordReset(body.email(), request);
        return AccountDtos.OkResponse.of("如果该邮箱已绑定账号，我们已发送重置验证码");
    }

    /** 消费重置令牌并设置新密码（公开）。 */
    @PostMapping("/password/reset")
    public AccountDtos.OkResponse reset(@Valid @RequestBody AccountDtos.ResetPasswordRequest body,
                                        HttpServletRequest request) {
        if (body.code() != null && !body.code().isBlank()) {
            emailService.resetPasswordByCode(body.email(), body.code(), body.newPassword(), request);
        } else {
            emailService.resetPassword(body.token(), body.newPassword(), request);
        }
        return AccountDtos.OkResponse.of("密码已重置，请用新密码登录");
    }
}
