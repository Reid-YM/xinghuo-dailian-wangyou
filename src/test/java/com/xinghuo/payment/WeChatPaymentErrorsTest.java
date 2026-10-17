package com.xinghuo.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingRequestHeaderException;

class WeChatPaymentErrorsTest {
    @Test
    void missingWechatSignatureHeaderIsBadRequestNotServiceFailure() {
        WeChatPaymentErrors errors = new WeChatPaymentErrors();
        MissingRequestHeaderException missing = new MissingRequestHeaderException("Wechatpay-Serial", null);

        var response = errors.missingHeader(missing);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_CALLBACK", response.getBody().get("error"));
        assertEquals("缺少微信支付回调签名请求头", response.getBody().get("message"));
    }
}
