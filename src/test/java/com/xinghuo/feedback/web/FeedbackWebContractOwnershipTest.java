package com.xinghuo.feedback.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FeedbackWebContractOwnershipTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void feedbackContractsAreOwnedByFeedbackWeb() throws IOException {
        Path web = ROOT.resolve("src/main/java/com/xinghuo/feedback/web");
        String create = Files.readString(web.resolve("TestFeedbackCreateRequest.java"));
        String update = Files.readString(web.resolve("TestFeedbackUpdateRequest.java"));
        String response = Files.readString(web.resolve("TestFeedbackResponse.java"));
        String mapper = Files.readString(web.resolve("FeedbackWebMapper.java"));
        String controller = Files.readString(web.resolve("AdminFeedbackController.java"));

        assertTrue(create.contains("@NotBlank @Size(max=120) @NotBlank @Size(max=120) String title"));
        assertTrue(create.contains("@NotBlank @Size(max=5000) @NotBlank @Size(max=5000) String content"));
        assertTrue(update.contains("@NotBlank @Size(max=16) @NotBlank @Size(max=16) String priority"));
        assertTrue(update.contains("@NotBlank @Size(max=24) @NotBlank @Size(max=24) String status"));
        assertTrue(response.contains("UUID attachmentAssetId"));
        assertTrue(response.contains("Instant createdAt"));
        assertTrue(response.contains("Instant updatedAt"));
        assertFalse(mapper.contains("com.xinghuo.dto.ApiDtos"));
        assertFalse(controller.contains("com.xinghuo.dto.ApiDtos"));
    }
}
