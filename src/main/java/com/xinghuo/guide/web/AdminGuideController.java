package com.xinghuo.guide.web;

import com.xinghuo.guide.application.GuideQueryApplicationService;
import com.xinghuo.guide.application.GuideQueryModels;
import com.xinghuo.guide.application.GuideWriteApplicationService;
import com.xinghuo.guide.application.GuideWriteModels;
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
    private final GuideQueryApplicationService queries;
    private final GuideWriteApplicationService writes;

    public AdminGuideController(
            GuideQueryApplicationService queries,
            GuideWriteApplicationService writes) {
        this.queries = queries;
        this.writes = writes;
    }

    @GetMapping
    List<GuideQueryModels.GuideSummary> list() {
        return queries.adminList();
    }

    @GetMapping("/{id}")
    GuideQueryModels.GuideDetail detail(@PathVariable Long id) {
        return queries.adminDetail(id);
    }

    @PostMapping
    GuideQueryModels.GuideDetail create(@RequestBody GuideAdminRequests.GuideUpsert body, Authentication auth) {
        return writes.create(GuideWriteWebMapper.toCommand(body), operator(auth));
    }

    @PutMapping("/{id}")
    GuideQueryModels.GuideDetail update(
            @PathVariable Long id,
            @RequestBody GuideAdminRequests.GuideUpsert body,
            Authentication auth) {
        return writes.update(id, GuideWriteWebMapper.toCommand(body), operator(auth));
    }

    @PatchMapping("/{id}/status")
    GuideQueryModels.GuideDetail status(
            @PathVariable Long id,
            @RequestBody GuideAdminRequests.GuideStatusInput body,
            Authentication auth) {
        return writes.setStatus(id, body.status(), body.reason(), operator(auth));
    }

    @DeleteMapping("/{id}")
    void archive(@PathVariable Long id, Authentication auth) {
        writes.archive(id, "后台归档攻略", operator(auth));
    }

    @DeleteMapping("/batch")
    GuideWriteModels.GuideBatchArchiveResult archiveBatch(
            @RequestBody GuideAdminRequests.GuideBatchArchiveRequest body,
            Authentication auth) {
        String reason = body == null || body.reason() == null || body.reason().isBlank()
                ? "后台批量归档攻略"
                : body.reason().trim();
        List<Long> ids = body == null ? List.of() : body.ids();
        return writes.archiveBatch(ids, reason, operator(auth));
    }

    @PostMapping("/archive-all")
    GuideWriteModels.GuideBatchArchiveResult archiveAll(
            @RequestBody(required = false) GuideAdminRequests.GuideBatchArchiveRequest body,
            Authentication auth) {
        String reason = body == null || body.reason() == null || body.reason().isBlank()
                ? "后台全部归档攻略"
                : body.reason().trim();
        return writes.archiveAll(reason, operator(auth));
    }

    @PostMapping("/import-games")
    GuideWriteModels.GuideImportResult importGames(
            @RequestBody(required = false) GuideAdminRequests.GuideImportRequest body,
            Authentication auth) {
        GuideAdminRequests.GuideImportRequest actual = body == null
                ? new GuideAdminRequests.GuideImportRequest(true, false, "批量生成多游戏攻略")
                : body;
        return writes.importGames(GuideWriteWebMapper.toCommand(actual), operator(auth));
    }

    private String operator(Authentication auth) {
        return auth == null || auth.getName() == null ? "admin" : auth.getName();
    }
}
