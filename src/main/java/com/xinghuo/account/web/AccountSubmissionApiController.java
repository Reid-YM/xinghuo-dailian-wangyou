package com.xinghuo.account.web;

import com.xinghuo.account.dto.AccountDtos;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.service.SubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用户攻略投稿。 */
@RestController
@RequestMapping("/api/account/submissions")
public class AccountSubmissionApiController {

    private final SubmissionService submissions;

    public AccountSubmissionApiController(SubmissionService submissions) {
        this.submissions = submissions;
    }

    @GetMapping
    @RequireAccount
    public List<AccountDtos.SubmissionItem> list(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        return submissions.listMine(account.getId(), page, size).stream()
                .map(AccountDtos.SubmissionItem::of)
                .toList();
    }

    @GetMapping("/{id}")
    @RequireAccount
    public AccountDtos.SubmissionDetail detail(@PathVariable Long id, HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        return AccountDtos.SubmissionDetail.of(submissions.getOwn(account.getId(), id));
    }

    @PostMapping
    @RequireAccount
    public AccountDtos.SubmissionItem create(
            @Valid @RequestBody AccountDtos.SubmissionCreateRequest body,
            HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        return AccountDtos.SubmissionItem.of(submissions.create(account,
                body.gameCode(), body.title(), body.summary(),
                body.contentMd(), body.coverImage(), request));
    }

    @PostMapping("/{id}/withdraw")
    @RequireAccount
    public AccountDtos.OkResponse withdraw(@PathVariable Long id, HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        submissions.withdraw(account.getId(), id, request);
        return AccountDtos.OkResponse.of("投稿已撤回");
    }
}
