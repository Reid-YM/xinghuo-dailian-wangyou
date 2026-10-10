package com.xinghuo.feedback.application;

import java.util.List;
import java.util.UUID;

/** Feedback-owned persistence and audit boundary. */
public interface FeedbackPort {
    List<TestFeedbackView> list();

    TestFeedbackView get(Long id);

    TestFeedbackView create(CreateTestFeedbackCommand command, String operator);

    TestFeedbackView update(Long id, UpdateTestFeedbackCommand command, String operator);

    TestFeedbackView attach(Long id, UUID assetId, String operator);

    long countNew();
}
