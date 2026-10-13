package com.xinghuo.payment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.core.notification.RequestParam;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.web.AccountAuthInterceptor;
import com.xinghuo.account.web.RequireAccount;
import com.xinghuo.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "app.wechat-pay", name = "enabled", havingValue = "true")
public class WeChatPaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(WeChatPaymentController.class);
    private final WeChatNativePaymentService payments;
    private final WeChatMiniPaymentIdentityService miniIdentity;
    private final AuditService audit;
    private final String paymentAppId;

    public WeChatPaymentController(WeChatNativePaymentService payments,
                                   WeChatMiniPaymentIdentityService miniIdentity,
                                   AuditService audit,
                                   @Value("${app.wechat-pay.app-id:}") String paymentAppId) {
        this.payments = payments;
        this.miniIdentity = miniIdentity;
        this.audit = audit;
        this.paymentAppId = paymentAppId == null ? "" : paymentAppId.trim();
    }

    public record QuoteAmount(BigDecimal amount, String subject) {}
    public record MiniLogin(@NotBlank String loginCode) {}
    public record PaymentView(String paymentNo, String businessNo, String subject,
                              BigDecimal amount, String status, java.time.Instant expiresAt,
                              String channel) {
        static PaymentView of(WeChatNativePaymentService.Payment p) {
            return new PaymentView(p.paymentNo(), p.businessNo(), p.subject(), p.amount(),
                    p.status(), p.expiresAt(), p.channel());
        }
    }
    public record MiniProgramPayView(String paymentNo, String businessNo, String subject,
                                     BigDecimal amount, String status, java.time.Instant expiresAt,
                                     String timeStamp, String nonceStr, String packageVal,
                                     String signType, String paySign) {
        static MiniProgramPayView of(WeChatNativePaymentService.MiniProgramPay p) {
            return new MiniProgramPayView(p.paymentNo(), p.businessNo(), p.subject(), p.amount(), p.status(),
                    p.expiresAt(), p.timeStamp(), p.nonceStr(), p.packageVal(), p.signType(), p.paySign());
        }
    }

    @PostMapping("/api/admin/orders/{orderNo}/wechat-payment")
    public PaymentView prepare(@PathVariable String orderNo, @RequestBody QuoteAmount body,
                               Authentication admin, HttpServletRequest request) {
        if (admin == null) throw new IllegalStateException("需要后台登录");
        PaymentView payment = PaymentView.of(payments.prepare(orderNo, body.amount(), body.subject()));
        audit.log("WECHAT_PAYMENT_PREPARE", "PAYMENT", payment.paymentNo(), admin.getName(),
                null, payment, "客服确认最终报价", request);
        return payment;
    }

    @GetMapping("/api/admin/orders/{orderNo}/wechat-payment")
    public ResponseEntity<PaymentView> adminLatest(@PathVariable String orderNo, Authentication admin) {
        if (admin == null) throw new IllegalStateException("需要后台登录");
        WeChatNativePaymentService.Payment payment = payments.latestForAdminOrder(orderNo);
        return payment == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(PaymentView.of(payment));
    }

    @PostMapping("/api/admin/wechat-payments/{paymentNo}/close")
    public Map<String, String> close(@PathVariable String paymentNo, Authentication admin,
                                     HttpServletRequest request) {
        if (admin == null) throw new IllegalStateException("需要后台登录");
        String status = payments.close(paymentNo) ? "CANCELLED" : "CAPTURED";
        audit.log("WECHAT_PAYMENT_CLOSE", "PAYMENT", paymentNo, admin.getName(),
                null, Map.of("status", status), "后台关闭待支付单", request);
        return Map.of("status", status);
    }

    @GetMapping("/api/account/payments/order/{businessNo}")
    @RequireAccount
    public ResponseEntity<PaymentView> latest(@PathVariable String businessNo,
                                               HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        WeChatNativePaymentService.Payment payment = payments.latestForOrder(businessNo, account.getId());
        return payment == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(PaymentView.of(payment));
    }

    @GetMapping("/api/account/payments/{paymentNo}")
    @RequireAccount
    public PaymentView status(@PathVariable String paymentNo, HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        return PaymentView.of(payments.getForAccount(paymentNo, account.getId()));
    }

    @PostMapping("/api/account/payments/{paymentNo}/native")
    @RequireAccount
    public PaymentView nativeCode(@PathVariable String paymentNo, HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        return PaymentView.of(payments.nativeQr(paymentNo, account.getId()));
    }

    /** Mini-program reads only payment intents owned by the exact wx.login-linked site account. */
    @PostMapping("/api/account/wechat-mini/payments")
    public List<PaymentView> miniPayments(@Valid @RequestBody MiniLogin body) {
        WeChatMiniPaymentIdentityService.MiniPayer payer = miniIdentity.authenticate(body.loginCode());
        requireMiniPaymentApp(payer);
        return payments.latestForAccount(payer.accountId()).stream().map(PaymentView::of).toList();
    }

    /** The amount and subject come exclusively from the staff-created payment intent. */
    @PostMapping("/api/account/wechat-mini/payments/{paymentNo}/prepay")
    public MiniProgramPayView miniPrepay(@PathVariable String paymentNo,
                                         @Valid @RequestBody MiniLogin body) {
        WeChatMiniPaymentIdentityService.MiniPayer payer = miniIdentity.authenticate(body.loginCode());
        requireMiniPaymentApp(payer);
        return MiniProgramPayView.of(payments.miniProgramPrepay(paymentNo, payer.accountId(), payer.openId()));
    }

    private void requireMiniPaymentApp(WeChatMiniPaymentIdentityService.MiniPayer payer) {
        if (paymentAppId.isBlank() || !paymentAppId.equals(payer.miniAppId())) {
            throw new IllegalStateException("小程序 AppID 尚未与微信支付配置对齐，暂不能在小程序内支付");
        }
    }

    @GetMapping(value = "/api/account/payments/{paymentNo}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    @RequireAccount
    public ResponseEntity<byte[]> qr(@PathVariable String paymentNo, HttpServletRequest request) throws Exception {
        Account account = AccountAuthInterceptor.requireAccount(request);
        WeChatNativePaymentService.Payment payment = payments.getForAccount(paymentNo, account.getId());
        if (!"APPROVED".equals(payment.status()) || !"NATIVE".equals(payment.channel())
                || payment.codeUrl() == null || payment.expiresAt() == null
                || !payment.expiresAt().isAfter(java.time.Instant.now())) {
            return ResponseEntity.notFound().build();
        }
        BitMatrix matrix = new MultiFormatWriter().encode(payment.codeUrl(), BarcodeFormat.QR_CODE, 320, 320);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore()).body(out.toByteArray());
    }

    @PostMapping(value = "/api/wechat-pay/notify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> notify(@RequestBody String originalBody,
            @RequestHeader("Wechatpay-Serial") String serial,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            @RequestHeader("Wechatpay-Signature") String signature,
            @RequestHeader("Wechatpay-Timestamp") String timestamp) {
        RequestParam request = new RequestParam.Builder()
                .serialNumber(serial).nonce(nonce).signature(signature)
                .timestamp(timestamp).body(originalBody).build();
        try {
            payments.acceptNotification(request, originalBody);
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
        } catch (ValidationException invalid) {
            LOG.warn("WeChat Pay notification signature rejected");
            return ResponseEntity.status(401).body(Map.of("code", "FAIL", "message", "验签失败"));
        } catch (Exception failure) {
            LOG.error("WeChat Pay notification processing failed", failure);
            return ResponseEntity.internalServerError().body(Map.of("code", "FAIL", "message", "处理失败"));
        }
    }
}
