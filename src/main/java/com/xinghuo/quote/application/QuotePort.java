package com.xinghuo.quote.application;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.QuoteLeadDtos;
import com.xinghuo.model.Quote;
import java.util.List;
import java.util.UUID;

/** Quote-owned persistence and integration boundary. */
public interface QuotePort {
    QuoteSubmissionResult create(QuoteSubmissionCommand command);

    List<ApiDtos.QuoteAdminResponse> list();

    QuoteLeadDtos.QuoteLeadPageResponse listLeads(
            String keyword, String status, String intent, String source, int page, int size);

    ApiDtos.QuoteAdminResponse updateStatus(UUID id, ApiDtos.QuoteStatusUpdate request, String operator);

    QuoteLeadDtos.QuoteLeadResponse updateCrm(
            UUID id, QuoteLeadDtos.QuoteCrmUpdateRequest request, String operator);

    QuoteLeadDtos.QuoteAiSuggestionResponse generateLeadSuggestion(UUID id, String operator);

    ApiDtos.AdminOrderResponse convertToOrder(
            UUID id, ApiDtos.QuoteConvertRequest request, String operator);

    Quote find(UUID id);

    void delete(UUID id, String operator);
}
