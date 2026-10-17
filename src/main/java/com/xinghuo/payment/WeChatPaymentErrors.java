package com.xinghuo.payment;

import com.xinghuo.account.service.AccountException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = WeChatPaymentController.class)
public class WeChatPaymentErrors {
    private static final Logger LOG = LoggerFactory.getLogger(WeChatPaymentErrors.class);

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<Map<String, String>> missingHeader(MissingRequestHeaderException error) {
        LOG.warn("WeChat Pay callback missing required header {}", error.getHeaderName());
        return response(HttpStatus.BAD_REQUEST, "INVALID_CALLBACK", "缺少微信支付回调签名请求头");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> bad(IllegalArgumentException error) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_INPUT", error.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> conflict(IllegalStateException error) {
        return response(HttpStatus.CONFLICT, "PAYMENT_CONFLICT", error.getMessage());
    }

    @ExceptionHandler(AccountException.class)
    ResponseEntity<Map<String, String>> account(AccountException error) {
        return response(error.getStatus(), error.getCode(), error.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> integrity(DataIntegrityViolationException error) {
        LOG.warn("WeChat Pay database conflict", error);
        return response(HttpStatus.CONFLICT, "PAYMENT_CONFLICT", "支付记录已变化，请刷新后重试");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> unexpected(Exception error) {
        LOG.error("WeChat Pay request failed", error);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_ERROR", "支付服务暂不可用，请稍后重试");
    }

    private ResponseEntity<Map<String, String>> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("error", code, "message", message));
    }
}
