package com.xinghuo.account.repo;

import com.xinghuo.account.model.Submission;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndCreatedAtAfter(Long userId, Instant since);

    long countByUserId(Long userId);

    /**
     * 只统计仍在流程中的投稿。
     *
     * <p>配额判定必须用它而不是 countByUserId：后者会把已发布、已驳回、
     * 已撤回的都算进去，用户累计投满上限后就**永久无法再投稿**，
     * 而提示语还写着「请等待审核结果」。
     */
    long countByUserIdAndStatusIn(Long userId,
                                  java.util.Collection<Submission.SubmissionStatus> statuses);

    List<Submission> findByStatusOrderByCreatedAtAsc(
            Submission.SubmissionStatus status, Pageable pageable);

    long countByStatus(Submission.SubmissionStatus status);
}
