package com.xinghuo.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingRequestHeaderException;

class WeChatPaymentErrorsTest {
    @Test
    void missingWechatSignatureHeaderIsBadRequestNotServiceFailure() throws Exception {
        WeChatPaymentErrors errors = new WeChatPaymentErrors();
        var method = WeChatPaymentErrorsTest.class.getDeclaredMethod("callback", String.class);
        MissingRequestHeaderException missing = new MissingRequestHeaderException(
                "Wechatpay-Serial", new MethodParameter(method, 0));

        var response = errors.missingHeader(missing);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_CALLBACK", response.getBody().get("error"));
        assertEquals("缺少微信支付回调签名请求头", response.getBody().get("message"));
    }

    private static void callback(String serial) {}
}
