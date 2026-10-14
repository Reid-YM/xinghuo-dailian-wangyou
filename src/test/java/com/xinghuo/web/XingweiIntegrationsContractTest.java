package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xinghuo.service.XingweiIntegrationConfigService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class XingweiIntegrationsContractTest {
    @Test
    void integrationPageUsesPermissionsAndKeepsSecretsServerSide() throws Exception {
        String controller = Files.readString(Path.of("src/main/java/com/xinghuo/controller/XingweiIntegrationsController.java"));
        String template = Files.readString(Path.of("src/main/resources/templates/admin-xingwei-integrations.html"));

        assertTrue(controller.contains("@PreAuthorize(\"@xingweiPermissionGuard.hasAny(authentication, 'employee:view', 'payment:view')\")"));
        assertTrue(controller.contains("permissions.has(authentication, \"employee:view\")"));
        assertTrue(controller.contains("permissions.has(authentication, \"payment:view\")"));
        assertTrue(template.contains("${weCom != null}"));
        assertTrue(template.contains("${payment != null}"));
        assertFalse(template.contains("weCom.appSecret"));
        assertFalse(template.contains("payment.apiV3Key"));
    }

    @Test
    void integrationPageRendersBothStatusesAndHidesUnavailableSection() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        Context context = new Context();
        context.setVariable("csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token"));
        context.setVariable("weCom", new XingweiIntegrationConfigService.WeComStatus(
                List.of(new XingweiIntegrationConfigService.Check("WECOM_CORP_ID", "企业 ID", true)),
                true, false));
        context.setVariable("payment", new XingweiIntegrationConfigService.PaymentStatus(
                List.of(new XingweiIntegrationConfigService.Check("WECHAT_PAY_MERCHANT_ID", "商户号", false)),
                false, false, false));

        String html = engine.process("admin-xingwei-integrations", context);
        assertTrue(html.contains("读取配置齐全"));
        assertTrue(html.contains("真实收款开关关闭"));
        assertTrue(html.contains("WECOM_CORP_ID"));
        assertTrue(html.contains("WECHAT_PAY_MERCHANT_ID"));
        assertTrue(html.contains("<h2>微信支付</h2>"));

        context.setVariable("payment", null);
        String weComOnly = engine.process("admin-xingwei-integrations", context);
        assertFalse(weComOnly.contains("<h2>微信支付</h2>"));
    }
}
