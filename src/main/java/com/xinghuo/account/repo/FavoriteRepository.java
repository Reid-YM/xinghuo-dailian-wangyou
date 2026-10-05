package com.xinghuo.account.repo;

import com.xinghuo.account.model.Comment;
import com.xinghuo.account.model.Favorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndTargetTypeAndTargetId(
            Long userId, Comment.TargetType targetType, String targetId);

    boolean existsByUserIdAndTargetTypeAndTargetId(
            Long userId, Comment.TargetType targetType, String targetId);

    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Favorite> findByUserIdAndTargetTypeOrderByCreatedAtDesc(
            Long userId, Comment.TargetType targetType, Pageable pageable);

    long countByUserId(Long userId);
}
