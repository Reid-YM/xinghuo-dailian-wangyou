/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.servlet.http.HttpServletRequest
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.CustomerOrderRepository;
import com.xinghuo.service.AuditService;
import com.xinghuo.service.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] ALPH = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final CustomerOrderRepository repo;
    private final UploadService uploads;
    private final AuditService audit;

    @Transactional(readOnly=true)
    public List<Map<String, Object>> publicCases() {
        ArrayList<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (CustomerOrder o : this.repo.findByAllowPublicCaseTrueOrderByFinishedAtDescUpdatedAtDesc()) {
            LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("maskedNo", this.maskBusinessNo(o.getBusinessNo()));
            m.put("summary", o.getPublicSummary());
            m.put("finishedAt", o.getFinishedAt());
            m.put("status", "completed");
            out.add(m);
            if (out.size() < 12) continue;
            break;
        }
        return out;
    }

    @Transactional
    public Map<String, Object> setPublicCase(String orderNo, boolean enabled, String summary, String operator) {
        CustomerOrder o = this.repo.findByOrderNo(orderNo).orElseThrow(NotFoundException::new);
        o.setAllowPublicCase(enabled);
        o.setPublicEnabledAt(enabled ? Instant.now() : null);
        if (summary != null && !summary.isBlank()) {
            String s = summary.trim();
            if (s.length() > 240) {
                throw new BadRequestException("\u6848\u4f8b\u6458\u8981\u8fc7\u957f");
            }
            o.setPublicSummary(s);
        }
        if (enabled && o.getFinishedAt() == null && o.getStatus() == CustomerOrder.Status.COMPLETED) {
            o.setFinishedAt(Instant.now());
        }
        o.setUpdatedAt(Instant.now());
        this.repo.save(o);
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("orderNo", o.getOrderNo());
        out.put("allowPublicCase", o.isAllowPublicCase());
        out.put("publicSummary", o.getPublicSummary());
        return out;
    }

    private String maskBusinessNo(String no) {
        if (no == null || no.length() <= 4) {
            return "XH****";
        }
        return no.substring(0, 2) + "****" + no.substring(no.length() - 2);
    }

    @Transactional(readOnly=true)
    public ApiDtos.OrderPublicResponse getPublic(String publicNo) {
        return this.pub(this.findByPublicNo(publicNo));
    }

    @Transactional(readOnly=true)
    public CustomerOrder findByPublicNo(String publicNo) {
        String normalized = this.normalizeNo(publicNo);
        if (this.validBusinessNo(normalized)) {
            return this.repo.findByBusinessNo(normalized).orElseThrow(NotFoundException::new);
        }
        if (this.validLegacyOrderNo(normalized)) {
            return this.repo.findByOrderNo(normalized).orElseThrow(NotFoundException::new);
        }
        throw new NotFoundException();
    }

    @Transactional(readOnly=true)
    public CustomerOrder findByOrderNo(String orderNo) {
        String normalized = this.normalizeNo(orderNo);
        if (!this.validBusinessNo(normalized) && !this.validLegacyOrderNo(normalized)) {
            throw new NotFoundException();
        }
        return this.repo.findByOrderNo(normalized).orElseThrow(NotFoundException::new);
    }

    @Transactional(readOnly=true)
    public List<ApiDtos.AdminOrderResponse> list() {
        return this.repo.findTop100ByOrderByUpdatedAtDesc().stream().map(this::admin).toList();
    }

    @Transactional
    public ApiDtos.AdminOrderResponse create(ApiDtos.CreateOrderRequest r, String operator, HttpServletRequest req) {
        String businessNo = this.generateBusinessNo();
        CustomerOrder o = this.newOrder(businessNo, null, null, this.parseStatus(r.status()), r.progressPercent(), r.progressText(), r.customerNote(), r.internalNote());
        this.repo.save(o);
        ApiDtos.AdminOrderResponse after = this.admin(o);
        this.audit.log("ORDER_CREATE", "ORDER", o.getBusinessNo(), operator, null, after, r.reason(), req);
        return after;
    }

    @Transactional
    public ApiDtos.AdminOrderResponse createFromQuote(Quote q, String operator, HttpServletRequest req, String reason) {
        Optional<CustomerOrder> existing = this.repo.findBySourceQuoteId(q.getId());
        if (existing.isPresent()) {
            return this.admin(existing.get());
        }
        String businessNo = q.getBusinessNo();
        if (businessNo == null || businessNo.isBlank()) {
            businessNo = this.generateBusinessNo();
        }
        String internal = "\u7531\u8be2\u4ef7\u81ea\u52a8\u8f6c\u5355\uff1a\u4e1a\u52a1\u7f16\u53f7=" + businessNo + "\uff1b\u6e38\u620f=" + q.getGame() + "\uff1b\u9700\u6c42=" + q.getDemand() + (String)(q.getCurrentProgress() == null ? "" : "\uff1b\u5f53\u524d\u8fdb\u5ea6=" + q.getCurrentProgress()) + (String)(q.getPlatform() == null ? "" : "\uff1b\u5e73\u53f0=" + q.getPlatform()) + (String)(q.getExpectedTime() == null ? "" : "\uff1b\u671f\u671b\u65f6\u95f4=" + q.getExpectedTime());
        CustomerOrder o = this.newOrder(businessNo, q.getId(), q.getCustomerAccountId(), CustomerOrder.Status.WAITING_CONFIRMATION, null, "\u5df2\u6536\u5230\u9700\u6c42\uff0c\u7b49\u5f85\u5ba2\u670d\u786e\u8ba4\u62a5\u4ef7\u4e0e\u6392\u671f", null, internal);
        this.repo.save(o);
        ApiDtos.AdminOrderResponse after = this.admin(o);
        this.audit.log("ORDER_CREATE_FROM_QUOTE", "ORDER", o.getBusinessNo(), operator, null, after, this.blankToDefault(reason, "\u8be2\u4ef7\u8f6c\u8ba2\u5355"), req);
        return after;
    }

    @Transactional
    public ApiDtos.AdminOrderResponse update(String orderNo, ApiDtos.UpdateOrderRequest r, String operator, HttpServletRequest req) {
        CustomerOrder o = this.findByOrderNo(orderNo);
        ApiDtos.AdminOrderResponse before = this.admin(o);
        CustomerOrder.Status nextStatus = this.parseStatus(r.status());
        o.setStatus(nextStatus);
        o.setProgressPercent(nextStatus == CustomerOrder.Status.COMPLETED && r.progressPercent() == null ? 100 : r.progressPercent());
        o.setProgressText(r.progressText().trim());
        o.setCustomerNote(this.trimNull(r.customerNote()));
        o.setInternalNote(this.trimNull(r.internalNote()));
        if (nextStatus == CustomerOrder.Status.COMPLETED && o.getFinishedAt() == null) {
            o.setFinishedAt(Instant.now());
        }
        o.setUpdatedAt(Instant.now());
        ApiDtos.AdminOrderResponse after = this.admin(o);
        this.audit.log("ORDER_UPDATE", "ORDER", o.getBusinessNo(), operator, before, after, r.reason(), req);
        return after;
    }

    @Transactional
    public ApiDtos.AdminOrderResponse attachImage(String orderNo, UUID assetId, String operator, HttpServletRequest req) {
        CustomerOrder o = this.findByOrderNo(orderNo);
        ApiDtos.AdminOrderResponse before = this.admin(o);
        this.uploads.bind(assetId, "ORDER", o.getOrderNo());
        o.setProgressImageAssetId(assetId);
        o.setUpdatedAt(Instant.now());
        ApiDtos.AdminOrderResponse after = this.admin(o);
        this.audit.log("ORDER_IMAGE_UPDATE", "ORDER", o.getBusinessNo(), operator, before, after, "\u66f4\u65b0\u8fdb\u5ea6\u622a\u56fe", req);
        return after;
    }

    @Transactional
    public void delete(String orderNo, String operator, HttpServletRequest req) {
        CustomerOrder o = this.findByOrderNo(orderNo);
        ApiDtos.AdminOrderResponse before = this.admin(o);
        this.repo.delete(o);
        this.audit.log("ORDER_DELETE", "ORDER", o.getBusinessNo(), operator, before, null, "\u540e\u53f0\u5220\u9664\u5c65\u7ea6\u8ba2\u5355", req);
    }

    public ApiDtos.OrderPublicResponse pub(CustomerOrder o) {
        return new ApiDtos.OrderPublicResponse(o.getBusinessNo() == null ? o.getOrderNo() : o.getBusinessNo(), o.getStatus().name(), o.getProgressPercent(), o.getProgressText(), o.getUpdatedAt(), o.getProgressImageAssetId() != null, o.getCustomerNote());
    }

    public ApiDtos.AdminOrderResponse admin(CustomerOrder o) {
        return new ApiDtos.AdminOrderResponse(o.getId(), o.getOrderNo(), o.getBusinessNo() == null ? o.getOrderNo() : o.getBusinessNo(), o.getSourceQuoteId(), o.getStatus().name(), o.getProgressPercent(), o.getProgressText(), o.getProgressImageAssetId() != null, o.getCustomerNote(), o.getInternalNote(), o.getUpdatedAt(), o.getVersion());
    }

    private CustomerOrder newOrder(String businessNo, UUID sourceQuoteId, Long customerAccountId, CustomerOrder.Status status, Integer progressPercent, String progressText, String customerNote, String internalNote) {
        Instant now = Instant.now();
        return CustomerOrder.builder().id(UUID.randomUUID()).orderNo(businessNo).businessNo(businessNo).sourceQuoteId(sourceQuoteId).customerAccountId(customerAccountId).status(status).progressPercent(progressPercent).progressText(progressText.trim()).customerNote(this.trimNull(customerNote)).internalNote(this.trimNull(internalNote)).createdAt(now).updatedAt(now).build();
    }

    private CustomerOrder.Status parseStatus(String s) {
        try {
            return CustomerOrder.Status.valueOf(s.toUpperCase(Locale.ROOT));
        }
        catch (Exception e) {
            throw new BadRequestException("\u975e\u6cd5\u8ba2\u5355\u72b6\u6001");
        }
    }

    private String normalizeNo(String s) {
        return s == null ? null : s.trim().toUpperCase(Locale.ROOT);
    }

    private boolean validBusinessNo(String s) {
        return s != null && s.matches("XH[A-Z0-9]{10}");
    }

    private boolean validLegacyOrderNo(String s) {
        return s != null && s.matches("XH-[A-Z2-9]{24}");
    }

    private String generateBusinessNo() {
        for (int n = 0; n < 8; ++n) {
            StringBuilder b = new StringBuilder("XH");
            for (int i = 0; i < 10; ++i) {
                b.append(ALPH[RNG.nextInt(ALPH.length)]);
            }
            String no = b.toString();
            if (!this.repo.findByBusinessNo(no).isEmpty() || !this.repo.findByOrderNo(no).isEmpty()) continue;
            return no;
        }
        throw new IllegalStateException("\u4e1a\u52a1\u7f16\u53f7\u751f\u6210\u5931\u8d25");
    }

    private String trimNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private String blankToDefault(String s, String fallback) {
        return s == null || s.isBlank() ? fallback : s.trim();
    }

    @Generated
    public OrderService(CustomerOrderRepository repo, UploadService uploads, AuditService audit) {
        this.repo = repo;
        this.uploads = uploads;
        this.audit = audit;
    }
}

