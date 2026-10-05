/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.servlet.http.HttpServletRequest
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 */
package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.AdminTestFeedback;
import com.xinghuo.repository.AdminTestFeedbackRepository;
import com.xinghuo.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.Generated;
import org.springframework.stereotype.Service;

@Service
public class AdminTestFeedbackService {
    private final AdminTestFeedbackRepository repo;
    private final AuditService audit;

    public List<ApiDtos.TestFeedbackResponse> list() {
        return this.repo.findTop100ByOrderByUpdatedAtDesc().stream().map(this::toResponse).toList();
    }

    public ApiDtos.TestFeedbackResponse get(Long id) {
        return this.toResponse((AdminTestFeedback)this.repo.findById(id).orElseThrow(NotFoundException::new));
    }

    public ApiDtos.TestFeedbackResponse create(ApiDtos.TestFeedbackCreateRequest r, String operator, HttpServletRequest req) {
        Instant now = Instant.now();
        AdminTestFeedback x = AdminTestFeedback.builder().title(this.clean(r.title(), 120)).pagePath(this.optional(r.pagePath(), 240)).content(this.clean(r.content(), 5000)).priority(this.priority(r.priority())).status(AdminTestFeedback.Status.NEW).operatorNote(this.optional(r.operatorNote(), 1000)).createdBy(operator).createdAt(now).updatedAt(now).build();
        x = (AdminTestFeedback)this.repo.save(x);
        ApiDtos.TestFeedbackResponse after = this.toResponse(x);
        this.audit.log("TEST_FEEDBACK_CREATE", "TEST_FEEDBACK", String.valueOf(x.getId()), operator, null, after, "\u63d0\u4ea4\u7f51\u9875\u6d4b\u8bd5\u4fee\u6539\u53cd\u9988", req);
        return after;
    }

    public ApiDtos.TestFeedbackResponse update(Long id, ApiDtos.TestFeedbackUpdateRequest r, String operator, HttpServletRequest req) {
        AdminTestFeedback x = (AdminTestFeedback)this.repo.findById(id).orElseThrow(NotFoundException::new);
        ApiDtos.TestFeedbackResponse before = this.toResponse(x);
        x.setTitle(this.clean(r.title(), 120));
        x.setPagePath(this.optional(r.pagePath(), 240));
        x.setContent(this.clean(r.content(), 5000));
        x.setPriority(this.priority(r.priority()));
        x.setStatus(this.status(r.status()));
        x.setOperatorNote(this.optional(r.operatorNote(), 1000));
        x.setUpdatedAt(Instant.now());
        x = (AdminTestFeedback)this.repo.save(x);
        ApiDtos.TestFeedbackResponse after = this.toResponse(x);
        this.audit.log("TEST_FEEDBACK_UPDATE", "TEST_FEEDBACK", String.valueOf(x.getId()), operator, before, after, "\u66f4\u65b0\u7f51\u9875\u6d4b\u8bd5\u4fee\u6539\u53cd\u9988", req);
        return after;
    }

    public ApiDtos.TestFeedbackResponse attach(Long id, UUID assetId, String operator, HttpServletRequest req) {
        AdminTestFeedback x = (AdminTestFeedback)this.repo.findById(id).orElseThrow(NotFoundException::new);
        ApiDtos.TestFeedbackResponse before = this.toResponse(x);
        x.setAttachmentAssetId(assetId);
        x.setUpdatedAt(Instant.now());
        x = (AdminTestFeedback)this.repo.save(x);
        ApiDtos.TestFeedbackResponse after = this.toResponse(x);
        this.audit.log("TEST_FEEDBACK_ATTACHMENT_UPDATE", "TEST_FEEDBACK", String.valueOf(x.getId()), operator, before, after, "\u4e0a\u4f20\u7f51\u9875\u6d4b\u8bd5\u53cd\u9988\u9644\u4ef6", req);
        return after;
    }

    public long newCount() {
        return this.repo.countByStatus(AdminTestFeedback.Status.NEW);
    }

    private ApiDtos.TestFeedbackResponse toResponse(AdminTestFeedback x) {
        return new ApiDtos.TestFeedbackResponse(x.getId(), x.getTitle(), x.getPagePath(), x.getContent(), x.getPriority().name(), x.getStatus().name(), x.getOperatorNote(), x.getAttachmentAssetId(), x.getCreatedBy(), x.getCreatedAt(), x.getUpdatedAt());
    }

    private AdminTestFeedback.Priority priority(String value) {
        try {
            return AdminTestFeedback.Priority.valueOf(this.clean(value, 16).toUpperCase(Locale.ROOT));
        }
        catch (Exception e) {
            return AdminTestFeedback.Priority.NORMAL;
        }
    }

    private AdminTestFeedback.Status status(String value) {
        try {
            return AdminTestFeedback.Status.valueOf(this.clean(value, 24).toUpperCase(Locale.ROOT));
        }
        catch (Exception e) {
            throw new BadRequestException("\u975e\u6cd5\u53cd\u9988\u72b6\u6001");
        }
    }

    private String clean(String value, int max) {
        String out;
        String string = out = value == null ? "" : value.trim();
        if (out.isBlank()) {
            throw new BadRequestException("\u8bf7\u586b\u5199\u5b8c\u6574\u53cd\u9988\u4fe1\u606f");
        }
        return out.length() > max ? out.substring(0, max) : out;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String optional(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String out = value.trim();
        return out.length() > max ? out.substring(0, max) : out;
    }

    @Generated
    public AdminTestFeedbackService(AdminTestFeedbackRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }
}

