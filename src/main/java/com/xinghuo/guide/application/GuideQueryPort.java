package com.xinghuo.guide.application;

import java.util.List;

public interface GuideQueryPort {
    List<GuideQueryModels.GuideSummary> publicList(String gameCode, String query, int limit);

    GuideQueryModels.GuideDetail publicDetail(String slug);

    List<GuideQueryModels.GuideSummary> adminList();

    GuideQueryModels.GuideDetail adminDetail(Long id);
}
