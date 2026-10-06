package com.xinghuo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xinghuo.service.SiteSettingService;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

class CustomerServiceControllerTest {

    @Test
    void redirectsToConfiguredDefaultCustomerService() {
        CustomerServiceController controller = controller(
                true,
                "https://work.weixin.qq.com/kfid/kfc123456789",
                "", "", "", "");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", "https://wydlgf.cn/support/order?campaign=home");

        ResponseEntity<Void> response = controller.startPath("order", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("https://work.weixin.qq.com/kfid/kfc123456789"));
        assertThat(response.getHeaders().getCacheControl()).contains("no-store");
    }

    @Test
    void prefersTopicSpecificCustomerServiceLink() {
        CustomerServiceController controller = controller(
                true,
                "https://work.weixin.qq.com/kfid/default123",
                "https://work.weixin.qq.com/kfid/game123",
                "https://work.weixin.qq.com/kfid/order123",
                "",
                "");

        ResponseEntity<Void> response = controller.startPath("order", new MockHttpServletRequest());

        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("https://work.weixin.qq.com/kfid/order123"));
    }

    @Test
    void persistedTopicLinkOverridesEnvironmentWithoutRestart() {
        SiteSettingService settings = mock(SiteSettingService.class);
        when(settings.get(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        when(settings.get("customerServiceOrderUrl", "https://work.weixin.qq.com/kfid/env-order"))
                .thenReturn("https://work.weixin.qq.com/kfid/admin-order");

        CustomerServiceController controller = new CustomerServiceController(
                true,
                "https://work.weixin.qq.com/kfid/default123",
                "",
                "https://work.weixin.qq.com/kfid/env-order",
                "",
                "",
                settings);

        ResponseEntity<Void> response = controller.startPath("order", new MockHttpServletRequest());

        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("https://work.weixin.qq.com/kfid/admin-order"));
    }

    @Test
    void acceptsOfficialGeneratedSceneLink() {
        CustomerServiceController controller = controller(
                true,
                "https://work.weixin.qq.com/kfid/default123",
                "https://work.weixin.qq.com/kf/game123?enc_scene=abc&scene_param=game",
                "", "", "");

        ResponseEntity<Void> response = controller.startPath("game", new MockHttpServletRequest());

        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("https://work.weixin.qq.com/kf/game123?enc_scene=abc&scene_param=game"));
    }

    @Test
    void invalidTopicSpecificLinkFallsBackToDefault() {
        CustomerServiceController controller = controller(
                true,
                "https://work.weixin.qq.com/kfid/default123",
                "https://example.com/kfid/fake",
                "", "", "");

        ResponseEntity<Void> response = controller.startPath("game", new MockHttpServletRequest());

        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("https://work.weixin.qq.com/kfid/default123"));
    }

    @Test
    void supportHomeRedirectsToFourCategoryHub() {
        CustomerServiceController controller = controller(
                true,
                "https://work.weixin.qq.com/kfid/default123",
                "", "", "", "");

        assertThat(controller.supportHome()).isEqualTo("redirect:/contact");
    }

    @Test
    void generalSupportPageRedirectsToFourCategoryHub() {
        CustomerServiceController controller = controller(
                true,
                "https://work.weixin.qq.com/kfid/default123",
                "", "", "", "");

        assertThat(controller.support("general", new ExtendedModelMap()))
                .isEqualTo("redirect:/contact");
    }

    @Test
    void fallsBackWhenFeatureIsDisabled() {
        CustomerServiceController controller = controller(
                false,
                "https://work.weixin.qq.com/kfid/kfc123456789",
                "", "", "", "");

        ResponseEntity<Void> response = controller.startPath("game", new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("/contact?customerService=unavailable"));
    }

    @Test
    void rejectsNonOfficialDefaultRedirectTarget() {
        CustomerServiceController controller = controller(
                true,
                "https://example.com/kfid/fake",
                "", "", "", "");

        ResponseEntity<Void> response = controller.startPath("business", new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("/contact?customerService=unavailable"));
    }

    @Test
    void rejectsOfficialHostWithWrongPath() {
        CustomerServiceController controller = controller(
                true,
                "https://work.weixin.qq.com/not-kf/example",
                "", "", "", "");

        ResponseEntity<Void> response = controller.startPath("after-sales", new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("/contact?customerService=unavailable"));
    }

    private static CustomerServiceController controller(
            boolean enabled,
            String defaultUrl,
            String gameUrl,
            String orderUrl,
            String afterSalesUrl,
            String businessUrl) {
        return new CustomerServiceController(
                enabled, defaultUrl, gameUrl, orderUrl, afterSalesUrl, businessUrl, null);
    }
}
