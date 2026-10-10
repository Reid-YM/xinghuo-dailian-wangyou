package com.xinghuo.quote.web;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.PublicQuoteRequest;
import com.xinghuo.dto.QuoteLeadDtos;
import com.xinghuo.quote.application.ConvertedOrderView;
import com.xinghuo.quote.application.QuoteAdminView;
import com.xinghuo.quote.application.QuoteAiSuggestionView;
import com.xinghuo.quote.application.QuoteConvertCommand;
import com.xinghuo.quote.application.QuoteCrmUpdateCommand;
import com.xinghuo.quote.application.QuoteLeadPageView;
import com.xinghuo.quote.application.QuoteLeadView;
import com.xinghuo.quote.application.QuoteStatusUpdateCommand;
import com.xinghuo.quote.application.QuoteSubmissionCommand;
import com.xinghuo.quote.application.QuoteSubmissionResult;
import java.util.List;

/** Maps HTTP-facing quote DTOs to and from application-owned contracts. */
public final class QuoteWebMapper {
    private QuoteWebMapper() {
    }

    public static QuoteSubmissionCommand toCommand(
            PublicQuoteRequest request,
            String idempotencyKey,
            String customerIpHash,
            Long customerAccountId) {
        return new QuoteSubmissionCommand(
                request.game(),
                request.demand(),
                request.currentProgress(),
                request.screenshotAssetId(),
                request.platform(),
                request.expectedTime(),
                request.contactMethod(),
                request.customerContact(),
                request.privacyConsent(),
                request.firstTouchSource(),
                request.utmSource(),
                request.utmMedium(),
                request.utmCampaign(),
                request.referrer(),
                idempotencyKey,
                customerIpHash,
                customerAccountId);
    }

    public static QuoteStatusUpdateCommand toCommand(ApiDtos.QuoteStatusUpdate request) {
        return new QuoteStatusUpdateCommand(request.status(), request.reason());
    }

    public static QuoteCrmUpdateCommand toCommand(QuoteLeadDtos.QuoteCrmUpdateRequest request) {
        return new QuoteCrmUpdateCommand(
                request.leadScore(),
                request.leadIntentLevel(),
                request.nextAction(),
                request.followUpNote(),
                request.reason());
    }

    public static QuoteConvertCommand toCommand(ApiDtos.QuoteConvertRequest request) {
        return new QuoteConvertCommand(request.reason());
    }

    public static ApiDtos.QuoteResponse toResponse(QuoteSubmissionResult result) {
        return new ApiDtos.QuoteResponse(
                result.id(), result.businessNo(), result.status(), result.createdAt());
    }

    public static ApiDtos.QuoteAdminResponse toResponse(QuoteAdminView view) {
        return new ApiDtos.QuoteAdminResponse(
                view.id(),
                view.businessNo(),
                view.game(),
                view.demand(),
                view.currentProgress(),
                view.screenshotAssetId(),
                view.platform(),
                view.expectedTime(),
                view.contactMethod(),
                view.customerContact(),
                view.customerIpHash(),
                view.status(),
                view.createdAt());
    }

    public static List<ApiDtos.QuoteAdminResponse> toAdminResponses(List<QuoteAdminView> views) {
        return views.stream().map(QuoteWebMapper::toResponse).toList();
    }

    public static QuoteLeadDtos.QuoteLeadResponse toResponse(QuoteLeadView view) {
        return new QuoteLeadDtos.QuoteLeadResponse(
                view.id(),
                view.businessNo(),
                view.game(),
                view.demand(),
                view.currentProgress(),
                view.screenshotAssetId(),
                view.platform(),
                view.expectedTime(),
                view.contactMethod(),
                view.customerContact(),
                view.customerIpHash(),
                view.status(),
                view.createdAt(),
                view.leadScore(),
                view.leadIntentLevel(),
                view.nextAction(),
                view.followUpNote(),
                view.aiSuggestion(),
                view.lastContactedAt(),
                view.firstTouchSource(),
                view.utmSource(),
                view.utmMedium(),
                view.utmCampaign(),
                view.referrer(),
                view.updatedAt(),
                view.customerSupplement());
    }

    public static QuoteLeadDtos.QuoteLeadPageResponse toResponse(QuoteLeadPageView view) {
        return new QuoteLeadDtos.QuoteLeadPageResponse(
                view.items().stream().map(QuoteWebMapper::toResponse).toList(),
                view.page(),
                view.size(),
                view.total(),
                view.totalPages(),
                view.sources());
    }

    public static QuoteLeadDtos.QuoteAiSuggestionResponse toResponse(QuoteAiSuggestionView view) {
        return new QuoteLeadDtos.QuoteAiSuggestionResponse(
                view.leadScore(),
                view.leadIntentLevel(),
                view.nextAction(),
                view.reply(),
                view.reason());
    }

    public static ApiDtos.AdminOrderResponse toResponse(ConvertedOrderView view) {
        return new ApiDtos.AdminOrderResponse(
                view.id(),
                view.orderNo(),
                view.businessNo(),
                view.sourceQuoteId(),
                view.status(),
                view.progressPercent(),
                view.progressText(),
                view.hasProgressImage(),
                view.customerNote(),
                view.internalNote(),
                view.updatedAt(),
                view.version());
    }
}
