package com.xinghuo.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/** Server-side integration readiness. Never returns credential values or file paths. */
@Service
public class XingweiIntegrationConfigService {
    private final Environment environment;

    public XingweiIntegrationConfigService(Environment environment) {
        this.environment = environment;
    }

    public WeComStatus weCom() {
        List<Check> checks = List.of(
                check("WECOM_CORP_ID", "企业 ID", present("wecom.corp-id")),
                check("WECOM_APP_SECRET", "通讯录读取密钥", present("wecom.app-secret")));
        return new WeComStatus(checks, allReady(checks), present("WECOM_SECRET"));
    }

    public PaymentStatus payment() {
        String key = value("app.wechat-pay.api-v3-key");
        String notifyUrl = value("app.wechat-pay.notify-url");
        List<Check> checks = List.of(
                check("WECHAT_PAY_MERCHANT_ID", "商户号", present("app.wechat-pay.merchant-id")),
                check("WECHAT_PAY_APP_ID", "支付 AppID", present("app.wechat-pay.app-id")),
                check("WECHAT_PAY_MERCHANT_SERIAL", "商户证书序列号", present("app.wechat-pay.merchant-serial")),
                check("WECHAT_PAY_PRIVATE_KEY_PATH", "商户私钥文件可读", readableAbsoluteFile("app.wechat-pay.private-key-path")),
                check("WECHAT_PAY_PUBLIC_KEY_PATH", "微信支付公钥文件可读", readableAbsoluteFile("app.wechat-pay.public-key-path")),
                check("WECHAT_PAY_PUBLIC_KEY_ID", "微信支付公钥 ID", present("app.wechat-pay.public-key-id")),
                check("WECHAT_PAY_API_V3_KEY", "APIv3 密钥格式", key.getBytes(StandardCharsets.UTF_8).length == 32),
                check("WECHAT_PAY_NOTIFY_URL", "HTTPS 支付回调地址", notifyUrl.startsWith("https://")
                        && notifyUrl.endsWith("/api/wechat-pay/notify")));
        boolean enabled = Boolean.parseBoolean(value("app.wechat-pay.enabled"));
        String miniAppId = value("app.wechat-mini.app-id");
        String payAppId = value("app.wechat-pay.app-id");
        return new PaymentStatus(checks, allReady(checks), enabled,
                !miniAppId.isBlank() && miniAppId.equals(payAppId));
    }

    private boolean readableAbsoluteFile(String key) {
        String value = value(key);
        if (value.isBlank()) return false;
        try {
            Path path = Path.of(value);
            return path.isAbsolute() && Files.isRegularFile(path) && Files.isReadable(path);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean present(String key) {
        return !value(key).isBlank();
    }

    private String value(String key) {
        String value = environment.getProperty(key);
        return value == null ? "" : value.trim();
    }

    private static boolean allReady(List<Check> checks) {
        return checks.stream().allMatch(Check::ready);
    }

    private static Check check(String key, String label, boolean ready) {
        return new Check(key, label, ready);
    }

    public record Check(String key, String label, boolean ready) {}
    public record WeComStatus(List<Check> checks, boolean readConfigured, boolean writeCredentialPresent) {}
    public record PaymentStatus(List<Check> checks, boolean configReady, boolean enabled,
                                boolean miniAppIdMatches) {}
}
