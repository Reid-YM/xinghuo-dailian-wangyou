/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.guide.GuideContentService
 *  com.xinghuo.guide.GuideModels$GuideDetail
 *  com.xinghuo.guide.GuideModels$GuideSummary
 *  com.xinghuo.guide.PublicGuideController
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.xinghuo.guide;

import com.xinghuo.guide.GuideContentService;
import com.xinghuo.guide.GuideModels;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/guides"})
public class PublicGuideController {
    private final GuideContentService guides;

    public PublicGuideController(GuideContentService guides) {
        this.guides = guides;
    }

    @GetMapping
    List<GuideModels.GuideSummary> list(@RequestParam(required=false) String game, @RequestParam(required=false) String q, @RequestParam(defaultValue="100") int limit) {
        return this.guides.publicList(game, q, limit);
    }

    @GetMapping(value={"/{slug}"})
    GuideModels.GuideDetail detail(@PathVariable String slug) {
        return this.guides.publicDetail(slug);
    }
}

