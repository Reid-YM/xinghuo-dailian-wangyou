package com.xinghuo.quote.application;

import java.util.List;
import java.util.UUID;

/** Quote-owned persistence and integration boundary. */
public interface QuotePort {
    QuoteSubmissionResult create(QuoteSubmissionCommand command);

    List<QuoteAdminView> list();

    QuoteLeadPageView listLeads(
            String keyword, String status, String intent, String source, int page, int size);

    QuoteAdminView updateStatus(UUID id, QuoteStatusUpdateCommand command, String operator);

    QuoteLeadView updateCrm(UUID id, QuoteCrmUpdateCommand command, String operator);

    QuoteAiSuggestionView generateLeadSuggestion(UUID id, String operator);

    ConvertedOrderView convertToOrder(UUID id, QuoteConvertCommand command, String operator);

    QuoteAdminView find(UUID id);

    void delete(UUID id, String operator);
}
