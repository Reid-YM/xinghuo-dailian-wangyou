package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.AdminTestFeedback;
import com.xinghuo.repository.AdminTestFeedbackRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AdminTestFeedbackService {
    private final AdminTestFeedbackRepository repo;
    private final AuditService audit;

    public List<ApiDtos.TestFeedbackResponse> list() {
        return repo.findTop100ByOrderByUpdatedAtDesc().stream().map(this::toResponse).toList();
    }

    public ApiDtos.TestFeedbackResponse get(Long id) {
        return toResponse(repo.findById(id).orElseThrow(NotFoundException::new));
    }

    public ApiDtos.TestFeedbackResponse create(
            ApiDtos.TestFeedbackCreateRequest request,
            String operator,
            HttpServletRequest httpRequest) {
        Instant now = Instant.now();
        AdminTestFeedback feedback = AdminTestFeedback.builder()
                .title(clean(request.title(), 120))
                .pagePath(optional(request.pagePath(), 240))
                .content(clean(request.content(), 5000))
                .priority(priority(request.priority()))
                .status(AdminTestFeedback.Status.NEW)
                .operatorNote(optional(request.operatorNote(), 1000))
                .createdBy(operator)
                .createdAt(now)
                .updatedAt(now)
                .build();
        feedback = repo.save(feedback);
        ApiDtos.TestFeedbackResponse after = toResponse(feedback);
        audit.log(
                "TEST_FEEDBACK_CREATE",
                "TEST_FEEDBACK",
                String.valueOf(feedback.getId()),
                operator,
                null,
                after,
                "提交网页测试修改反馈",
                httpRequest);
        return after;
    }

    public ApiDtos.TestFeedbackResponse update(
            Long id,
            ApiDtos.TestFeedbackUpdateRequest request,
            String operator,
            HttpServletRequest httpRequest) {
        AdminTestFeedback feedback = repo.findById(id).orElseThrow(NotFoundException::new);
        ApiDtos.TestFeedbackResponse before = toResponse(feedback);
        feedback.setTitle(clean(request.title(), 120));
        feedback.setPagePath(optional(request.pagePath(), 240));
        feedback.setContent(clean(request.content(), 5000));
        feedback.setPriority(priority(request.priority()));
        feedback.setStatus(status(request.status()));
        if (request.operatorNote() != null) {
            feedback.setOperatorNote(optional(request.operatorNote(), 1000));
        }
        feedback.setUpdatedAt(Instant.now());
        feedback = repo.save(feedback);
        ApiDtos.TestFeedbackResponse after = toResponse(feedback);
        audit.log(
                "TEST_FEEDBACK_UPDATE",
                "TEST_FEEDBACK",
                String.valueOf(feedback.getId()),
                operator,
                before,
                after,
                "更新网页测试修改反馈",
                httpRequest);
        return after;
    }

    public ApiDtos.TestFeedbackResponse attach(
            Long id,
            UUID assetId,
            String operator,
            HttpServletRequest httpRequest) {
        AdminTestFeedback feedback = repo.findById(id).orElseThrow(NotFoundException::new);
        ApiDtos.TestFeedbackResponse before = toResponse(feedback);
        feedback.setAttachmentAssetId(assetId);
        feedback.setUpdatedAt(Instant.now());
        feedback = repo.save(feedback);
        ApiDtos.TestFeedbackResponse after = toResponse(feedback);
        audit.log(
                "TEST_FEEDBACK_ATTACHMENT_UPDATE",
                "TEST_FEEDBACK",
                String.valueOf(feedback.getId()),
                operator,
                before,
                after,
                "上传网页测试反馈附件",
                httpRequest);
        return after;
    }

    public long newCount() {
        return repo.countByStatus(AdminTestFeedback.Status.NEW);
    }

    private ApiDtos.TestFeedbackResponse toResponse(AdminTestFeedback feedback) {
        return new ApiDtos.TestFeedbackResponse(
                feedback.getId(),
                feedback.getTitle(),
                feedback.getPagePath(),
                feedback.getContent(),
                feedback.getPriority().name(),
                feedback.getStatus().name(),
                feedback.getOperatorNote(),
                feedback.getAttachmentAssetId(),
                feedback.getCreatedBy(),
                feedback.getCreatedAt(),
                feedback.getUpdatedAt());
    }

    private AdminTestFeedback.Priority priority(String value) {
        try {
            return AdminTestFeedback.Priority.valueOf(clean(value, 16).toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return AdminTestFeedback.Priority.NORMAL;
        }
    }

    private AdminTestFeedback.Status status(String value) {
        try {
            return AdminTestFeedback.Status.valueOf(clean(value, 24).toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            throw new BadRequestException("非法反馈状态");
        }
    }

    private String clean(String value, int max) {
        String out = value == null ? "" : value.trim();
        if (out.isBlank()) {
            throw new BadRequestException("请填写完整反馈信息");
        }
        return out.length() > max ? out.substring(0, max) : out;
    }

    private String optional(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String out = value.trim();
        return out.length() > max ? out.substring(0, max) : out;
    }

    public AdminTestFeedbackService(AdminTestFeedbackRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }
}
