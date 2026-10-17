package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.CustomerOrderRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int BUSINESS_NO_LENGTH = 10;
    private static final int BUSINESS_NO_ATTEMPTS = 8;
    private static final int PUBLIC_CASE_LIMIT = 12;

    private final CustomerOrderRepository repo;
    private final UploadService uploads;
    private final AuditService audit;
    private final JdbcTemplate jdbc;

    public OrderService(CustomerOrderRepository repo, UploadService uploads, AuditService audit) {
        this(repo, uploads, audit, null);
    }

    @Autowired
    public OrderService(CustomerOrderRepository repo, UploadService uploads, AuditService audit,
                        JdbcTemplate jdbc) {
        this.repo = repo;
        this.uploads = uploads;
        this.audit = audit;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> publicCases() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (CustomerOrder order : repo.findByAllowPublicCaseTrueOrderByFinishedAtDescUpdatedAtDesc()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("maskedNo", maskBusinessNo(order.getBusinessNo()));
            row.put("summary", order.getPublicSummary());
            row.put("finishedAt", order.getFinishedAt());
            row.put("status", "completed");
            out.add(row);
            if (out.size() >= PUBLIC_CASE_LIMIT) {
                break;
            }
        }
        return out;
    }

    @Transactional
    public Map<String, Object> setPublicCase(
            String orderNo,
            boolean enabled,
            String summary,
            String operator) {
        CustomerOrder order = repo.findByOrderNo(orderNo).orElseThrow(NotFoundException::new);
        order.setAllowPublicCase(enabled);
        order.setPublicEnabledAt(enabled ? Instant.now() : null);

        if (summary != null && !summary.isBlank()) {
            String normalizedSummary = summary.trim();
            if (normalizedSummary.length() > 240) {
                throw new BadRequestException("案例摘要过长");
            }
            order.setPublicSummary(normalizedSummary);
        }

        if (enabled
                && order.getFinishedAt() == null
                && order.getStatus() == CustomerOrder.Status.COMPLETED) {
            order.setFinishedAt(Instant.now());
        }
        order.setUpdatedAt(Instant.now());
        repo.save(order);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("orderNo", order.getOrderNo());
        out.put("allowPublicCase", order.isAllowPublicCase());
        out.put("publicSummary", order.getPublicSummary());
        return out;
    }

    private String maskBusinessNo(String businessNo) {
        if (businessNo == null || businessNo.length() <= 4) {
            return "XH****";
        }
        return businessNo.substring(0, 2)
                + "****"
                + businessNo.substring(businessNo.length() - 2);
    }

    @Transactional(readOnly = true)
    public ApiDtos.OrderPublicResponse getPublic(String publicNo) {
        return pub(findByPublicNo(publicNo));
    }

    @Transactional(readOnly = true)
    public CustomerOrder findByPublicNo(String publicNo) {
        String normalized = normalizeNo(publicNo);
        if (validBusinessNo(normalized)) {
            return repo.findByBusinessNo(normalized).orElseThrow(NotFoundException::new);
        }
        if (validLegacyOrderNo(normalized)) {
            return repo.findByOrderNo(normalized).orElseThrow(NotFoundException::new);
        }
        throw new NotFoundException();
    }

    @Transactional(readOnly = true)
    public CustomerOrder findByOrderNo(String orderNo) {
        String normalized = normalizeNo(orderNo);
        if (!validBusinessNo(normalized) && !validLegacyOrderNo(normalized)) {
            throw new NotFoundException();
        }
        return repo.findByOrderNo(normalized).orElseThrow(NotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.AdminOrderResponse> list() {
        return repo.findTop100ByOrderByUpdatedAtDesc().stream()
                .map(this::admin)
                .toList();
    }

    @Transactional
    public ApiDtos.AdminOrderResponse create(
            ApiDtos.CreateOrderRequest request,
            String operator,
            HttpServletRequest httpRequest) {
        String businessNo = generateBusinessNo();
        CustomerOrder order = newOrder(
                businessNo,
                null,
                null,
                parseStatus(request.status()),
                request.progressPercent(),
                request.progressText(),
                request.customerNote(),
                request.internalNote());
        repo.save(order);

        ApiDtos.AdminOrderResponse after = admin(order);
        audit.log(
                "ORDER_CREATE",
                "ORDER",
                order.getBusinessNo(),
                operator,
                null,
                after,
                request.reason(),
                httpRequest);
        return after;
    }

    @Transactional
    public ApiDtos.AdminOrderResponse createFromQuote(
            Quote quote,
            String operator,
            HttpServletRequest httpRequest,
            String reason) {
        Optional<CustomerOrder> existing = repo.findBySourceQuoteId(quote.getId());
        if (existing.isPresent()) {
            return admin(existing.get());
        }

        String businessNo = quote.getBusinessNo();
        if (businessNo == null || businessNo.isBlank()) {
            businessNo = generateBusinessNo();
        }

        String internal = "由询价自动转单：业务编号=" + businessNo
                + "；游戏=" + quote.getGame()
                + "；需求=" + quote.getDemand()
                + (quote.getCurrentProgress() == null ? "" : "；当前进度=" + quote.getCurrentProgress())
                + (quote.getPlatform() == null ? "" : "；平台=" + quote.getPlatform())
                + (quote.getExpectedTime() == null ? "" : "；期望时间=" + quote.getExpectedTime());

        CustomerOrder order = newOrder(
                businessNo,
                quote.getId(),
                quote.getCustomerAccountId(),
                CustomerOrder.Status.WAITING_CONFIRMATION,
                null,
                "已收到需求，等待客服确认报价与排期",
                null,
                internal);
        repo.save(order);
        // The guest acceptance path updates this row via JdbcTemplate in the same transaction.
        // Flush the JPA insert now so that JDBC can observe the newly created order.
        repo.flush();

        ApiDtos.AdminOrderResponse after = admin(order);
        audit.log(
                "ORDER_CREATE_FROM_QUOTE",
                "ORDER",
                order.getBusinessNo(),
                operator,
                null,
                after,
                blankToDefault(reason, "询价转订单"),
                httpRequest);
        return after;
    }

    @Transactional
    public ApiDtos.AdminOrderResponse update(
            String orderNo,
            ApiDtos.UpdateOrderRequest request,
            String operator,
            HttpServletRequest httpRequest) {
        CustomerOrder order = findByOrderNo(orderNo);
        if (request.version() == null || request.version() != order.getVersion()) {
            throw new ConflictException("订单已被更新，请刷新后重新编辑；当前内容尚未保存");
        }

        ApiDtos.AdminOrderResponse before = admin(order);
        CustomerOrder.Status nextStatus = parseStatus(request.status());
        order.setStatus(nextStatus);
        order.setProgressPercent(
                nextStatus == CustomerOrder.Status.COMPLETED && request.progressPercent() == null
                        ? 100
                        : request.progressPercent());
        order.setProgressText(request.progressText().trim());
        order.setCustomerNote(trimNull(request.customerNote()));
        order.setInternalNote(trimNull(request.internalNote()));
        if (nextStatus == CustomerOrder.Status.COMPLETED && order.getFinishedAt() == null) {
            order.setFinishedAt(Instant.now());
        }
        order.setUpdatedAt(Instant.now());
        repo.flush();

        ApiDtos.AdminOrderResponse after = admin(order);
        audit.log(
                "ORDER_UPDATE",
                "ORDER",
                order.getBusinessNo(),
                operator,
                before,
                after,
                request.reason(),
                httpRequest);
        return after;
    }

    @Transactional
    public ApiDtos.AdminOrderResponse attachImage(
            String orderNo,
            UUID assetId,
            String operator,
            HttpServletRequest httpRequest) {
        CustomerOrder order = findByOrderNo(orderNo);
        ApiDtos.AdminOrderResponse before = admin(order);
        uploads.bind(assetId, "ORDER", order.getOrderNo());
        order.setProgressImageAssetId(assetId);
        order.setUpdatedAt(Instant.now());

        ApiDtos.AdminOrderResponse after = admin(order);
        audit.log(
                "ORDER_IMAGE_UPDATE",
                "ORDER",
                order.getBusinessNo(),
                operator,
                before,
                after,
                "更新进度截图",
                httpRequest);
        return after;
    }

    @Transactional
    public void delete(String orderNo, String operator, HttpServletRequest httpRequest) {
        CustomerOrder order = findByOrderNo(orderNo);
        if (jdbc != null) {
            Long payments = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM payment_order WHERE business_no=?",
                    Long.class, order.getBusinessNo());
            if (payments != null && payments > 0) {
                throw new ConflictException("订单已有支付记录，不能删除；请联系客服核对支付与履约状态");
            }
        }
        ApiDtos.AdminOrderResponse before = admin(order);
        repo.delete(order);
        audit.log(
                "ORDER_DELETE",
                "ORDER",
                order.getBusinessNo(),
                operator,
                before,
                null,
                "后台删除履约订单",
                httpRequest);
    }

    public ApiDtos.OrderPublicResponse pub(CustomerOrder order) {
        return new ApiDtos.OrderPublicResponse(
                order.getBusinessNo() == null ? order.getOrderNo() : order.getBusinessNo(),
                order.getStatus().name(),
                order.getProgressPercent(),
                order.getProgressText(),
                order.getUpdatedAt(),
                order.getProgressImageAssetId() != null,
                order.getCustomerNote());
    }

    public ApiDtos.AdminOrderResponse admin(CustomerOrder order) {
        return new ApiDtos.AdminOrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getBusinessNo() == null ? order.getOrderNo() : order.getBusinessNo(),
                order.getSourceQuoteId(),
                order.getStatus().name(),
                order.getProgressPercent(),
                order.getProgressText(),
                order.getProgressImageAssetId() != null,
                order.getCustomerNote(),
                order.getInternalNote(),
                order.getUpdatedAt(),
                order.getVersion());
    }

    private CustomerOrder newOrder(
            String businessNo,
            UUID sourceQuoteId,
            Long customerAccountId,
            CustomerOrder.Status status,
            Integer progressPercent,
            String progressText,
            String customerNote,
            String internalNote) {
        Instant now = Instant.now();
        return CustomerOrder.builder()
                .id(UUID.randomUUID())
                .orderNo(businessNo)
                .businessNo(businessNo)
                .sourceQuoteId(sourceQuoteId)
                .customerAccountId(customerAccountId)
                .status(status)
                .progressPercent(progressPercent)
                .progressText(progressText.trim())
                .customerNote(trimNull(customerNote))
                .internalNote(trimNull(internalNote))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private CustomerOrder.Status parseStatus(String value) {
        try {
            return CustomerOrder.Status.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new BadRequestException("非法订单状态");
        }
    }

    private String normalizeNo(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean validBusinessNo(String value) {
        return value != null && value.matches("XH[A-Z0-9]{10}");
    }

    private boolean validLegacyOrderNo(String value) {
        return value != null && value.matches("XH-[A-Z2-9]{24}");
    }

    private String generateBusinessNo() {
        for (int attempt = 0; attempt < BUSINESS_NO_ATTEMPTS; attempt++) {
            StringBuilder builder = new StringBuilder("XH");
            for (int i = 0; i < BUSINESS_NO_LENGTH; i++) {
                builder.append(ALPHABET[RNG.nextInt(ALPHABET.length)]);
            }
            String businessNo = builder.toString();
            if (!repo.findByBusinessNo(businessNo).isEmpty()
                    || !repo.findByOrderNo(businessNo).isEmpty()) {
                continue;
            }
            return businessNo;
        }
        throw new IllegalStateException("业务编号生成失败");
    }

    private String trimNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
