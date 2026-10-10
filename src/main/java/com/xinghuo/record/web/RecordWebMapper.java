package com.xinghuo.record.web;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.record.application.RecordPublicView;

/** Maps application-owned record projections to the established public API contract. */
public final class RecordWebMapper {
    private RecordWebMapper() {
    }

    public static ApiDtos.RecordPublicResponse toResponse(RecordPublicView view) {
        return new ApiDtos.RecordPublicResponse(
                view.businessNo(),
                view.stage(),
                view.status(),
                view.title(),
                view.expectedTime(),
                view.progressPercent(),
                view.progressText(),
                view.createdAt(),
                view.updatedAt(),
                view.hasProgressImage(),
                view.customerNote());
    }
}
