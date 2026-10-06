package com.xinghuo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class CustomerServiceControllerTest {

    @Test
    void redirectsToConfiguredOfficialWeComCustomerService() {
        CustomerServiceController controller = new CustomerServiceController(
                true, "https://work.weixin.qq.com/kfid/kfc123456789");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", "https://wydlgf.cn/service/mw4?campaign=home");

        ResponseEntity<Void> response = controller.start("order", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("https://work.weixin.qq.com/kfid/kfc123456789"));
        assertThat(response.getHeaders().getCacheControl()).contains("no-store");
    }

    @Test
    void fallsBackWhenFeatureIsDisabled() {
        CustomerServiceController controller = new CustomerServiceController(
                false, "https://work.weixin.qq.com/kfid/kfc123456789");

        ResponseEntity<Void> response = controller.start("game", new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("/contact?customerService=unavailable"));
    }

    @Test
    void rejectsNonOfficialRedirectTargets() {
        CustomerServiceController controller = new CustomerServiceController(
                true, "https://example.com/kfid/fake");

        ResponseEntity<Void> response = controller.start("business", new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("/contact?customerService=unavailable"));
    }

    @Test
    void rejectsOfficialHostWithWrongPath() {
        CustomerServiceController controller = new CustomerServiceController(
                true, "https://work.weixin.qq.com/not-kf/example");

        ResponseEntity<Void> response = controller.start("after-sales", new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("/contact?customerService=unavailable"));
    }
}
