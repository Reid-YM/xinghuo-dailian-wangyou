package com.xinghuo.guide;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guides")
public class PublicGuideController {
    private final GuideContentService guides;

    public PublicGuideController(GuideContentService guides) {
        this.guides = guides;
    }

    @GetMapping
    List<GuideModels.GuideSummary> list(
            @RequestParam(required = false) String game,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "featured") String sort
    ) {
        return guides.publicList(game, q, limit, sort);
    }

    @GetMapping("/{slug}")
    GuideModels.GuideDetail detail(@PathVariable String slug) {
        return guides.publicDetail(slug);
    }
}