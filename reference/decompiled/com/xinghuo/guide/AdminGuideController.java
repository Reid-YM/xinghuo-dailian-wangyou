/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.guide.AdminGuideController
 *  com.xinghuo.guide.GuideContentService
 *  com.xinghuo.guide.GuideModels$GuideDetail
 *  com.xinghuo.guide.GuideModels$GuideImportRequest
 *  com.xinghuo.guide.GuideModels$GuideImportResult
 *  com.xinghuo.guide.GuideModels$GuideStatusInput
 *  com.xinghuo.guide.GuideModels$GuideSummary
 *  com.xinghuo.guide.GuideModels$GuideUpsert
 *  jakarta.servlet.http.HttpServletRequest
 *  org.springframework.security.core.Authentication
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PatchMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.xinghuo.guide;

import com.xinghuo.guide.GuideContentService;
import com.xinghuo.guide.GuideModels;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/admin/guides"})
public class AdminGuideController {
    private final GuideContentService guides;

    public AdminGuideController(GuideContentService guides) {
        this.guides = guides;
    }

    @GetMapping
    List<GuideModels.GuideSummary> list() {
        return this.guides.adminList();
    }

    @GetMapping(value={"/{id}"})
    GuideModels.GuideDetail detail(@PathVariable Long id) {
        return this.guides.adminDetail(id);
    }

    @PostMapping
    GuideModels.GuideDetail create(@RequestBody GuideModels.GuideUpsert body, Authentication auth, HttpServletRequest request) {
        return this.guides.create(body, this.operator(auth), request);
    }

    @PutMapping(value={"/{id}"})
    GuideModels.GuideDetail update(@PathVariable Long id, @RequestBody GuideModels.GuideUpsert body, Authentication auth, HttpServletRequest request) {
        return this.guides.update(id, body, this.operator(auth), request);
    }

    @PatchMapping(value={"/{id}/status"})
    GuideModels.GuideDetail status(@PathVariable Long id, @RequestBody GuideModels.GuideStatusInput body, Authentication auth, HttpServletRequest request) {
        return this.guides.setStatus(id, body.status(), body.reason(), this.operator(auth), request);
    }

    @DeleteMapping(value={"/{id}"})
    void archive(@PathVariable Long id, Authentication auth, HttpServletRequest request) {
        this.guides.archive(id, "\u540e\u53f0\u5f52\u6863\u653b\u7565", this.operator(auth), request);
    }

    @PostMapping(value={"/import-games"})
    GuideModels.GuideImportResult importGames(@RequestBody(required=false) GuideModels.GuideImportRequest body, Authentication auth, HttpServletRequest request) {
        GuideModels.GuideImportRequest actual = body == null ? new GuideModels.GuideImportRequest(Boolean.valueOf(true), Boolean.valueOf(false), "\u6279\u91cf\u751f\u6210\u591a\u6e38\u620f\u653b\u7565") : body;
        return this.guides.importGames(actual, this.operator(auth), request);
    }

    private String operator(Authentication auth) {
        return auth == null || auth.getName() == null ? "admin" : auth.getName();
    }
}

