package com.xinghuo.feedback.infrastructure;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.feedback.application.CreateTestFeedbackCommand;
import com.xinghuo.feedback.application.FeedbackPort;
import com.xinghuo.feedback.application.TestFeedbackView;
import com.xinghuo.feedback.application.UpdateTestFeedbackCommand;
import com.xinghuo.model.AdminTestFeedback;
import com.xinghuo.repository.AdminTestFeedbackRepository;
import com.xinghuo.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Feedback-owned persistence and audit adapter. */
@Component
public class FeedbackPersistenceAdapter implements FeedbackPort {
    private final AdminTestFeedbackRepository repo;
    private final AuditService audit;

    public FeedbackPersistenceAdapter(AdminTestFeedbackRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestFeedbackView> list() {
        return repo.findTop100ByOrderByUpdatedAtDesc().stream().map(this::view).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TestFeedbackView get(Long id) {
        return view(repo.findById(id).orElseThrow(NotFoundException::new));
    }

    @Override
    @Transactional
    public TestFeedbackView create(CreateTestFeedbackCommand command, String operator) {
        Instant now = Instant.now();
        AdminTestFeedback item = AdminTestFeedback.builder()
                .title(clean(command.title(), 120))
                .pagePath(optional(command.pagePath(), 240))
                .content(clean(command.content(), 5000))
                .priority(priority(command.priority()))
                .status(AdminTestFeedback.Status.NEW)
                .operatorNote(optional(command.operatorNote(), 1000))
                .createdBy(operator)
                .createdAt(now)
                .updatedAt(now)
                .build();
        item = repo.save(item);
        TestFeedbackView after = view(item);
        audit.log(
                "TEST_FEEDBACK_CREATE",
                "TEST_FEEDBACK",
                String.valueOf(item.getId()),
                operator,
                null,
                after,
                "提交网页测试修改反馈",
                currentRequest());
        return after;
    }

    @Override
    @Transactional
    public TestFeedbackView update(Long id, UpdateTestFeedbackCommand command, String operator) {
        AdminTestFeedback item = repo.findById(id).orElseThrow(NotFoundException::new);
        TestFeedbackView before = view(item);
        item.setTitle(clean(command.title(), 120));
        item.setPagePath(optional(command.pagePath(), 240));
        item.setContent(clean(command.content(), 5000));
        item.setPriority(priority(command.priority()));
        item.setStatus(status(command.status()));
        item.setOperatorNote(optional(command.operatorNote(), 1000));
        item.setUpdatedAt(Instant.now());
        item = repo.save(item);
        TestFeedbackView after = view(item);
        audit.log(
                "TEST_FEEDBACK_UPDATE",
                "TEST_FEEDBACK",
                String.valueOf(item.getId()),
                operator,
                before,
                after,
                "更新网页测试修改反馈",
                currentRequest());
        return after;
    }

    @Override
    @Transactional
    public TestFeedbackView attach(Long id, UUID assetId, String operator) {
        AdminTestFeedback item = repo.findById(id).orElseThrow(NotFoundException::new);
        TestFeedbackView before = view(item);
        item.setAttachmentAssetId(assetId);
        item.setUpdatedAt(Instant.now());
        item = repo.save(item);
        TestFeedbackView after = view(item);
        audit.log(
                "TEST_FEEDBACK_ATTACHMENT_UPDATE",
                "TEST_FEEDBACK",
                String.valueOf(item.getId()),
                operator,
                before,
                after,
                "上传网页测试反馈附件",
                currentRequest());
        return after;
    }

    private TestFeedbackView view(AdminTestFeedback item) {
        return new TestFeedbackView(
                item.getId(),
                item.getTitle(),
                item.getPagePath(),
                item.getContent(),
                item.getPriority().name(),
                item.getStatus().name(),
                item.getOperatorNote(),
                item.getAttachmentAssetId(),
                item.getCreatedBy(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    private AdminTestFeedback.Priority priority(String value) {
        try {
            return AdminTestFeedback.Priority.valueOf(clean(value, 16).toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return AdminTestFeedback.Priority.NORMAL;
        }
    }

    private AdminTestFeedback.Status status(String value) {
        try {
            return AdminTestFeedback.Status.valueOf(clean(value, 24).toUpperCase(Locale.ROOT));
        } catch (Exception e) {
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

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest();
        }
        return null;
    }
}
