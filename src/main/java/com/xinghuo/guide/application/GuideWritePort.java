package com.xinghuo.guide.application;

import java.util.List;

public interface GuideWritePort {
    GuideQueryModels.GuideDetail create(GuideWriteModels.GuideUpsertCommand command, String operator);

    GuideQueryModels.GuideDetail update(Long id, GuideWriteModels.GuideUpsertCommand command, String operator);

    GuideQueryModels.GuideDetail setStatus(Long id, String status, String reason, String operator);

    void archive(Long id, String reason, String operator);

    GuideWriteModels.GuideBatchArchiveResult archiveBatch(List<Long> ids, String reason, String operator);

    GuideWriteModels.GuideBatchArchiveResult archiveAll(String reason, String operator);

    GuideWriteModels.GuideImportResult importGames(GuideWriteModels.GuideImportCommand command, String operator);
}
