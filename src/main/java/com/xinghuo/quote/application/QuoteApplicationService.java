package com.xinghuo.quote.application;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.QuoteLeadDtos;
import com.xinghuo.model.Quote;
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

    public List<ApiDtos.QuoteAdminResponse> list() {
        return port.list();
    }

    public QuoteLeadDtos.QuoteLeadPageResponse listLeads(
            String keyword, String status, String intent, String source, int page, int size) {
        return port.listLeads(keyword, status, intent, source, page, size);
    }

    public ApiDtos.QuoteAdminResponse updateStatus(
            UUID id, ApiDtos.QuoteStatusUpdate request, String operator) {
        return port.updateStatus(id, request, operator);
    }

    public QuoteLeadDtos.QuoteLeadResponse updateCrm(
            UUID id, QuoteLeadDtos.QuoteCrmUpdateRequest request, String operator) {
        return port.updateCrm(id, request, operator);
    }

    public QuoteLeadDtos.QuoteAiSuggestionResponse generateLeadSuggestion(UUID id, String operator) {
        return port.generateLeadSuggestion(id, operator);
    }

    public ApiDtos.AdminOrderResponse convertToOrder(
            UUID id, ApiDtos.QuoteConvertRequest request, String operator) {
        return port.convertToOrder(id, request, operator);
    }

    public Quote find(UUID id) {
        return port.find(id);
    }

    public void delete(UUID id, String operator) {
        port.delete(id, operator);
    }
}
