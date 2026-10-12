package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.PublicQuoteRequest;
import com.xinghuo.dto.QuoteLeadDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.QuoteRepository;
import jakarta.persistence.criteria.Expression;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuoteService {
    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] ALPH = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int BUSINESS_NO_LENGTH = 10;
    private static final int BUSINESS_NO_ATTEMPTS = 8;
    private static final int MAX_LEAD_PAGE_SIZE = 100;

    private final QuoteRepository repo;
    private final UploadService uploads;
    private final AuditService audit;
    private final OrderService orders;

    public QuoteService(QuoteRepository repo, UploadService uploads, AuditService audit, OrderService orders) {
        this.repo = repo;
        this.uploads = uploads;
        this.audit = audit;
        this.orders = orders;
    }

    @Transactional
    public ApiDtos.QuoteResponse create(PublicQuoteRequest r, String key, String ipHash, Long customerAccountId) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new BadRequestException("Idempotency-Key 必填且长度不超过100");
        }
        validateContact(r);

        Optional<Quote> old = repo.findByIdempotencyKey(key);
        if (old.isPresent()) {
            Quote q = old.get();
            ensureBusinessNo(q);
            return quoteResponse(q);
        }

        Instant now = Instant.now();
        Quote q = Quote.builder()
                .id(UUID.randomUUID())
                .businessNo(generateBusinessNo())
                .game(r.game().trim())
                .demand(r.demand().trim())
                .currentProgress(trimNull(r.currentProgress()))
                .screenshotAssetId(r.screenshotAssetId())
                .platform(trimNull(r.platform()))
                .expectedTime(trimNull(r.expectedTime()))
                .contactMethod(trimNull(r.contactMethod()))
                .customerContact(trimNull(r.customerContact()))
                .customerAccountId(customerAccountId)
                .firstTouchSource(trimNull(r.firstTouchSource()))
                .utmSource(trimNull(r.utmSource()))
                .utmMedium(trimNull(r.utmMedium()))
                .utmCampaign(trimNull(r.utmCampaign()))
                .referrer(trimNull(r.referrer()))
                .status(Quote.Status.NEW)
                .idempotencyKey(key.trim())
                .customerIpHash(ipHash)
                .createdAt(now)
                .updatedAt(now)
                .build();
        repo.save(q);

        if (r.screenshotAssetId() != null) {
            uploads.bind(r.screenshotAssetId(), "QUOTE", q.getId().toString());
        }
        return quoteResponse(q);
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.QuoteAdminResponse> list() {
        return repo.findTop100ByOrderByCreatedAtDesc().stream().map(this::admin).toList();
    }

    @Transactional(readOnly = true)
    public QuoteLeadDtos.QuoteLeadPageResponse listLeads(
            String keyword,
            String status,
            String intent,
            String source,
            int page,
            int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(MAX_LEAD_PAGE_SIZE, size));
        Quote.Status statusFilter = parseQuoteStatus(status);
        String intentFilter = blankToNull(intent);
        String sourceFilter = blankToNull(source);
        String kw = blankToNull(keyword);

        Specification<Quote> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (statusFilter != null) {
                predicates.add(cb.equal(root.get("status"), statusFilter));
            }
            if (intentFilter != null) {
                predicates.add(cb.equal(root.get("leadIntentLevel"), intentFilter));
            }
            if (sourceFilter != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("utmSource"), sourceFilter),
                        cb.equal(root.get("firstTouchSource"), sourceFilter)));
            }
            if (kw != null) {
                String like = "%" + kw.toLowerCase(Locale.ROOT) + "%";
                Expression<String> businessNo = root.get("businessNo");
                Expression<String> game = root.get("game");
                Expression<String> demand = root.get("demand");
                Expression<String> customerContact = root.get("customerContact");
                predicates.add(cb.or(
                        cb.like(cb.lower(businessNo), like),
                        cb.like(cb.lower(game), like),
                        cb.like(cb.lower(demand), like),
                        cb.like(cb.lower(cb.coalesce(customerContact, "")), like)));
            }
            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Quote> result = repo.findAll(
                spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<QuoteLeadDtos.QuoteLeadResponse> items = result.getContent().stream()
                .map(this::leadAdmin)
                .toList();
        return new QuoteLeadDtos.QuoteLeadPageResponse(
                items,
                safePage,
                safeSize,
                result.getTotalElements(),
                result.getTotalPages(),
                repo.distinctUtmSources());
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

    @Transactional
    public ApiDtos.QuoteAdminResponse updateStatus(
            UUID id,
            ApiDtos.QuoteStatusUpdate r,
            String operator,
            HttpServletRequest req) {
        Quote q = repo.findById(id).orElseThrow(NotFoundException::new);
        ApiDtos.QuoteAdminResponse before = admin(q);
        Quote.Status next = parseStatus(r.status());
        if (q.getStatus() == Quote.Status.CONVERTED && next != Quote.Status.CONVERTED) {
            throw new BadRequestException("已转为订单的询价不能改回其他状态");
        }
        q.setStatus(next);
        q.setUpdatedAt(Instant.now());
        ApiDtos.QuoteAdminResponse after = admin(q);
        audit.log("QUOTE_STATUS_UPDATE", "QUOTE", q.getBusinessNo(), operator, before, after, r.reason(), req);
        return after;
    }

    @Transactional
    public QuoteLeadDtos.QuoteLeadResponse updateCrm(
            UUID id,
            QuoteLeadDtos.QuoteCrmUpdateRequest r,
            String operator,
            HttpServletRequest req) {
        Quote q = repo.findById(id).orElseThrow(NotFoundException::new);
        QuoteLeadDtos.QuoteLeadResponse before = leadAdmin(q);
        q.setLeadScore(r.leadScore());
        q.setLeadIntentLevel(trimNull(r.leadIntentLevel()));
        q.setNextAction(trimNull(r.nextAction()));
        q.setFollowUpNote(trimNull(r.followUpNote()));
        q.setLastContactedAt(Instant.now());
        q.setUpdatedAt(Instant.now());
        QuoteLeadDtos.QuoteLeadResponse after = leadAdmin(q);
        audit.log(
                "QUOTE_CRM_UPDATE",
                "QUOTE",
                q.getBusinessNo(),
                operator,
                before,
                after,
                r.reason() == null || r.reason().isBlank() ? "更新线索跟进信息" : r.reason(),
                req);
        return after;
    }

    @Transactional
    public QuoteLeadDtos.QuoteAiSuggestionResponse generateLeadSuggestion(
            UUID id,
            String operator,
            HttpServletRequest req) {
        Quote q = repo.findById(id).orElseThrow(NotFoundException::new);
        QuoteLeadDtos.QuoteLeadResponse before = leadAdmin(q);
        int score = scoreLead(q);
        String level = score >= 75 ? "HIGH" : score >= 45 ? "MEDIUM" : "LOW";
        String next = switch (level) {
            case "HIGH" -> "优先联系，确认预算、交付时间和付款方式，并在当天给出明确报价。";
            case "MEDIUM" -> "先补齐需求边界，确认平台、当前进度和可接受交付周期后再报价。";
            default -> "先用低压沟通确认真实需求和联系方式，避免投入过多人工时间。";
        };
        String reply = buildReply(q, level, next);
        String reason = buildScoreReason(q, score, level);
        q.setLeadScore(score);
        q.setLeadIntentLevel(level);
        q.setNextAction(next);
        q.setAiSuggestion(reply + "\n\n判断依据：" + reason);
        q.setUpdatedAt(Instant.now());
        audit.log(
                "QUOTE_AI_SUGGESTION",
                "QUOTE",
                q.getBusinessNo(),
                operator,
                before,
                leadAdmin(q),
                "生成AI线索建议",
                req);
        return new QuoteLeadDtos.QuoteAiSuggestionResponse(score, level, next, reply, reason);
    }

    @Transactional
    public ApiDtos.AdminOrderResponse convertToOrder(
            UUID id,
            ApiDtos.QuoteConvertRequest r,
            String operator,
            HttpServletRequest req) {
        Quote q = repo.findById(id).orElseThrow(NotFoundException::new);
        if (q.getStatus() == Quote.Status.CLOSED || q.getStatus() == Quote.Status.CANCELLED) {
            throw new BadRequestException("已关闭或已取消的询价不能转为订单");
        }
        ensureBusinessNo(q);
        ApiDtos.QuoteAdminResponse before = admin(q);
        ApiDtos.AdminOrderResponse order = orders.createFromQuote(q, operator, req, r.reason());
        if (q.getStatus() != Quote.Status.CONVERTED) {
            q.setStatus(Quote.Status.CONVERTED);
            q.setUpdatedAt(Instant.now());
            audit.log(
                    "QUOTE_CONVERTED",
                    "QUOTE",
                    q.getBusinessNo(),
                    operator,
                    before,
                    admin(q),
                    r.reason() == null || r.reason().isBlank() ? "询价转订单" : r.reason(),
                    req);
        }
        return order;
    }

    @Transactional(readOnly = true)
    public Quote find(UUID id) {
        return repo.findById(id).orElseThrow(NotFoundException::new);
    }

    private String customerProgress(Quote q) {
        return (q.getCurrentProgress() == null ? "" : q.getCurrentProgress())
                + (q.getCustomerSupplement() == null ? "" : "\n客户补充：" + q.getCustomerSupplement());
    }

    private ApiDtos.QuoteResponse quoteResponse(Quote q) {
        return new ApiDtos.QuoteResponse(
                q.getId(),
                q.getBusinessNo(),
                q.getStatus().name(),
                q.getCreatedAt());
    }

    private ApiDtos.QuoteAdminResponse admin(Quote q) {
        return new ApiDtos.QuoteAdminResponse(
                q.getId(),
                q.getBusinessNo(),
                q.getGame(),
                q.getDemand(),
                customerProgress(q),
                q.getScreenshotAssetId(),
                q.getPlatform(),
                q.getExpectedTime(),
                q.getContactMethod(),
                q.getCustomerContact(),
                q.getCustomerIpHash(),
                q.getStatus().name(),
                q.getCreatedAt());
    }

    private QuoteLeadDtos.QuoteLeadResponse leadAdmin(Quote q) {
        return new QuoteLeadDtos.QuoteLeadResponse(
                q.getId(),
                q.getBusinessNo(),
                q.getGame(),
                q.getDemand(),
                customerProgress(q),
                q.getScreenshotAssetId(),
                q.getPlatform(),
                q.getExpectedTime(),
                q.getContactMethod(),
                q.getCustomerContact(),
                q.getCustomerIpHash(),
                q.getStatus().name(),
                q.getCreatedAt(),
                q.getLeadScore(),
                q.getLeadIntentLevel(),
                q.getNextAction(),
                q.getFollowUpNote(),
                q.getAiSuggestion(),
                q.getLastContactedAt(),
                q.getFirstTouchSource(),
                q.getUtmSource(),
                q.getUtmMedium(),
                q.getUtmCampaign(),
                q.getReferrer(),
                q.getUpdatedAt(),
                q.getCustomerSupplement());
    }

    @Transactional
    public void delete(UUID id, String operator, HttpServletRequest req) {
        Quote q = repo.findById(id).orElseThrow(NotFoundException::new);
        ApiDtos.QuoteAdminResponse before = admin(q);
        repo.delete(q);
        audit.log("QUOTE_DELETE", "QUOTE", q.getBusinessNo(), operator, before, null, "后台删除询价线索", req);
    }

    private void ensureBusinessNo(Quote q) {
        if (q.getBusinessNo() == null || q.getBusinessNo().isBlank()) {
            q.setBusinessNo(generateBusinessNo());
            q.setUpdatedAt(Instant.now());
        }
    }

    private String generateBusinessNo() {
        for (int attempt = 0; attempt < BUSINESS_NO_ATTEMPTS; attempt++) {
            StringBuilder value = new StringBuilder("XH");
            for (int i = 0; i < BUSINESS_NO_LENGTH; i++) {
                value.append(ALPH[RNG.nextInt(ALPH.length)]);
            }
            String businessNo = value.toString();
            if (repo.findByBusinessNo(businessNo).isEmpty()) {
                return businessNo;
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

    private void validateContact(PublicQuoteRequest r) {
        String method = trimNull(r.contactMethod());
        String contact = trimNull(r.customerContact());
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

    private int scoreLead(Quote q) {
        int score = 20;
        if (q.getCustomerContact() != null && !q.getCustomerContact().isBlank()) {
            score += 20;
        }
        if (q.getDemand() != null && q.getDemand().length() >= 12) {
            score += 15;
        }
        if (q.getCurrentProgress() != null && !q.getCurrentProgress().isBlank()) {
            score += 10;
        }
        if (q.getPlatform() != null && !q.getPlatform().isBlank()) {
            score += 8;
        }
        if (q.getExpectedTime() != null && !q.getExpectedTime().isBlank()) {
            score += 12;
        }
        if (q.getScreenshotAssetId() != null) {
            score += 10;
        }
        if (q.getFirstTouchSource() != null || q.getUtmSource() != null) {
            score += 5;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String buildReply(Quote q, String level, String next) {
        String demand = q.getDemand() == null ? "你的需求" : q.getDemand();
        String time = q.getExpectedTime() == null ? "交付时间" : q.getExpectedTime();
        String platform = q.getPlatform() == null ? "游戏平台" : q.getPlatform();
        String prefix = "HIGH".equals(level)
                ? "这个需求信息比较完整，我可以优先帮你确认。"
                : "我先帮你把需求边界确认清楚。";
        return prefix
                + "目前看到的需求是「" + demand
                + "」，平台为「" + platform
                + "」，期望时间为「" + time
                + "」。" + next;
    }

    private String buildScoreReason(Quote q, int score, String level) {
        List<String> reasons = new ArrayList<>();
        if (q.getCustomerContact() != null && !q.getCustomerContact().isBlank()) {
            reasons.add("已留下联系方式");
        }
        if (q.getExpectedTime() != null && !q.getExpectedTime().isBlank()) {
            reasons.add("有期望时间");
        }
        if (q.getScreenshotAssetId() != null) {
            reasons.add("已上传截图");
        }
        if (q.getCurrentProgress() != null && !q.getCurrentProgress().isBlank()) {
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
}
