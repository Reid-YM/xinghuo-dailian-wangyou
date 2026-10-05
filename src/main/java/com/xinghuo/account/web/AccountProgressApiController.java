package com.xinghuo.account.web;

import com.xinghuo.account.dto.AccountDtos;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.ProgressEntry;
import com.xinghuo.account.service.AccountException;
import com.xinghuo.account.service.ProgressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 攻略进度云同步。 */
@RestController
@RequestMapping("/api/account/progress")
public class AccountProgressApiController {

    private final ProgressService progress;

    public AccountProgressApiController(ProgressService progress) {
        this.progress = progress;
    }

    @GetMapping
    @RequireAccount
    public List<AccountDtos.ProgressMeta> list(HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        return progress.list(account.getId()).stream()
                .map(e -> new AccountDtos.ProgressMeta(
                        e.getToolKey(), e.getVersion(), e.getUpdatedAt()))
                .toList();
    }

    @GetMapping("/{toolKey}")
    @RequireAccount
    public AccountDtos.ProgressResponse get(@PathVariable String toolKey,
                                            HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        ProgressEntry entry = progress.get(account.getId(), toolKey)
                .orElseThrow(() -> AccountException.notFound("PROGRESS_NOT_FOUND", "该工具还没有同步过进度"));
        return new AccountDtos.ProgressResponse(
                entry.getToolKey(), entry.getVersion(), entry.getPayload(), entry.getUpdatedAt());
    }

    @PutMapping("/{toolKey}")
    @RequireAccount
    public AccountDtos.ProgressResponse put(@PathVariable String toolKey,
                                            @Valid @RequestBody AccountDtos.ProgressPutRequest body,
                                            HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        ProgressEntry entry = progress.put(account.getId(), toolKey, body.version(), body.payload());
        return new AccountDtos.ProgressResponse(
                entry.getToolKey(), entry.getVersion(), entry.getPayload(), entry.getUpdatedAt());
    }

    @DeleteMapping("/{toolKey}")
    @RequireAccount
    public AccountDtos.OkResponse delete(@PathVariable String toolKey, HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        progress.delete(account.getId(), toolKey);
        return AccountDtos.OkResponse.of("已删除该工具的云同步进度");
    }
}
