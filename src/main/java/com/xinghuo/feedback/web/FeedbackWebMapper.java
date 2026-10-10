package com.xinghuo.feedback.web;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.feedback.application.CreateTestFeedbackCommand;
import com.xinghuo.feedback.application.TestFeedbackView;
import com.xinghuo.feedback.application.UpdateTestFeedbackCommand;
import java.util.List;

/** Maps test-feedback HTTP DTOs to and from application-owned contracts. */
public final class FeedbackWebMapper {
    private FeedbackWebMapper() {
    }

    public static CreateTestFeedbackCommand toCommand(ApiDtos.TestFeedbackCreateRequest request) {
        return new CreateTestFeedbackCommand(
                request.title(),
                request.pagePath(),
                request.content(),
                request.priority(),
                request.operatorNote());
    }

    public static UpdateTestFeedbackCommand toCommand(ApiDtos.TestFeedbackUpdateRequest request) {
        return new UpdateTestFeedbackCommand(
                request.title(),
                request.pagePath(),
                request.content(),
                request.priority(),
                request.status(),
                request.operatorNote());
    }

    public static ApiDtos.TestFeedbackResponse toResponse(TestFeedbackView view) {
        return new ApiDtos.TestFeedbackResponse(
                view.id(),
                view.title(),
                view.pagePath(),
                view.content(),
                view.priority(),
                view.status(),
                view.operatorNote(),
                view.attachmentAssetId(),
                view.createdBy(),
                view.createdAt(),
                view.updatedAt());
    }

    public static List<ApiDtos.TestFeedbackResponse> toResponses(List<TestFeedbackView> views) {
        return views.stream().map(FeedbackWebMapper::toResponse).toList();
    }
}
