package com.xinghuo.feedback.application;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Application boundary for administrative test feedback. */
@Service
public class FeedbackApplicationService {
    private final FeedbackPort port;

    public FeedbackApplicationService(FeedbackPort port) {
        this.port = port;
    }

    public List<TestFeedbackView> list() {
        return port.list();
    }

    public TestFeedbackView get(Long id) {
        return port.get(id);
    }

    public TestFeedbackView create(CreateTestFeedbackCommand command, String operator) {
        return port.create(command, operator);
    }

    public TestFeedbackView update(Long id, UpdateTestFeedbackCommand command, String operator) {
        return port.update(id, command, operator);
    }

    public TestFeedbackView attach(Long id, UUID assetId, String operator) {
        return port.attach(id, assetId, operator);
    }
}
