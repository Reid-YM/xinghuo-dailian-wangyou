package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

class XingweiIntegrationConfigServiceTest {
    @TempDir Path temp;

    @Test
    void missingConfigurationNeverAppearsReady() {
        XingweiIntegrationConfigService service = new XingweiIntegrationConfigService(new MockEnvironment());
        assertFalse(service.weCom().readConfigured());
        assertFalse(service.weCom().writeCredentialPresent());
        assertFalse(service.payment().configReady());
        assertFalse(service.payment().enabled());
        assertFalse(service.payment().miniAppIdMatches());
    }

    @Test
    void checksNativeConfigurationWithoutReturningCredentials() throws Exception {
        Path privateKey = Files.writeString(temp.resolve("merchant.pem"), "TEST_ONLY");
        Path publicKey = Files.writeString(temp.resolve("wechat.pem"), "TEST_ONLY");
        MockEnvironment environment = new MockEnvironment()
                .withProperty("wecom.corp-id", "test-corp")
                .withProperty("wecom.app-secret", "test-read-secret")
                .withProperty("WECOM_SECRET", "test-write-secret")
                .withProperty("app.wechat-pay.merchant-id", "test-merchant")
                .withProperty("app.wechat-pay.app-id", "test-app")
                .withProperty("app.wechat-mini.app-id", "test-app")
                .withProperty("app.wechat-pay.merchant-serial", "test-serial")
                .withProperty("app.wechat-pay.private-key-path", privateKey.toString())
                .withProperty("app.wechat-pay.public-key-path", publicKey.toString())
                .withProperty("app.wechat-pay.public-key-id", "test-public-key")
                .withProperty("app.wechat-pay.api-v3-key", "00000000000000000000000000000000")
                .withProperty("app.wechat-pay.notify-url", "https://example.test/api/wechat-pay/notify")
                .withProperty("app.wechat-pay.enabled", "false");
        XingweiIntegrationConfigService service = new XingweiIntegrationConfigService(environment);

        assertTrue(service.weCom().readConfigured());
        assertTrue(service.weCom().writeCredentialPresent());
        assertTrue(service.payment().configReady());
        assertFalse(service.payment().enabled());
        assertTrue(service.payment().miniAppIdMatches());
        assertFalse(service.payment().toString().contains("test-merchant"));
        assertFalse(service.payment().toString().contains(privateKey.toString()));
        assertFalse(service.weCom().toString().contains("test-read-secret"));

        environment.withProperty("app.wechat-pay.public-key-path", temp.resolve("missing.pem").toString());
        assertFalse(service.payment().configReady());
    }
}
