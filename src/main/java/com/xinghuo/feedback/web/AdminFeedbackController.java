package com.xinghuo.feedback.web;

import com.xinghuo.exception.NotFoundException;
import com.xinghuo.feedback.application.FeedbackApplicationService;
import com.xinghuo.upload.infrastructure.UploadAsset;
import com.xinghuo.upload.infrastructure.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Generated;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/api/admin/test-feedback"})
public class AdminFeedbackController {
    private final FeedbackApplicationService feedback;
    private final UploadService uploads;

    @GetMapping
    List<TestFeedbackResponse> feedback() {
        return FeedbackWebMapper.toResponses(this.feedback.list());
    }

    @PostMapping
    TestFeedbackResponse createFeedback(@Valid @RequestBody TestFeedbackCreateRequest r, Authentication a, HttpServletRequest req) {
        return FeedbackWebMapper.toResponse(
                this.feedback.create(FeedbackWebMapper.toCommand(r), a.getName()));
    }

    @PutMapping(value={"/{id}"})
    TestFeedbackResponse updateFeedback(@PathVariable Long id, @Valid @RequestBody TestFeedbackUpdateRequest r, Authentication a, HttpServletRequest req) {
        return FeedbackWebMapper.toResponse(
                this.feedback.update(id, FeedbackWebMapper.toCommand(r), a.getName()));
    }

    @PostMapping(value={"/{id}/attachment"})
    TestFeedbackResponse uploadFeedbackAttachment(@PathVariable Long id, @RequestParam(value="file") MultipartFile file, Authentication a, HttpServletRequest req) {
        UploadAsset asset = this.uploads.storeImage(file, UploadAsset.Kind.TEST_FEEDBACK_ATTACHMENT);
        this.uploads.bind(asset.getId(), "TEST_FEEDBACK", String.valueOf(id));
        return FeedbackWebMapper.toResponse(this.feedback.attach(id, asset.getId(), a.getName()));
    }

    @GetMapping(value={"/{id}/attachment"})
    ResponseEntity<Resource> feedbackAttachment(@PathVariable Long id) {
        var item = this.feedback.get(id);
        if (item.attachmentAssetId() == null) {
            throw new NotFoundException();
        }
        return ResponseEntity.ok()
                .contentType(this.uploads.mediaType(item.attachmentAssetId()))
                .cacheControl(CacheControl.noStore())
                .body(this.uploads.load(item.attachmentAssetId(), "TEST_FEEDBACK", String.valueOf(id)));
    }

    @Generated
    public AdminFeedbackController(FeedbackApplicationService feedback, UploadService uploads) {
        this.feedback = feedback;
        this.uploads = uploads;
    }
}
