package com.xinghuo.guide;

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
@RequestMapping("/api/admin/guides")
public class AdminGuideController {
    private final GuideContentService guides;

    public AdminGuideController(GuideContentService guides) {
        this.guides = guides;
    }

    @GetMapping
    List<GuideModels.GuideSummary> list() {
        return guides.adminList();
    }

    @GetMapping("/{id}")
    GuideModels.GuideDetail detail(@PathVariable Long id) {
        return guides.adminDetail(id);
    }

    @PostMapping
    GuideModels.GuideDetail create(@RequestBody GuideModels.GuideUpsert body, Authentication auth, HttpServletRequest request) {
        return guides.create(body, operator(auth), request);
    }

    @PutMapping("/{id}")
    GuideModels.GuideDetail update(@PathVariable Long id, @RequestBody GuideModels.GuideUpsert body, Authentication auth, HttpServletRequest request) {
        return guides.update(id, body, operator(auth), request);
    }

    @PatchMapping("/{id}/status")
    GuideModels.GuideDetail status(@PathVariable Long id, @RequestBody GuideModels.GuideStatusInput body, Authentication auth, HttpServletRequest request) {
        return guides.setStatus(id, body.status(), body.reason(), operator(auth), request);
    }

    @DeleteMapping("/{id}")
    void archive(@PathVariable Long id, Authentication auth, HttpServletRequest request) {
        guides.archive(id, "后台归档攻略", operator(auth), request);
    }

    @DeleteMapping("/batch")
    GuideModels.GuideBatchArchiveResult archiveBatch(@RequestBody GuideModels.GuideBatchArchiveRequest body, Authentication auth, HttpServletRequest request) {
        String reason = body == null || body.reason() == null || body.reason().isBlank()
                ? "后台批量归档攻略"
                : body.reason().trim();
        List<Long> ids = body == null ? List.of() : body.ids();
        return guides.archiveBatch(ids, reason, operator(auth), request);
    }

    @PostMapping("/archive-all")
    GuideModels.GuideBatchArchiveResult archiveAll(@RequestBody(required = false) GuideModels.GuideBatchArchiveRequest body, Authentication auth, HttpServletRequest request) {
        String reason = body == null || body.reason() == null || body.reason().isBlank()
                ? "后台全部归档攻略"
                : body.reason().trim();
        return guides.archiveAll(reason, operator(auth), request);
    }

    @PostMapping("/import-games")
    GuideModels.GuideImportResult importGames(@RequestBody(required = false) GuideModels.GuideImportRequest body, Authentication auth, HttpServletRequest request) {
        GuideModels.GuideImportRequest actual = body == null ? new GuideModels.GuideImportRequest(true, false, "批量生成多游戏攻略") : body;
        return guides.importGames(actual, operator(auth), request);
    }

    private String operator(Authentication auth) {
        return auth == null || auth.getName() == null ? "admin" : auth.getName();
    }
}
