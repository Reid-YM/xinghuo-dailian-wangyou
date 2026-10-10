package com.xinghuo.guide.application;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GuideWriteApplicationService {
    private final GuideWritePort port;

    public GuideWriteApplicationService(GuideWritePort port) {
        this.port = port;
    }

    public GuideQueryModels.GuideDetail create(GuideWriteModels.GuideUpsertCommand command, String operator) {
        return port.create(command, operator);
    }

    public GuideQueryModels.GuideDetail update(Long id, GuideWriteModels.GuideUpsertCommand command, String operator) {
        return port.update(id, command, operator);
    }

    public GuideQueryModels.GuideDetail setStatus(Long id, String status, String reason, String operator) {
        return port.setStatus(id, status, reason, operator);
    }

    public void archive(Long id, String reason, String operator) {
        port.archive(id, reason, operator);
    }

    public GuideWriteModels.GuideBatchArchiveResult archiveBatch(List<Long> ids, String reason, String operator) {
        return port.archiveBatch(ids, reason, operator);
    }

    public GuideWriteModels.GuideBatchArchiveResult archiveAll(String reason, String operator) {
        return port.archiveAll(reason, operator);
    }

    public GuideWriteModels.GuideImportResult importGames(GuideWriteModels.GuideImportCommand command, String operator) {
        return port.importGames(command, operator);
    }
}
