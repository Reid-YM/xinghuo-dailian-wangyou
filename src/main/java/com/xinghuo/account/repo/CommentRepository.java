package com.xinghuo.account.repo;

import com.xinghuo.account.model.Comment;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 对外的评论列表：只取已发布。 */
    List<Comment> findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtAsc(
            Comment.TargetType targetType, String targetId, Comment.CommentStatus status,
            Pageable pageable);

    long countByTargetTypeAndTargetIdAndStatus(
            Comment.TargetType targetType, String targetId, Comment.CommentStatus status);

    /** 后台审核列表。 */
    List<Comment> findByStatusOrderByCreatedAtAsc(Comment.CommentStatus status, Pageable pageable);

    /** 用户自己的评论（含待审与驳回，便于用户看到状态）。 */
    List<Comment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** 用户自己在某个目标下的评论（含待审），用于内容页展示「待审核」状态。 */
    List<Comment> findByUserIdAndTargetTypeAndTargetIdOrderByCreatedAtAsc(
            Long userId, Comment.TargetType targetType, String targetId);

    /** 反垃圾：统计某用户在某时间点之后的发帖量。 */
    long countByUserIdAndCreatedAtAfter(Long userId, Instant since);

    long countByCreatedAtAfter(Instant since);

    @Modifying
    @Query("update Comment c set c.reportCount = c.reportCount + 1 where c.id = :id")
    int incrementReportCount(@Param("id") Long id);
}
