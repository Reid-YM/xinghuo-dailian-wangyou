package com.xinghuo.account.repo;

import com.xinghuo.account.model.CommentReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
}
