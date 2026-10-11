package com.xinghuo.record.web;

import com.xinghuo.record.application.RecordPublicView;

/** Maps application-owned record projections to the established public API contract. */
public final class RecordWebMapper {
    private RecordWebMapper() {
    }

    public static RecordPublicResponse toResponse(RecordPublicView view) {
        return new RecordPublicResponse(
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
