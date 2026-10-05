package com.xinghuo.account.web;

import com.xinghuo.account.dto.AccountDtos;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.Comment;
import com.xinghuo.account.service.AccountNameLookup;
import com.xinghuo.account.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 评论：公开读取，登录后发表。 */
@RestController
@RequestMapping("/api/account/comments")
public class AccountCommentApiController {

    private final CommentService comments;
    private final AccountNameLookup names;

    public AccountCommentApiController(CommentService comments, AccountNameLookup names) {
        this.comments = comments;
        this.names = names;
    }

    /** 公开列表：不需要登录即可查看已发布评论。 */
    @GetMapping
    public List<AccountDtos.CommentItem> list(@RequestParam String targetType,
                                              @RequestParam String targetId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              HttpServletRequest request) {
        Comment.TargetType type = CommentService.parseTargetType(targetType);
        List<Comment> rows = comments.listPublished(type, targetId, page, size);
        Map<Long, String> nameMap = names.names(rows.stream().map(Comment::getUserId).toList());
        Long viewerId = AccountAuthInterceptor.currentAccount(request)
                .map(Account::getId).orElse(null);
        return rows.stream()
                .map(c -> AccountDtos.CommentItem.of(c, nameMap.get(c.getUserId()), viewerId))
                .toList();
    }

    /**
     * 当前用户在该目标下的评论（含待审）。
     *
     * <p>公开列表只返回已发布内容，若不给用户看自己的待审评论，
     * 用户会以为提交失败而反复重发。
     */
    @GetMapping("/mine")
    @RequireAccount
    public List<AccountDtos.CommentItem> mine(@RequestParam String targetType,
                                              @RequestParam String targetId,
                                              HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        return comments.listMineForTarget(account.getId(),
                        CommentService.parseTargetType(targetType), targetId).stream()
                .map(c -> AccountDtos.CommentItem.of(c, account.getDisplayName(), account.getId()))
                .toList();
    }

    @PostMapping
    @RequireAccount
    public AccountDtos.CommentItem create(@Valid @RequestBody AccountDtos.CommentCreateRequest body,
                                          HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        Comment comment = comments.create(account,
                CommentService.parseTargetType(body.targetType()), body.targetId(),
                body.parentId(), body.content(), request);
        return AccountDtos.CommentItem.of(comment, account.getDisplayName(), account.getId());
    }

    @DeleteMapping("/{id}")
    @RequireAccount
    public AccountDtos.OkResponse delete(@PathVariable Long id, HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        comments.deleteOwn(account.getId(), id);
        return AccountDtos.OkResponse.of("评论已删除");
    }

    @PostMapping("/{id}/report")
    @RequireAccount
    public AccountDtos.OkResponse report(@PathVariable Long id,
                                         @Valid @RequestBody AccountDtos.CommentReportRequest body,
                                         HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        comments.report(account, id, body.reason());
        return AccountDtos.OkResponse.of("举报已提交，我们会尽快处理");
    }
}
