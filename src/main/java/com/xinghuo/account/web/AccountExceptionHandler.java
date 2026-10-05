package com.xinghuo.account.web;

import com.xinghuo.account.service.AccountException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 账号模块的异常处理。
 *
 * <p>必须限定 basePackages：下面的兜底 {@code @ExceptionHandler(Exception.class)}
 * 若不加限定会接管全站控制器的异常，把基线 {@code ApiExceptionHandler} 的
 * 错误响应格式一并改掉（含后台与公开 API），属于会波及线上的副作用。
 */
@RestControllerAdvice(basePackages = "com.xinghuo.account.web")
public class AccountExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AccountExceptionHandler.class);

    @ExceptionHandler(AccountException.class)
    public ResponseEntity<Map<String, Object>> handleAccount(AccountException e) {
        return ResponseEntity.status(e.getStatus()).body(body(e.getCode(), e.getMessage()));
    }

    /** Bean Validation 失败：取第一条字段错误，转成与其他错误一致的格式。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage() == null ? "参数不合法" : err.getDefaultMessage())
                .orElse("参数不合法");
        return ResponseEntity.badRequest().body(body("INVALID_INPUT", message));
    }

    /**
     * 唯一键冲突转 409。
     *
     * <p>多处业务是「先 exists/count 再 save」，并发下仍可能撞上唯一索引
     * （contact_norm / email_norm / 收藏 / 进度均有唯一键）。不兜这一层的话，
     * DataIntegrityViolationException 会落到通用处理器变成 500「服务暂时不可用」，
     * 让调用方以为是服务故障而不是可重试的冲突。
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegrity(
            org.springframework.dao.DataIntegrityViolationException e) {
        log.warn("账号模块唯一键冲突", e);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                .body(body("CONFLICT", "该操作与已有数据冲突，请刷新后重试"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(body("INVALID_INPUT", "参数不合法"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception e) {
        // 记录但不外抛细节：堆栈可能包含联系方式等个人信息。
        log.error("账号模块未预期异常", e);
        return ResponseEntity.internalServerError().body(body("INTERNAL_ERROR", "服务暂时不可用，请稍后再试"));
    }

    private Map<String, Object> body(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        return body;
    }
}
