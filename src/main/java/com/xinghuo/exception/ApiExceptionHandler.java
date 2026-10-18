package com.xinghuo.exception;

import com.xinghuo.controller.AdminApiController;
import com.xinghuo.controller.AdminLeadController;
import com.xinghuo.controller.PublicApiController;
import com.xinghuo.controller.XingweiOperationsController;
import com.xinghuo.controller.XingweiAdminAccountController;
import com.xinghuo.controller.XingweiPaymentWorkspaceController;
import com.xinghuo.controller.XingweiAuxiliaryWorkspaceController;
import com.xinghuo.controller.XingweiCatalogDetailsController;
import com.xinghuo.controller.XingweiServiceCatalogController;
import com.xinghuo.controller.XingweiBusinessMaintenanceController;
import org.springframework.dao.OptimisticLockingFailureException;
import com.xinghuo.controller.SeoController;
import com.xinghuo.account.service.AccountException;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.guide.AdminGuideController;
import com.xinghuo.guide.PublicGuideController;
import jakarta.persistence.OptimisticLockException;
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

@RestControllerAdvice(assignableTypes={com.xinghuo.controller.GuestBusinessApiController.class, com.xinghuo.controller.XingweiBusinessScopeController.class, PublicApiController.class, AdminApiController.class, AdminLeadController.class, PublicGuideController.class, AdminGuideController.class, SeoController.class, XingweiOperationsController.class, XingweiAdminAccountController.class, XingweiPaymentWorkspaceController.class, XingweiAuxiliaryWorkspaceController.class, XingweiCatalogDetailsController.class, XingweiServiceCatalogController.class, XingweiBusinessMaintenanceController.class})
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private ResponseEntity<Map<String, Object>> body(HttpStatus s, String code, String message) {
        return ResponseEntity.status((HttpStatusCode)s).body(Map.of("code", code, "message", message));
    }

    @ExceptionHandler(value={ClientAbortException.class, AsyncRequestNotUsableException.class})
    void clientAbort(Exception e) {
        log.debug("Client disconnected before response completed: {}", e.getMessage());
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

    @ExceptionHandler(value={AccountException.class})
    ResponseEntity<?> account(AccountException e) {
        // 公开进度/订单端点套了 IP 限流（修复 C-1），超限抛 AccountException.RATE_LIMITED。
        // 账号模块自己的 AccountExceptionHandler 只覆盖 com.xinghuo.account.web，
        // 不会接住这里，因此在公开 API 的 advice 里补一个映射，超限返回 429 而非 500。
        return this.body(e.getStatus(), e.getCode(), e.getMessage());
    }

    @ExceptionHandler(value={OptimisticLockException.class, OptimisticLockingFailureException.class})
    ResponseEntity<?> optimistic() {
        return this.body(HttpStatus.CONFLICT, "STALE_DATA", "\u6570\u636e\u5df2\u88ab\u5176\u4ed6\u7528\u6237\u4fee\u6539\uff0c\u8bf7\u5237\u65b0\u540e\u91cd\u8bd5");
    }

    @ExceptionHandler(value={MethodArgumentNotValidException.class})
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        String m = e.getBindingResult().getFieldErrors().stream().findFirst().map(x -> x.getDefaultMessage()).orElse("\u8bf7\u6c42\u53c2\u6570\u4e0d\u6b63\u786e");
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
