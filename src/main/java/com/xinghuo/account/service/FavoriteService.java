package com.xinghuo.account.service;

import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.Comment;
import com.xinghuo.account.model.Favorite;
import com.xinghuo.account.repo.FavoriteRepository;
import java.time.Duration;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 收藏。 */
@Service
public class FavoriteService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_FAVORITES = 500;

    private final FavoriteRepository favorites;
    private final RateLimiter rateLimiter;

    public FavoriteService(FavoriteRepository favorites, RateLimiter rateLimiter) {
        this.favorites = favorites;
        this.rateLimiter = rateLimiter;
    }

    /**
     * 切换收藏状态。
     *
     * @return true 表示操作后为「已收藏」
     */
    @Transactional
    public boolean toggle(Account account, Comment.TargetType targetType, String targetId) {
        // 切换是幂等的读写混合操作，正常使用不会高频；限制频率只为挡住脚本刷。
        rateLimiter.check("favorite:" + account.getId(), 60, Duration.ofMinutes(1));

        return favorites.findByUserIdAndTargetTypeAndTargetId(
                        account.getId(), targetType, targetId)
                .map(existing -> {
                    favorites.delete(existing);
                    return false;
                })
                .orElseGet(() -> {
                    if (favorites.countByUserId(account.getId()) >= MAX_FAVORITES) {
                        throw AccountException.badRequest("FAVORITE_QUOTA_EXCEEDED",
                                "收藏数量已达上限（" + MAX_FAVORITES + " 条）");
                    }
                    favorites.save(Favorite.builder()
                            .userId(account.getId())
                            .targetType(targetType)
                            .targetId(targetId)
                            .build());
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(Long userId, Comment.TargetType targetType, String targetId) {
        return favorites.existsByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
    }

    @Transactional(readOnly = true)
    public List<Favorite> list(Long userId, Comment.TargetType targetType, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page),
                Math.min(Math.max(1, size), MAX_PAGE_SIZE));
        if (targetType == null) {
            return favorites.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return favorites.findByUserIdAndTargetTypeOrderByCreatedAtDesc(userId, targetType, pageable);
    }

    @Transactional(readOnly = true)
    public long count(Long userId) {
        return favorites.countByUserId(userId);
    }
}
