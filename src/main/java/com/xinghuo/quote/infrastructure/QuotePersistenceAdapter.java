package com.xinghuo.quote.infrastructure;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.Quote;
import com.xinghuo.quote.application.ConvertedOrderView;
import com.xinghuo.quote.application.QuoteAdminView;
import com.xinghuo.quote.application.QuoteAiSuggestionView;
import com.xinghuo.quote.application.QuoteConvertCommand;
import com.xinghuo.quote.application.QuoteCrmUpdateCommand;
import com.xinghuo.quote.application.QuoteLeadPageView;
import com.xinghuo.quote.application.QuoteLeadView;
import com.xinghuo.quote.application.QuotePort;
import com.xinghuo.quote.application.QuoteStatusUpdateCommand;
import com.xinghuo.quote.application.QuoteSubmissionCommand;
import com.xinghuo.quote.application.QuoteSubmissionResult;
import com.xinghuo.repository.QuoteRepository;
import com.xinghuo.service.AuditService;
import com.xinghuo.service.OrderService;
import com.xinghuo.service.UploadService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Quote-owned persistence and integration adapter. */
@Component
public class QuotePersistenceAdapter implements QuotePort {
    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] ALPH = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final QuoteRepository repo;
    private final UploadService uploads;
    private final AuditService audit;
    private final OrderService orders;

    public QuotePersistenceAdapter(
            QuoteRepository repo, UploadService uploads, AuditService audit, OrderService orders) {
        this.repo = repo;
        this.uploads = uploads;
        this.audit = audit;
        this.orders = orders;
    }

    @Override
    @Transactional
    public QuoteSubmissionResult create(QuoteSubmissionCommand command) {
        String key = command.idempotencyKey();
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new BadRequestException("Idempotency-Key 必填且长度不超过100");
        }
        validateContact(command.contactMethod(), command.customerContact());

        Optional<Quote> old = repo.findByIdempotencyKey(key);
        if (old.isPresent()) {
            Quote quote = old.get();
            ensureBusinessNo(quote);
            return submissionResult(quote);
        }

        Instant now = Instant.now();
        Quote quote = Quote.builder()
                .id(UUID.randomUUID())
                .businessNo(generateBusinessNo())
                .game(command.game().trim())
                .demand(command.demand().trim())
                .currentProgress(trimNull(command.currentProgress()))
                .screenshotAssetId(command.screenshotAssetId())
                .platform(trimNull(command.platform()))
                .expectedTime(trimNull(command.expectedTime()))
                .contactMethod(trimNull(command.contactMethod()))
                .customerContact(trimNull(command.customerContact()))
                .customerAccountId(command.customerAccountId())
                .firstTouchSource(trimNull(command.firstTouchSource()))
                .utmSource(trimNull(command.utmSource()))
                .utmMedium(trimNull(command.utmMedium()))
                .utmCampaign(trimNull(command.utmCampaign()))
                .referrer(trimNull(command.referrer()))
                .status(Quote.Status.NEW)
                .idempotencyKey(key.trim())
                .customerIpHash(command.customerIpHash())
                .createdAt(now)
                .updatedAt(now)
                .build();
        repo.save(quote);
        if (command.screenshotAssetId() != null) {
            uploads.bind(command.screenshotAssetId(), "QUOTE", quote.getId().toString());
        }
        return submissionResult(quote);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteAdminView> list() {
        return repo.findTop100ByOrderByCreatedAtDesc().stream().map(this::admin).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuoteLeadPageView listLeads(
            String keyword, String status, String intent, String source, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(100, size));
        Quote.Status statusFilter = parseQuoteStatus(status);
        String intentFilter = blankToNull(intent);
        String sourceFilter = blankToNull(source);
        String kw = blankToNull(keyword);

        Specification<Quote> spec = (root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();
            if (statusFilter != null) {
                predicates.add(cb.equal(root.<Quote.Status>get("status"), statusFilter));
            }
            if (intentFilter != null) {
                predicates.add(cb.equal(root.<String>get("leadIntentLevel"), intentFilter));
            }
            if (sourceFilter != null) {
                predicates.add(cb.or(
                        cb.equal(root.<String>get("utmSource"), sourceFilter),
                        cb.equal(root.<String>get("firstTouchSource"), sourceFilter)));
            }
            if (kw != null) {
                String like = "%" + kw.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("businessNo")), like),
                        cb.like(cb.lower(root.<String>get("game")), like),
                        cb.like(cb.lower(root.<String>get("demand")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("customerContact"), "")), like)));
            }
            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Quote> result = repo.findAll(
                spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<QuoteLeadView> items = result.getContent().stream().map(this::leadAdmin).toList();
        return new QuoteLeadPageView(
                items,
                safePage,
                safeSize,
                result.getTotalElements(),
                result.getTotalPages(),
                repo.distinctUtmSources());
    }

    @Override
    @Transactional
    public QuoteAdminView updateStatus(
            UUID id, QuoteStatusUpdateCommand command, String operator) {
        Quote quote = repo.findById(id).orElseThrow(NotFoundException::new);
        QuoteAdminView before = admin(quote);
        Quote.Status next = parseStatus(command.status());
        if (quote.getStatus() == Quote.Status.CONVERTED && next != Quote.Status.CONVERTED) {
            throw new BadRequestException("已转为订单的询价不能改回其他状态");
        }
        quote.setStatus(next);
        quote.setUpdatedAt(Instant.now());
        QuoteAdminView after = admin(quote);
        audit.log(
                "QUOTE_STATUS_UPDATE",
                "QUOTE",
                quote.getBusinessNo(),
                operator,
                before,
                after,
                command.reason(),
                currentRequest());
        return after;
    }

    @Override
    @Transactional
    public QuoteLeadView updateCrm(UUID id, QuoteCrmUpdateCommand command, String operator) {
        Quote quote = repo.findById(id).orElseThrow(NotFoundException::new);
        QuoteLeadView before = leadAdmin(quote);
        quote.setLeadScore(command.leadScore());
        quote.setLeadIntentLevel(trimNull(command.leadIntentLevel()));
        quote.setNextAction(trimNull(command.nextAction()));
        quote.setFollowUpNote(trimNull(command.followUpNote()));
        quote.setLastContactedAt(Instant.now());
        quote.setUpdatedAt(Instant.now());
        QuoteLeadView after = leadAdmin(quote);
        audit.log(
                "QUOTE_CRM_UPDATE",
                "QUOTE",
                quote.getBusinessNo(),
                operator,
                before,
                after,
                command.reason() == null || command.reason().isBlank()
                        ? "更新线索跟进信息"
                        : command.reason(),
                currentRequest());
        return after;
    }

    @Override
    @Transactional
    public QuoteAiSuggestionView generateLeadSuggestion(UUID id, String operator) {
        Quote quote = repo.findById(id).orElseThrow(NotFoundException::new);
        QuoteLeadView before = leadAdmin(quote);
        int score = scoreLead(quote);
        String level = score >= 75 ? "HIGH" : (score >= 45 ? "MEDIUM" : "LOW");
        String next = switch (level) {
            case "HIGH" -> "优先联系，确认预算、交付时间和付款方式，并在当天给出明确报价。";
            case "MEDIUM" -> "先补齐需求边界，确认平台、当前进度和可接受交付周期后再报价。";
            default -> "先用低压沟通确认真实需求和联系方式，避免投入过多人工时间。";
        };
        String reply = buildReply(quote, level, next);
        String reason = buildScoreReason(quote, score, level);
        quote.setLeadScore(score);
        quote.setLeadIntentLevel(level);
        quote.setNextAction(next);
        quote.setAiSuggestion(reply + "\n\n判断依据：" + reason);
        quote.setUpdatedAt(Instant.now());
        audit.log(
                "QUOTE_AI_SUGGESTION",
                "QUOTE",
                quote.getBusinessNo(),
                operator,
                before,
                leadAdmin(quote),
                "生成AI线索建议",
                currentRequest());
        return new QuoteAiSuggestionView(score, level, next, reply, reason);
    }

    @Override
    @Transactional
    public ConvertedOrderView convertToOrder(UUID id, QuoteConvertCommand command, String operator) {
        Quote quote = repo.findById(id).orElseThrow(NotFoundException::new);
        if (quote.getStatus() == Quote.Status.CLOSED || quote.getStatus() == Quote.Status.CANCELLED) {
            throw new BadRequestException("已关闭或已取消的询价不能转为订单");
        }
        ensureBusinessNo(quote);
        QuoteAdminView before = admin(quote);
        var order = orders.createFromQuote(quote, operator, currentRequest(), command.reason());
        if (quote.getStatus() != Quote.Status.CONVERTED) {
            quote.setStatus(Quote.Status.CONVERTED);
            quote.setUpdatedAt(Instant.now());
            audit.log(
                    "QUOTE_CONVERTED",
                    "QUOTE",
                    quote.getBusinessNo(),
                    operator,
                    before,
                    admin(quote),
                    command.reason() == null || command.reason().isBlank()
                            ? "询价转订单"
                            : command.reason(),
                    currentRequest());
        }
        return new ConvertedOrderView(
                order.id(),
                order.orderNo(),
                order.businessNo(),
                order.sourceQuoteId(),
                order.status(),
                order.progressPercent(),
                order.progressText(),
                order.hasProgressImage(),
                order.customerNote(),
                order.internalNote(),
                order.updatedAt(),
                order.version());
    }

    @Override
    @Transactional(readOnly = true)
    public QuoteAdminView find(UUID id) {
        return admin(repo.findById(id).orElseThrow(NotFoundException::new));
    }

    @Override
    @Transactional
    public void delete(UUID id, String operator) {
        Quote quote = repo.findById(id).orElseThrow(NotFoundException::new);
        QuoteAdminView before = admin(quote);
        repo.delete(quote);
        audit.log(
                "QUOTE_DELETE",
                "QUOTE",
                quote.getBusinessNo(),
                operator,
                before,
                null,
                "后台删除询价线索",
                currentRequest());
    }

    private QuoteSubmissionResult submissionResult(Quote quote) {
        return new QuoteSubmissionResult(
                quote.getId(), quote.getBusinessNo(), quote.getStatus().name(), quote.getCreatedAt());
    }

    private Quote.Status parseQuoteStatus(String raw) {
        String value = blankToNull(raw);
        if (value == null) {
            return null;
        }
        try {
            return Quote.Status.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("非法线索状态筛选值");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String customerProgress(Quote quote) {
        return (quote.getCurrentProgress() == null ? "" : quote.getCurrentProgress())
                + (quote.getCustomerSupplement() == null
                        ? ""
                        : "\n客户补充：" + quote.getCustomerSupplement());
    }

    private QuoteAdminView admin(Quote quote) {
        return new QuoteAdminView(
                quote.getId(),
                quote.getBusinessNo(),
                quote.getGame(),
                quote.getDemand(),
                customerProgress(quote),
                quote.getScreenshotAssetId(),
                quote.getPlatform(),
                quote.getExpectedTime(),
                quote.getContactMethod(),
                quote.getCustomerContact(),
                quote.getCustomerIpHash(),
                quote.getStatus().name(),
                quote.getCreatedAt());
    }

    private QuoteLeadView leadAdmin(Quote quote) {
        return new QuoteLeadView(
                quote.getId(),
                quote.getBusinessNo(),
                quote.getGame(),
                quote.getDemand(),
                customerProgress(quote),
                quote.getScreenshotAssetId(),
                quote.getPlatform(),
                quote.getExpectedTime(),
                quote.getContactMethod(),
                quote.getCustomerContact(),
                quote.getCustomerIpHash(),
                quote.getStatus().name(),
                quote.getCreatedAt(),
                quote.getLeadScore(),
                quote.getLeadIntentLevel(),
                quote.getNextAction(),
                quote.getFollowUpNote(),
                quote.getAiSuggestion(),
                quote.getLastContactedAt(),
                quote.getFirstTouchSource(),
                quote.getUtmSource(),
                quote.getUtmMedium(),
                quote.getUtmCampaign(),
                quote.getReferrer(),
                quote.getUpdatedAt(),
                quote.getCustomerSupplement());
    }

    private void ensureBusinessNo(Quote quote) {
        if (quote.getBusinessNo() == null || quote.getBusinessNo().isBlank()) {
            quote.setBusinessNo(generateBusinessNo());
            quote.setUpdatedAt(Instant.now());
        }
    }

    private String generateBusinessNo() {
        for (int attempt = 0; attempt < 8; attempt++) {
            StringBuilder builder = new StringBuilder("XH");
            for (int i = 0; i < 10; i++) {
                builder.append(ALPH[RNG.nextInt(ALPH.length)]);
            }
            if (repo.findByBusinessNo(builder.toString()).isEmpty()) {
                return builder.toString();
            }
        }
        throw new IllegalStateException("业务编号生成失败");
    }

    private Quote.Status parseStatus(String value) {
        try {
            return Quote.Status.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("非法询价状态");
        }
    }

    private void validateContact(String rawMethod, String rawContact) {
        String method = trimNull(rawMethod);
        String contact = trimNull(rawContact);
        if (method == null || method.isBlank() || contact == null) {
            throw new BadRequestException("请填写联系方式和联系号");
        }
        String normalized = contact.replaceAll("[\\s()-]", "");
        if (normalized.matches("1[3-9]\\d{9}")) {
            normalized = "+86" + normalized;
        } else if (normalized.startsWith("00")) {
            normalized = "+" + normalized.substring(2);
        }
        boolean phoneMethod = method.contains("手机")
                || method.equalsIgnoreCase("phone")
                || method.equalsIgnoreCase("mobile");
        if (phoneMethod && !normalized.matches("\\+[1-9]\\d{7,14}")) {
            throw new BadRequestException("请输入正确的手机号和国家区号");
        }
    }

    private int scoreLead(Quote quote) {
        int score = 20;
        if (quote.getCustomerContact() != null && !quote.getCustomerContact().isBlank()) {
            score += 20;
        }
        if (quote.getDemand() != null && quote.getDemand().length() >= 12) {
            score += 15;
        }
        if (quote.getCurrentProgress() != null && !quote.getCurrentProgress().isBlank()) {
            score += 10;
        }
        if (quote.getPlatform() != null && !quote.getPlatform().isBlank()) {
            score += 8;
        }
        if (quote.getExpectedTime() != null && !quote.getExpectedTime().isBlank()) {
            score += 12;
        }
        if (quote.getScreenshotAssetId() != null) {
            score += 10;
        }
        if (quote.getFirstTouchSource() != null || quote.getUtmSource() != null) {
            score += 5;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String buildReply(Quote quote, String level, String next) {
        String demand = quote.getDemand() == null ? "你的需求" : quote.getDemand();
        String time = quote.getExpectedTime() == null ? "交付时间" : quote.getExpectedTime();
        String platform = quote.getPlatform() == null ? "游戏平台" : quote.getPlatform();
        String prefix = "HIGH".equals(level)
                ? "这个需求信息比较完整，我可以优先帮你确认。"
                : "我先帮你把需求边界确认清楚。";
        return prefix
                + "目前看到的需求是「"
                + demand
                + "」，平台为「"
                + platform
                + "」，期望时间为「"
                + time
                + "」。"
                + next;
    }

    private String buildScoreReason(Quote quote, int score, String level) {
        ArrayList<String> reasons = new ArrayList<>();
        if (quote.getCustomerContact() != null && !quote.getCustomerContact().isBlank()) {
            reasons.add("已留下联系方式");
        }
        if (quote.getExpectedTime() != null && !quote.getExpectedTime().isBlank()) {
            reasons.add("有期望时间");
        }
        if (quote.getScreenshotAssetId() != null) {
            reasons.add("已上传截图");
        }
        if (quote.getCurrentProgress() != null && !quote.getCurrentProgress().isBlank()) {
            reasons.add("有当前进度");
        }
        if (reasons.isEmpty()) {
            reasons.add("需求信息仍需补充");
        }
        return "评分 " + score + "，意向等级 " + level + "；" + String.join("、", reasons) + "。";
    }

    private String trimNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest();
        }
        return null;
    }
}
