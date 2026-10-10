package com.xinghuo.guide.web;

import com.xinghuo.guide.application.GuideQueryApplicationService;
import com.xinghuo.guide.application.GuideQueryModels;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guides")
public class PublicGuideController {
    private final GuideQueryApplicationService guides;

    public PublicGuideController(GuideQueryApplicationService guides) {
        this.guides = guides;
    }

    @GetMapping
    List<GuideQueryModels.GuideSummary> list(
            @RequestParam(required = false) String game,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "100") int limit) {
        return guides.publicList(game, q, limit);
    }

    @GetMapping("/{slug}")
    GuideQueryModels.GuideDetail detail(@PathVariable String slug) {
        return guides.publicDetail(slug);
    }
}
