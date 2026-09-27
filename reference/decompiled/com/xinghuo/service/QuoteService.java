/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.dto.ApiDtos$AdminOrderResponse
 *  com.xinghuo.dto.ApiDtos$QuoteAdminResponse
 *  com.xinghuo.dto.ApiDtos$QuoteConvertRequest
 *  com.xinghuo.dto.ApiDtos$QuoteResponse
 *  com.xinghuo.dto.ApiDtos$QuoteStatusUpdate
 *  com.xinghuo.dto.PublicQuoteRequest
 *  com.xinghuo.dto.QuoteLeadDtos$QuoteAiSuggestionResponse
 *  com.xinghuo.dto.QuoteLeadDtos$QuoteCrmUpdateRequest
 *  com.xinghuo.dto.QuoteLeadDtos$QuoteLeadPageResponse
 *  com.xinghuo.dto.QuoteLeadDtos$QuoteLeadResponse
 *  com.xinghuo.exception.BadRequestException
 *  com.xinghuo.exception.NotFoundException
 *  com.xinghuo.model.Quote
 *  com.xinghuo.model.Quote$Status
 *  com.xinghuo.repository.QuoteRepository
 *  com.xinghuo.service.AuditService
 *  com.xinghuo.service.OrderService
 *  com.xinghuo.service.QuoteService
 *  com.xinghuo.service.UploadService
 *  jakarta.persistence.criteria.Expression
 *  jakarta.persistence.criteria.Predicate
 *  jakarta.servlet.http.HttpServletRequest
 *  lombok.Generated
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.jpa.domain.Specification
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.PublicQuoteRequest;
import com.xinghuo.dto.QuoteLeadDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.QuoteRepository;
import com.xinghuo.service.AuditService;
import com.xinghuo.service.OrderService;
import com.xinghuo.service.UploadService;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuoteService {
    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] ALPH = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final QuoteRepository repo;
    private final UploadService uploads;
    private final AuditService audit;
    private final OrderService orders;

    @Transactional
    public ApiDtos.QuoteResponse create(PublicQuoteRequest r, String key, String ipHash, Long customerAccountId) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new BadRequestException("Idempotency-Key \u5fc5\u586b\u4e14\u957f\u5ea6\u4e0d\u8d85\u8fc7100");
        }
        this.validateContact(r);
        Optional old = this.repo.findByIdempotencyKey(key);
        if (old.isPresent()) {
            Quote q = (Quote)old.get();
            this.ensureBusinessNo(q);
            return new ApiDtos.QuoteResponse(q.getId(), q.getBusinessNo(), q.getStatus().name(), q.getCreatedAt());
        }
        Instant now = Instant.now();
        Quote q = Quote.builder().id(UUID.randomUUID()).businessNo(this.generateBusinessNo()).game(r.game().trim()).demand(r.demand().trim()).currentProgress(this.trimNull(r.currentProgress())).screenshotAssetId(r.screenshotAssetId()).platform(this.trimNull(r.platform())).expectedTime(this.trimNull(r.expectedTime())).contactMethod(this.trimNull(r.contactMethod())).customerContact(this.trimNull(r.customerContact())).customerAccountId(customerAccountId).firstTouchSource(this.trimNull(r.firstTouchSource())).utmSource(this.trimNull(r.utmSource())).utmMedium(this.trimNull(r.utmMedium())).utmCampaign(this.trimNull(r.utmCampaign())).referrer(this.trimNull(r.referrer())).status(Quote.Status.NEW).idempotencyKey(key.trim()).customerIpHash(ipHash).createdAt(now).updatedAt(now).build();
        this.repo.save((Object)q);
        if (r.screenshotAssetId() != null) {
            this.uploads.bind(r.screenshotAssetId(), "QUOTE", q.getId().toString());
        }
        return new ApiDtos.QuoteResponse(q.getId(), q.getBusinessNo(), q.getStatus().name(), q.getCreatedAt());
    }

    @Transactional(readOnly=true)
    public List<ApiDtos.QuoteAdminResponse> list() {
        return this.repo.findTop100ByOrderByCreatedAtDesc().stream().map(arg_0 -> this.admin(arg_0)).toList();
    }

    @Transactional(readOnly=true)
    public QuoteLeadDtos.QuoteLeadPageResponse listLeads(String keyword, String status, String intent, String source, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(100, size));
        Quote.Status statusFilter = this.parseQuoteStatus(status);
        String intentFilter = this.blankToNull(intent);
        String sourceFilter = this.blankToNull(source);
        String kw = this.blankToNull(keyword);
        Specification & Serializable spec = (Specification & Serializable)(root, query, cb) -> {
            ArrayList<Predicate> ps = new ArrayList<Predicate>();
            if (statusFilter != null) {
                ps.add(cb.equal((Expression)root.get("status"), (Object)statusFilter));
            }
            if (intentFilter != null) {
                ps.add(cb.equal((Expression)root.get("leadIntentLevel"), (Object)intentFilter));
            }
            if (sourceFilter != null) {
                ps.add(cb.or((Expression)cb.equal((Expression)root.get("utmSource"), (Object)sourceFilter), (Expression)cb.equal((Expression)root.get("firstTouchSource"), (Object)sourceFilter)));
            }
            if (kw != null) {
                String like = "%" + kw.toLowerCase(Locale.ROOT) + "%";
                ps.add(cb.or(new Predicate[]{cb.like(cb.lower((Expression)root.get("businessNo")), like), cb.like(cb.lower((Expression)root.get("game")), like), cb.like(cb.lower((Expression)root.get("demand")), like), cb.like(cb.lower(cb.coalesce((Expression)root.get("customerContact"), (Object)"")), like)}));
            }
            return ps.isEmpty() ? cb.conjunction() : cb.and(ps.toArray(new Predicate[0]));
        };
        Page result = this.repo.findAll((Specification)spec, (Pageable)PageRequest.of((int)safePage, (int)safeSize, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdAt"})));
        List<QuoteLeadDtos.QuoteLeadResponse> items = result.getContent().stream().map(arg_0 -> this.leadAdmin(arg_0)).toList();
        return new QuoteLeadDtos.QuoteLeadPageResponse(items, safePage, safeSize, result.getTotalElements(), result.getTotalPages(), this.repo.distinctUtmSources());
    }

    private Quote.Status parseQuoteStatus(String raw) {
        String v = this.blankToNull(raw);
        if (v == null) {
            return null;
        }
        try {
            return Quote.Status.valueOf((String)v.toUpperCase(Locale.ROOT));
        }
        catch (Exception e) {
            throw new BadRequestException("\u975e\u6cd5\u7ebf\u7d22\u72b6\u6001\u7b5b\u9009\u503c");
        }
    }

    private String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    @Transactional
    public ApiDtos.QuoteAdminResponse updateStatus(UUID id, ApiDtos.QuoteStatusUpdate r, String operator, HttpServletRequest req) {
        Quote q = (Quote)this.repo.findById((Object)id).orElseThrow(NotFoundException::new);
        ApiDtos.QuoteAdminResponse before = this.admin(q);
        Quote.Status next = this.parseStatus(r.status());
        if (q.getStatus() == Quote.Status.CONVERTED && next != Quote.Status.CONVERTED) {
            throw new BadRequestException("\u5df2\u8f6c\u4e3a\u8ba2\u5355\u7684\u8be2\u4ef7\u4e0d\u80fd\u6539\u56de\u5176\u4ed6\u72b6\u6001");
        }
        q.setStatus(next);
        q.setUpdatedAt(Instant.now());
        ApiDtos.QuoteAdminResponse after = this.admin(q);
        this.audit.log("QUOTE_STATUS_UPDATE", "QUOTE", q.getBusinessNo(), operator, (Object)before, (Object)after, r.reason(), req);
        return after;
    }

    @Transactional
    public QuoteLeadDtos.QuoteLeadResponse updateCrm(UUID id, QuoteLeadDtos.QuoteCrmUpdateRequest r, String operator, HttpServletRequest req) {
        Quote q = (Quote)this.repo.findById((Object)id).orElseThrow(NotFoundException::new);
        QuoteLeadDtos.QuoteLeadResponse before = this.leadAdmin(q);
        q.setLeadScore(r.leadScore());
        q.setLeadIntentLevel(this.trimNull(r.leadIntentLevel()));
        q.setNextAction(this.trimNull(r.nextAction()));
        q.setFollowUpNote(this.trimNull(r.followUpNote()));
        q.setLastContactedAt(Instant.now());
        q.setUpdatedAt(Instant.now());
        QuoteLeadDtos.QuoteLeadResponse after = this.leadAdmin(q);
        this.audit.log("QUOTE_CRM_UPDATE", "QUOTE", q.getBusinessNo(), operator, (Object)before, (Object)after, r.reason() == null || r.reason().isBlank() ? "\u66f4\u65b0\u7ebf\u7d22\u8ddf\u8fdb\u4fe1\u606f" : r.reason(), req);
        return after;
    }

    @Transactional
    public QuoteLeadDtos.QuoteAiSuggestionResponse generateLeadSuggestion(UUID id, String operator, HttpServletRequest req) {
        String level;
        Quote q = (Quote)this.repo.findById((Object)id).orElseThrow(NotFoundException::new);
        QuoteLeadDtos.QuoteLeadResponse before = this.leadAdmin(q);
        int score = this.scoreLead(q);
        String next = switch (level = score >= 75 ? "HIGH" : (score >= 45 ? "MEDIUM" : "LOW")) {
            case "HIGH" -> "\u4f18\u5148\u8054\u7cfb\uff0c\u786e\u8ba4\u9884\u7b97\u3001\u4ea4\u4ed8\u65f6\u95f4\u548c\u4ed8\u6b3e\u65b9\u5f0f\uff0c\u5e76\u5728\u5f53\u5929\u7ed9\u51fa\u660e\u786e\u62a5\u4ef7\u3002";
            case "MEDIUM" -> "\u5148\u8865\u9f50\u9700\u6c42\u8fb9\u754c\uff0c\u786e\u8ba4\u5e73\u53f0\u3001\u5f53\u524d\u8fdb\u5ea6\u548c\u53ef\u63a5\u53d7\u4ea4\u4ed8\u5468\u671f\u540e\u518d\u62a5\u4ef7\u3002";
            default -> "\u5148\u7528\u4f4e\u538b\u6c9f\u901a\u786e\u8ba4\u771f\u5b9e\u9700\u6c42\u548c\u8054\u7cfb\u65b9\u5f0f\uff0c\u907f\u514d\u6295\u5165\u8fc7\u591a\u4eba\u5de5\u65f6\u95f4\u3002";
        };
        String reply = this.buildReply(q, level, next);
        String reason = this.buildScoreReason(q, score, level);
        q.setLeadScore(Integer.valueOf(score));
        q.setLeadIntentLevel(level);
        q.setNextAction(next);
        q.setAiSuggestion(reply + "\n\n\u5224\u65ad\u4f9d\u636e\uff1a" + reason);
        q.setUpdatedAt(Instant.now());
        this.audit.log("QUOTE_AI_SUGGESTION", "QUOTE", q.getBusinessNo(), operator, (Object)before, (Object)this.leadAdmin(q), "\u751f\u6210AI\u7ebf\u7d22\u5efa\u8bae", req);
        return new QuoteLeadDtos.QuoteAiSuggestionResponse(Integer.valueOf(score), level, next, reply, reason);
    }

    @Transactional
    public ApiDtos.AdminOrderResponse convertToOrder(UUID id, ApiDtos.QuoteConvertRequest r, String operator, HttpServletRequest req) {
        Quote q = (Quote)this.repo.findById((Object)id).orElseThrow(NotFoundException::new);
        if (q.getStatus() == Quote.Status.CLOSED || q.getStatus() == Quote.Status.CANCELLED) {
            throw new BadRequestException("\u5df2\u5173\u95ed\u6216\u5df2\u53d6\u6d88\u7684\u8be2\u4ef7\u4e0d\u80fd\u8f6c\u4e3a\u8ba2\u5355");
        }
        this.ensureBusinessNo(q);
        ApiDtos.QuoteAdminResponse before = this.admin(q);
        ApiDtos.AdminOrderResponse order = this.orders.createFromQuote(q, operator, req, r.reason());
        if (q.getStatus() != Quote.Status.CONVERTED) {
            q.setStatus(Quote.Status.CONVERTED);
            q.setUpdatedAt(Instant.now());
            this.audit.log("QUOTE_CONVERTED", "QUOTE", q.getBusinessNo(), operator, (Object)before, (Object)this.admin(q), r.reason() == null || r.reason().isBlank() ? "\u8be2\u4ef7\u8f6c\u8ba2\u5355" : r.reason(), req);
        }
        return order;
    }

    @Transactional(readOnly=true)
    public Quote find(UUID id) {
        return (Quote)this.repo.findById((Object)id).orElseThrow(NotFoundException::new);
    }

    private ApiDtos.QuoteAdminResponse admin(Quote q) {
        return new ApiDtos.QuoteAdminResponse(q.getId(), q.getBusinessNo(), q.getGame(), q.getDemand(), q.getCurrentProgress(), q.getScreenshotAssetId(), q.getPlatform(), q.getExpectedTime(), q.getContactMethod(), q.getCustomerContact(), q.getCustomerIpHash(), q.getStatus().name(), q.getCreatedAt());
    }

    private QuoteLeadDtos.QuoteLeadResponse leadAdmin(Quote q) {
        return new QuoteLeadDtos.QuoteLeadResponse(q.getId(), q.getBusinessNo(), q.getGame(), q.getDemand(), q.getCurrentProgress(), q.getScreenshotAssetId(), q.getPlatform(), q.getExpectedTime(), q.getContactMethod(), q.getCustomerContact(), q.getCustomerIpHash(), q.getStatus().name(), q.getCreatedAt(), q.getLeadScore(), q.getLeadIntentLevel(), q.getNextAction(), q.getFollowUpNote(), q.getAiSuggestion(), q.getLastContactedAt(), q.getFirstTouchSource(), q.getUtmSource(), q.getUtmMedium(), q.getUtmCampaign(), q.getReferrer(), q.getUpdatedAt());
    }

    @Transactional
    public void delete(UUID id, String operator, HttpServletRequest req) {
        Quote q = (Quote)this.repo.findById((Object)id).orElseThrow(NotFoundException::new);
        ApiDtos.QuoteAdminResponse before = this.admin(q);
        this.repo.delete((Object)q);
        this.audit.log("QUOTE_DELETE", "QUOTE", q.getBusinessNo(), operator, (Object)before, null, "\u540e\u53f0\u5220\u9664\u8be2\u4ef7\u7ebf\u7d22", req);
    }

    private void ensureBusinessNo(Quote q) {
        if (q.getBusinessNo() == null || q.getBusinessNo().isBlank()) {
            q.setBusinessNo(this.generateBusinessNo());
            q.setUpdatedAt(Instant.now());
        }
    }

    private String generateBusinessNo() {
        for (int n = 0; n < 8; ++n) {
            StringBuilder b = new StringBuilder("XH");
            for (int i = 0; i < 10; ++i) {
                b.append(ALPH[RNG.nextInt(ALPH.length)]);
            }
            if (!this.repo.findByBusinessNo(b.toString()).isEmpty()) continue;
            return b.toString();
        }
        throw new IllegalStateException("\u4e1a\u52a1\u7f16\u53f7\u751f\u6210\u5931\u8d25");
    }

    private Quote.Status parseStatus(String s) {
        try {
            return Quote.Status.valueOf((String)s.toUpperCase(Locale.ROOT));
        }
        catch (Exception e) {
            throw new BadRequestException("\u975e\u6cd5\u8be2\u4ef7\u72b6\u6001");
        }
    }

    private void validateContact(PublicQuoteRequest r) {
        boolean phoneMethod;
        String method = this.trimNull(r.contactMethod());
        String contact = this.trimNull(r.customerContact());
        if (method == null || method.isBlank() || contact == null) {
            throw new BadRequestException("\u8bf7\u586b\u5199\u8054\u7cfb\u65b9\u5f0f\u548c\u8054\u7cfb\u53f7");
        }
        Object normalized = contact.replaceAll("[\\s()-]", "");
        if (((String)normalized).matches("1[3-9]\\d{9}")) {
            normalized = "+86" + (String)normalized;
        } else if (((String)normalized).startsWith("00")) {
            normalized = "+" + ((String)normalized).substring(2);
        }
        boolean bl = phoneMethod = method.contains("\u624b\u673a") || method.equalsIgnoreCase("phone") || method.equalsIgnoreCase("mobile");
        if (phoneMethod && !((String)normalized).matches("\\+[1-9]\\d{7,14}")) {
            throw new BadRequestException("\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u624b\u673a\u53f7\u548c\u56fd\u5bb6\u533a\u53f7");
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
        String demand = q.getDemand() == null ? "\u4f60\u7684\u9700\u6c42" : q.getDemand();
        String time = q.getExpectedTime() == null ? "\u4ea4\u4ed8\u65f6\u95f4" : q.getExpectedTime();
        String platform = q.getPlatform() == null ? "\u6e38\u620f\u5e73\u53f0" : q.getPlatform();
        String prefix = "HIGH".equals(level) ? "\u8fd9\u4e2a\u9700\u6c42\u4fe1\u606f\u6bd4\u8f83\u5b8c\u6574\uff0c\u6211\u53ef\u4ee5\u4f18\u5148\u5e2e\u4f60\u786e\u8ba4\u3002" : "\u6211\u5148\u5e2e\u4f60\u628a\u9700\u6c42\u8fb9\u754c\u786e\u8ba4\u6e05\u695a\u3002";
        return prefix + "\u76ee\u524d\u770b\u5230\u7684\u9700\u6c42\u662f\u300c" + demand + "\u300d\uff0c\u5e73\u53f0\u4e3a\u300c" + platform + "\u300d\uff0c\u671f\u671b\u65f6\u95f4\u4e3a\u300c" + time + "\u300d\u3002" + next;
    }

    private String buildScoreReason(Quote q, int score, String level) {
        ArrayList<String> reasons = new ArrayList<String>();
        if (q.getCustomerContact() != null && !q.getCustomerContact().isBlank()) {
            reasons.add("\u5df2\u7559\u4e0b\u8054\u7cfb\u65b9\u5f0f");
        }
        if (q.getExpectedTime() != null && !q.getExpectedTime().isBlank()) {
            reasons.add("\u6709\u671f\u671b\u65f6\u95f4");
        }
        if (q.getScreenshotAssetId() != null) {
            reasons.add("\u5df2\u4e0a\u4f20\u622a\u56fe");
        }
        if (q.getCurrentProgress() != null && !q.getCurrentProgress().isBlank()) {
            reasons.add("\u6709\u5f53\u524d\u8fdb\u5ea6");
        }
        if (reasons.isEmpty()) {
            reasons.add("\u9700\u6c42\u4fe1\u606f\u4ecd\u9700\u8865\u5145");
        }
        return "\u8bc4\u5206 " + score + "\uff0c\u610f\u5411\u7b49\u7ea7 " + level + "\uff1b" + String.join((CharSequence)"\u3001", reasons) + "\u3002";
    }

    private String trimNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    @Generated
    public QuoteService(QuoteRepository repo, UploadService uploads, AuditService audit, OrderService orders) {
        this.repo = repo;
        this.uploads = uploads;
        this.audit = audit;
        this.orders = orders;
    }
}

