package com.xinghuo.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wechat.pay.java.core.exception.ServiceException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Payment gateway response codes must be actionable without leaking raw API responses. */
class WeChatPrepayErrorContractTest {
    @Test
    void merchantBindingAndNativeEntitlementHaveSpecificRemediation() {
        assertTrue(ApiExceptionHandler.weChatFailureMessage("APPID_MCHID_NOT_MATCH").contains("AppID"));
        assertTrue(ApiExceptionHandler.weChatFailureMessage("NO_AUTH").contains("Native"));
        assertTrue(ApiExceptionHandler.weChatFailureMessage("SIGN_ERROR").contains("证书"));
        assertTrue(ApiExceptionHandler.weChatFailureMessage("OUT_TRADE_NO_USED").contains("核对交易状态"));
    }

    @Test
    void upstreamErrorsProduceBadGatewayInsteadOfInternalError() {
        ServiceException exception = mock(ServiceException.class);
        when(exception.getErrorCode()).thenReturn("NO_AUTH");
        ResponseEntity<?> reply = new ApiExceptionHandler().weChatService(exception);
        assertEquals(HttpStatus.BAD_GATEWAY, reply.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) reply.getBody();
        assertEquals("WECHAT_PAY_REJECTED", body.get("code"));
        assertTrue(String.valueOf(body.get("message")).contains("Native"));
    }

    @Test
    void paramErrorReturnsOnlyApprovedFieldNameAndNeverGatewayValues() {
        ServiceException exception = mock(ServiceException.class);
        when(exception.getErrorCode()).thenReturn("PARAM_ERROR");
        when(exception.getResponseBody()).thenReturn(
                "{\"code\":\"PARAM_ERROR\",\"message\":\"private_key_sensitive\","
                + "\"detail\":{\"field\":\"time_expire\",\"value\":\"sensitive-token\"}}");
        ResponseEntity<?> reply = new ApiExceptionHandler().weChatService(exception);
        assertEquals(HttpStatus.BAD_GATEWAY, reply.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) reply.getBody();
        assertTrue(String.valueOf(body.get("message")).contains("time_expire"));
        assertFalse(String.valueOf(body.get("message")).contains("sensitive-token"));
        assertFalse(String.valueOf(body.get("message")).contains("private_key_sensitive"));
        assertEquals("", ApiExceptionHandler.safeWeChatParameterField(
                "{\"detail\":{\"field\":\"card_secret_password\"}}"));
        assertEquals("", ApiExceptionHandler.safeWeChatParameterField("not-json"));
        assertEquals("notify_url", ApiExceptionHandler.safeWeChatParameterField(
                "{\"detail\":{\"field\":\"notify_url\"}}"));
    }

    @Test
    void malformedOrSensitiveRemoteCodesNeverAppearInCustomerResponses() {
        String unsafe = "SIGN_ERROR_private_key_secret\\n";
        assertEquals("UNKNOWN", ApiExceptionHandler.safeWeChatErrorCode(unsafe));
        String message = ApiExceptionHandler.weChatFailureMessage(
                ApiExceptionHandler.safeWeChatErrorCode(unsafe));
        assertFalse(message.contains("private_key_secret"));
        assertTrue(message.contains("UNKNOWN"));
    }
}
