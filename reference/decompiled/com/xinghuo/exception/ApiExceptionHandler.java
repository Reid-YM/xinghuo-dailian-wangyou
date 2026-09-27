/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.controller.AdminApiController
 *  com.xinghuo.controller.AdminLeadController
 *  com.xinghuo.controller.CustomerAccountController
 *  com.xinghuo.controller.PublicApiController
 *  com.xinghuo.controller.SeoController
 *  com.xinghuo.exception.ApiExceptionHandler
 *  com.xinghuo.exception.BadRequestException
 *  com.xinghuo.exception.ConflictException
 *  com.xinghuo.exception.NotFoundException
 *  com.xinghuo.guide.AdminGuideController
 *  com.xinghuo.guide.PublicGuideController
 *  jakarta.persistence.OptimisticLockException
 *  org.apache.catalina.connector.ClientAbortException
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.dao.DataIntegrityViolationException
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.HttpStatusCode
 *  org.springframework.http.ResponseEntity
 *  org.springframework.http.converter.HttpMessageNotReadableException
 *  org.springframework.orm.ObjectOptimisticLockingFailureException
 *  org.springframework.web.HttpMediaTypeNotSupportedException
 *  org.springframework.web.bind.MethodArgumentNotValidException
 *  org.springframework.web.bind.annotation.ExceptionHandler
 *  org.springframework.web.bind.annotation.RestControllerAdvice
 *  org.springframework.web.context.request.async.AsyncRequestNotUsableException
 *  org.springframework.web.multipart.MaxUploadSizeExceededException
 *  org.springframework.web.servlet.resource.NoResourceFoundException
 */
package com.xinghuo.exception;

import com.xinghuo.controller.AdminApiController;
import com.xinghuo.controller.AdminLeadController;
import com.xinghuo.controller.CustomerAccountController;
import com.xinghuo.controller.PublicApiController;
import com.xinghuo.controller.SeoController;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.guide.AdminGuideController;
import com.xinghuo.guide.PublicGuideController;
import jakarta.persistence.OptimisticLockException;
import java.time.Instant;
import java.util.Map;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice(assignableTypes={PublicApiController.class, AdminApiController.class, CustomerAccountController.class, AdminLeadController.class, PublicGuideController.class, AdminGuideController.class, SeoController.class})
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private ResponseEntity<Map<String, Object>> body(HttpStatus s, String code, String message) {
        return ResponseEntity.status((HttpStatusCode)s).body(Map.of("code", code, "message", message, "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(value={ClientAbortException.class, AsyncRequestNotUsableException.class})
    void clientAbort(Exception e) {
        log.debug("Client disconnected before response completed: {}", (Object)e.getMessage());
    }

    @ExceptionHandler(value={NotFoundException.class})
    ResponseEntity<?> notFound() {
        return this.body(HttpStatus.NOT_FOUND, "NOT_FOUND", "\u672a\u627e\u5230\u5bf9\u5e94\u6570\u636e");
    }

    @ExceptionHandler(value={NoResourceFoundException.class})
    ResponseEntity<?> noResource() {
        return this.body(HttpStatus.NOT_FOUND, "NOT_FOUND", "\u9875\u9762\u672a\u627e\u5230");
    }

    @ExceptionHandler(value={BadRequestException.class})
    ResponseEntity<?> bad(BadRequestException e) {
        return this.body(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", e.getMessage());
    }

    @ExceptionHandler(value={ConflictException.class})
    ResponseEntity<?> conflict(ConflictException e) {
        return this.body(HttpStatus.CONFLICT, "CONFLICT", e.getMessage());
    }

    @ExceptionHandler(value={OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    ResponseEntity<?> optimistic() {
        return this.body(HttpStatus.CONFLICT, "STALE_DATA", "\u6570\u636e\u5df2\u88ab\u5176\u4ed6\u7528\u6237\u4fee\u6539\uff0c\u8bf7\u5237\u65b0\u540e\u91cd\u8bd5");
    }

    @ExceptionHandler(value={MethodArgumentNotValidException.class})
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        String m = e.getBindingResult().getFieldErrors().stream().findFirst().map(x -> x.getField() + ": " + x.getDefaultMessage()).orElse("\u8bf7\u6c42\u53c2\u6570\u4e0d\u6b63\u786e");
        return this.body(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", m);
    }

    @ExceptionHandler(value={HttpMessageNotReadableException.class})
    ResponseEntity<?> unreadable() {
        return this.body(HttpStatus.BAD_REQUEST, "INVALID_JSON", "\u8bf7\u6c42\u5185\u5bb9\u683c\u5f0f\u4e0d\u6b63\u786e");
    }

    @ExceptionHandler(value={HttpMediaTypeNotSupportedException.class})
    ResponseEntity<?> mediaType() {
        return this.body(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "\u8bf7\u6c42\u683c\u5f0f\u4e0d\u53d7\u652f\u6301\uff0c\u8bf7\u5237\u65b0\u9875\u9762\u540e\u91cd\u8bd5");
    }

    @ExceptionHandler(value={MaxUploadSizeExceededException.class})
    ResponseEntity<?> tooLarge() {
        return this.body(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "\u56fe\u7247\u8d85\u8fc7\u5141\u8bb8\u5927\u5c0f");
    }

    @ExceptionHandler(value={DataIntegrityViolationException.class})
    ResponseEntity<?> integrity(DataIntegrityViolationException e) {
        log.warn("Integrity violation", (Throwable)e);
        return this.body(HttpStatus.CONFLICT, "DATA_CONFLICT", "\u6570\u636e\u51b2\u7a81\uff0c\u8bf7\u68c0\u67e5\u552f\u4e00\u5b57\u6bb5\u6216\u5173\u8054\u5173\u7cfb");
    }

    @ExceptionHandler(value={Exception.class})
    ResponseEntity<?> unknown(Exception e) {
        log.error("Unhandled error", (Throwable)e);
        return this.body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "\u670d\u52a1\u5668\u5185\u90e8\u9519\u8bef");
    }
}

