package com.xinghuo.feedback.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestFeedbackCreateRequest(
        @NotBlank @Size(max=120) @NotBlank @Size(max=120) String title,
        @Size(max=240) @Size(max=240) String pagePath,
        @NotBlank @Size(max=5000) @NotBlank @Size(max=5000) String content,
        @Size(max=16) @Size(max=16) String priority,
        @Size(max=1000) @Size(max=1000) String operatorNote) {
}
