package com.xinghuo.payment;

import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** The switch stays off until production credentials and callback URL are installed. */
@Configuration
@ConditionalOnProperty(prefix = "app.wechat-pay", name = "enabled", havingValue = "true")
@EnableScheduling
public class WeChatPayConfig {
    @Bean
    WeChatPayClient weChatPayClient(
            @Value("${app.wechat-pay.merchant-id}") String merchantId,
            @Value("${app.wechat-pay.app-id}") String appId,
            @Value("${app.wechat-pay.merchant-serial}") String merchantSerial,
            @Value("${app.wechat-pay.private-key-path}") String privateKeyPath,
            @Value("${app.wechat-pay.public-key-path}") String publicKeyPath,
            @Value("${app.wechat-pay.public-key-id}") String publicKeyId,
            @Value("${app.wechat-pay.api-v3-key}") String apiV3Key,
            @Value("${app.wechat-pay.notify-url}") String notifyUrl) {
        require(merchantId, "merchant-id");
        require(appId, "app-id");
        require(merchantSerial, "merchant-serial");
        require(publicKeyId, "public-key-id");
        require(apiV3Key, "api-v3-key");
        require(notifyUrl, "notify-url");
        if (apiV3Key.getBytes(StandardCharsets.UTF_8).length != 32) {
            throw new IllegalStateException("WECHAT_PAY_API_V3_KEY must be 32 bytes");
        }
        if (!notifyUrl.startsWith("https://") || !notifyUrl.endsWith("/api/wechat-pay/notify")) {
            throw new IllegalStateException("WECHAT_PAY_NOTIFY_URL must be the public HTTPS callback URL");
        }
        requireFile(privateKeyPath, "private-key-path");
        requireFile(publicKeyPath, "public-key-path");
        RSAPublicKeyConfig config = new RSAPublicKeyConfig.Builder()
                .merchantId(merchantId)
                .privateKeyFromPath(privateKeyPath)
                .merchantSerialNumber(merchantSerial)
                .publicKeyFromPath(publicKeyPath)
                .publicKeyId(publicKeyId)
                .apiV3Key(apiV3Key)
                .build();
        return new WeChatPayClient(merchantId, appId, notifyUrl,
                new NativePayService.Builder().config(config).build(),
                new NotificationParser(config));
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing app.wechat-pay." + name);
        }
    }

    private static void requireFile(String value, String name) {
        require(value, name);
        Path path = Path.of(value);
        if (!path.isAbsolute() || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalStateException("app.wechat-pay." + name + " must be an absolute readable file");
        }
    }

    public record WeChatPayClient(String merchantId, String appId, String notifyUrl,
                                  NativePayService nativePay, NotificationParser parser) {}
}
