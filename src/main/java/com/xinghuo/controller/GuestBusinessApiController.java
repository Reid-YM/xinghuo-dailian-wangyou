package com.xinghuo.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.xinghuo.payment.WeChatNativePaymentService;
import com.xinghuo.service.GuestQuoteFlowService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Private guest business APIs. Authorization is the HttpOnly XH_GUEST capability cookie. */
@RestController
@RequestMapping("/api/guest/business")
public class GuestBusinessApiController {
    private final GuestQuoteFlowService guests;

    public GuestBusinessApiController(GuestQuoteFlowService guests) {
        this.guests = guests;
    }

    @GetMapping("/{businessNo}")
    public Map<String, Object> snapshot(@PathVariable String businessNo, HttpServletRequest request) {
        return guests.snapshot(token(request), businessNo);
    }

    @PostMapping("/{businessNo}/accept")
    public Map<String, Object> accept(@PathVariable String businessNo, HttpServletRequest request) {
        return guests.accept(token(request), businessNo, request);
    }

    @PostMapping("/{businessNo}/payment/native")
    public PaymentView nativePayment(@PathVariable String businessNo, HttpServletRequest request) {
        return PaymentView.of(guests.nativePayment(token(request), businessNo));
    }

    @GetMapping(value = "/{businessNo}/payment/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@PathVariable String businessNo, HttpServletRequest request) throws Exception {
        WeChatNativePaymentService.Payment payment = guests.payment(token(request), businessNo);
        if (payment == null || !"APPROVED".equals(payment.status()) || payment.codeUrl() == null
                || payment.expiresAt() == null || !payment.expiresAt().isAfter(Instant.now())) {
            return ResponseEntity.notFound().build();
        }
        BitMatrix matrix = new MultiFormatWriter().encode(
                payment.codeUrl(), BarcodeFormat.QR_CODE, 320, 320);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore())
                .body(out.toByteArray());
    }

    private String token(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (GuestQuoteFlowService.COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public record PaymentView(
            String paymentNo,
            String businessNo,
            String subject,
            BigDecimal amount,
            String status,
            Instant expiresAt) {
        static PaymentView of(WeChatNativePaymentService.Payment payment) {
            return new PaymentView(payment.paymentNo(), payment.businessNo(), payment.subject(),
                    payment.amount(), payment.status(), payment.expiresAt());
        }
    }
}
