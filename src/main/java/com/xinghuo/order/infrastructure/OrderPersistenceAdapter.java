package com.xinghuo.order.infrastructure;

import com.xinghuo.audit.infrastructure.AuditService;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.order.application.AdminOrderView;
import com.xinghuo.order.application.CreateOrderCommand;
import com.xinghuo.order.application.OrderFromQuoteCommand;
import com.xinghuo.order.application.OrderPort;
import com.xinghuo.order.application.OrderPublicView;
import com.xinghuo.order.application.PublicCaseSettingsView;
import com.xinghuo.order.application.PublicCaseView;
import com.xinghuo.order.application.UpdateOrderCommand;
import com.xinghuo.order.domain.OrderProgressAsset;
import com.xinghuo.repository.CustomerOrderRepository;
import com.xinghuo.upload.infrastructure.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Order-owned persistence and integration adapter. */
@Component
public class OrderPersistenceAdapter implements OrderPort {
    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] ALPH = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final CustomerOrderRepository repo;
    private final UploadService uploads;
    private final AuditService audit;

    public OrderPersistenceAdapter(
            CustomerOrderRepository repo, UploadService uploads, AuditService audit) {
        this.repo = repo;
        this.uploads = uploads;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicCaseView> publicCases() {
        ArrayList<PublicCaseView> out = new ArrayList<>();
        for (CustomerOrder order : repo.findByAllowPublicCaseTrueOrderByFinishedAtDescUpdatedAtDesc()) {
            out.add(new PublicCaseView(
                    maskBusinessNo(order.getBusinessNo()),
                    order.getPublicSummary(),
                    order.getFinishedAt(),
                    "completed"));
            if (out.size() >= 12) {
                break;
            }
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderPublicView getPublic(String publicNo) {
        return publicView(findByPublicNo(publicNo));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderProgressAsset progressAsset(String publicNo) {
        CustomerOrder order = findByPublicNo(publicNo);
        return new OrderProgressAsset(order.getProgressImageAssetId(), order.getOrderNo());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminOrderView> list() {
        return repo.findTop100ByOrderByUpdatedAtDesc().stream().map(this::admin).toList();
    }

    @Override
    @Transactional
    public AdminOrderView create(CreateOrderCommand command, String operator) {
        String businessNo = generateBusinessNo();
        CustomerOrder order = newOrder(
                businessNo,
                null,
                null,
                parseStatus(command.status()),
                command.progressPercent(),
                command.progressText(),
                command.customerNote(),
                command.internalNote());
        repo.save(order);
        AdminOrderView after = admin(order);
        audit.log(
                "ORDER_CREATE",
                "ORDER",
                order.getBusinessNo(),
                operator,
                null,
                after,
                command.reason(),
                currentRequest());
        return after;
    }

    @Override
    @Transactional
    public AdminOrderView createFromQuote(OrderFromQuoteCommand command, String operator) {
        Optional<CustomerOrder> existing = repo.findBySourceQuoteId(command.quoteId());
        if (existing.isPresent()) {
            return admin(existing.get());
        }
        String businessNo = command.businessNo();
        if (businessNo == null || businessNo.isBlank()) {
            businessNo = generateBusinessNo();
        }
        String internal = "由询价自动转单：业务编号=" + businessNo
                + "；游戏=" + command.game()
                + "；需求=" + command.demand()
                + (command.currentProgress() == null ? "" : "；当前进度=" + command.currentProgress())
                + (command.platform() == null ? "" : "；平台=" + command.platform())
                + (command.expectedTime() == null ? "" : "；期望时间=" + command.expectedTime());
        CustomerOrder order = newOrder(
                businessNo,
                command.quoteId(),
                command.customerAccountId(),
                CustomerOrder.Status.WAITING_CONFIRMATION,
                null,
                "已收到需求，等待客服确认报价与排期",
                null,
                internal);
        repo.save(order);
        AdminOrderView after = admin(order);
        audit.log(
                "ORDER_CREATE_FROM_QUOTE",
                "ORDER",
                order.getBusinessNo(),
                operator,
                null,
                after,
                blankToDefault(command.reason(), "询价转订单"),
                currentRequest());
        return after;
    }

    @Override
    @Transactional
    public AdminOrderView update(String orderNo, UpdateOrderCommand command, String operator) {
        CustomerOrder order = findByOrderNo(orderNo);
        if (command.version() == null || command.version() != order.getVersion()) {
            throw new ConflictException("订单已被更新，请刷新后重新编辑；当前内容尚未保存");
        }
        AdminOrderView before = admin(order);
        CustomerOrder.Status nextStatus = parseStatus(command.status());
        order.setStatus(nextStatus);
        order.setProgressPercent(
                nextStatus == CustomerOrder.Status.COMPLETED && command.progressPercent() == null
                        ? 100
                        : command.progressPercent());
        order.setProgressText(command.progressText().trim());
        order.setCustomerNote(trimNull(command.customerNote()));
        order.setInternalNote(trimNull(command.internalNote()));
        if (nextStatus == CustomerOrder.Status.COMPLETED && order.getFinishedAt() == null) {
            order.setFinishedAt(Instant.now());
        }
        order.setUpdatedAt(Instant.now());
        repo.flush();
        AdminOrderView after = admin(order);
        audit.log(
                "ORDER_UPDATE",
                "ORDER",
                order.getBusinessNo(),
                operator,
                before,
                after,
                command.reason(),
                currentRequest());
        return after;
    }

    @Override
    @Transactional
    public AdminOrderView attachImage(String orderNo, UUID assetId, String operator) {
        CustomerOrder order = findByOrderNo(orderNo);
        AdminOrderView before = admin(order);
        uploads.bind(assetId, "ORDER", order.getOrderNo());
        order.setProgressImageAssetId(assetId);
        order.setUpdatedAt(Instant.now());
        AdminOrderView after = admin(order);
        audit.log(
                "ORDER_IMAGE_UPDATE",
                "ORDER",
                order.getBusinessNo(),
                operator,
                before,
                after,
                "更新进度截图",
                currentRequest());
        return after;
    }

    @Override
    @Transactional
    public PublicCaseSettingsView setPublicCase(
            String orderNo, boolean enabled, String summary, String operator) {
        CustomerOrder order = repo.findByOrderNo(orderNo).orElseThrow(NotFoundException::new);
        AdminOrderView before = admin(order);
        order.setAllowPublicCase(enabled);
        order.setPublicEnabledAt(enabled ? Instant.now() : null);
        if (summary != null && !summary.isBlank()) {
            String normalized = summary.trim();
            if (normalized.length() > 240) {
                throw new BadRequestException("案例摘要过长");
            }
            order.setPublicSummary(normalized);
        }
        if (enabled && order.getFinishedAt() == null && order.getStatus() == CustomerOrder.Status.COMPLETED) {
            order.setFinishedAt(Instant.now());
        }
        order.setUpdatedAt(Instant.now());
        repo.save(order);
        PublicCaseSettingsView after = new PublicCaseSettingsView(
                order.getOrderNo(), order.isAllowPublicCase(), order.getPublicSummary());
        audit.log(
                "ORDER_PUBLIC_CASE_UPDATE",
                "ORDER",
                order.getBusinessNo(),
                operator,
                before,
                admin(order),
                enabled ? "公开案例" : "取消公开案例",
                currentRequest());
        return after;
    }

    @Override
    @Transactional
    public void delete(String orderNo, String operator) {
        CustomerOrder order = findByOrderNo(orderNo);
        AdminOrderView before = admin(order);
        repo.delete(order);
        audit.log(
                "ORDER_DELETE",
                "ORDER",
                order.getBusinessNo(),
                operator,
                before,
                null,
                "后台删除履约订单",
                currentRequest());
    }

    private CustomerOrder findByPublicNo(String publicNo) {
        String normalized = normalizeNo(publicNo);
        if (validBusinessNo(normalized)) {
            return repo.findByBusinessNo(normalized).orElseThrow(NotFoundException::new);
        }
        if (validLegacyOrderNo(normalized)) {
            return repo.findByOrderNo(normalized).orElseThrow(NotFoundException::new);
        }
        throw new NotFoundException();
    }

    private CustomerOrder findByOrderNo(String orderNo) {
        String normalized = normalizeNo(orderNo);
        if (!validBusinessNo(normalized) && !validLegacyOrderNo(normalized)) {
            throw new NotFoundException();
        }
        return repo.findByOrderNo(normalized).orElseThrow(NotFoundException::new);
    }

    private OrderPublicView publicView(CustomerOrder order) {
        return new OrderPublicView(
                order.getBusinessNo() == null ? order.getOrderNo() : order.getBusinessNo(),
                order.getStatus().name(),
                order.getProgressPercent(),
                order.getProgressText(),
                order.getUpdatedAt(),
                order.getProgressImageAssetId() != null,
                order.getCustomerNote());
    }

    private AdminOrderView admin(CustomerOrder order) {
        return new AdminOrderView(
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

    private CustomerOrder.Status parseStatus(String status) {
        try {
            return CustomerOrder.Status.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
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
        for (int attempt = 0; attempt < 8; attempt++) {
            StringBuilder builder = new StringBuilder("XH");
            for (int i = 0; i < 10; i++) {
                builder.append(ALPH[RNG.nextInt(ALPH.length)]);
            }
            String businessNo = builder.toString();
            if (repo.findByBusinessNo(businessNo).isEmpty() && repo.findByOrderNo(businessNo).isEmpty()) {
                return businessNo;
            }
        }
        throw new IllegalStateException("业务编号生成失败");
    }

    private String maskBusinessNo(String no) {
        if (no == null || no.length() <= 4) {
            return "XH****";
        }
        return no.substring(0, 2) + "****" + no.substring(no.length() - 2);
    }

    private String trimNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest();
        }
        return null;
    }
}
