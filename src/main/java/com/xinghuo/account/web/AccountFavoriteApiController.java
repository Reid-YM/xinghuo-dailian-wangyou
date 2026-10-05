package com.xinghuo.account.web;

import com.xinghuo.account.dto.AccountDtos;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.Comment;
import com.xinghuo.account.service.CommentService;
import com.xinghuo.account.service.FavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 收藏。 */
@RestController
@RequestMapping("/api/account/favorites")
public class AccountFavoriteApiController {

    private final FavoriteService favorites;

    public AccountFavoriteApiController(FavoriteService favorites) {
        this.favorites = favorites;
    }

    @GetMapping
    @RequireAccount
    public List<AccountDtos.FavoriteItem> list(@RequestParam(required = false) String type,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        Comment.TargetType targetType = type == null || type.isBlank()
                ? null : CommentService.parseTargetType(type);
        return favorites.list(account.getId(), targetType, page, size).stream()
                .map(f -> new AccountDtos.FavoriteItem(
                        f.getTargetType().name(), f.getTargetId(), f.getCreatedAt()))
                .toList();
    }

    /** 切换收藏。用 toggle 而不是 POST/DELETE 两个端点：前端一个按钮即可完成两种操作。 */
    @PostMapping("/toggle")
    @RequireAccount
    public AccountDtos.FavoriteToggleResponse toggle(
            @Valid @RequestBody AccountDtos.FavoriteToggleRequest body,
            HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        boolean favorited = favorites.toggle(account,
                CommentService.parseTargetType(body.targetType()), body.targetId());
        return new AccountDtos.FavoriteToggleResponse(favorited, favorites.count(account.getId()));
    }
}
