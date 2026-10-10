package com.xinghuo.feedback.application;

/** Application-owned command for creating test feedback. */
public record CreateTestFeedbackCommand(
        String title,
        String pagePath,
        String content,
        String priority,
        String operatorNote) {
}
