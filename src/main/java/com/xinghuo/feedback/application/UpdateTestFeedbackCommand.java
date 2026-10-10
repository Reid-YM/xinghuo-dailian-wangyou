package com.xinghuo.feedback.application;

/** Application-owned command for updating test feedback. */
public record UpdateTestFeedbackCommand(
        String title,
        String pagePath,
        String content,
        String priority,
        String status,
        String operatorNote) {
}
