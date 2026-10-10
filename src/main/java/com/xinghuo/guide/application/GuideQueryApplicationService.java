package com.xinghuo.guide.application;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GuideQueryApplicationService {
    private final GuideQueryPort port;

    public GuideQueryApplicationService(GuideQueryPort port) {
        this.port = port;
    }

    public List<GuideQueryModels.GuideSummary> publicList(String gameCode, String query, int limit) {
        return port.publicList(gameCode, query, limit);
    }

    public GuideQueryModels.GuideDetail publicDetail(String slug) {
        return port.publicDetail(slug);
    }

    public List<GuideQueryModels.GuideSummary> adminList() {
        return port.adminList();
    }

    public GuideQueryModels.GuideDetail adminDetail(Long id) {
        return port.adminDetail(id);
    }
}
