package com.xinghuo.quote.application;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Application boundary for public quote submission and quote administration. */
@Service
public class QuoteApplicationService {
    private final QuotePort port;

    public QuoteApplicationService(QuotePort port) {
        this.port = port;
    }

    public QuoteSubmissionResult create(QuoteSubmissionCommand command) {
        return port.create(command);
    }

    public List<QuoteAdminView> list() {
        return port.list();
    }

    public QuoteLeadPageView listLeads(
            String keyword, String status, String intent, String source, int page, int size) {
        return port.listLeads(keyword, status, intent, source, page, size);
    }

    public QuoteAdminView updateStatus(
            UUID id, QuoteStatusUpdateCommand command, String operator) {
        return port.updateStatus(id, command, operator);
    }

    public QuoteLeadView updateCrm(UUID id, QuoteCrmUpdateCommand command, String operator) {
        return port.updateCrm(id, command, operator);
    }

    public QuoteAiSuggestionView generateLeadSuggestion(UUID id, String operator) {
        return port.generateLeadSuggestion(id, operator);
    }

    public ConvertedOrderView convertToOrder(UUID id, QuoteConvertCommand command, String operator) {
        return port.convertToOrder(id, command, operator);
    }

    public QuoteAdminView find(UUID id) {
        return port.find(id);
    }

    public void delete(UUID id, String operator) {
        port.delete(id, operator);
    }
}
