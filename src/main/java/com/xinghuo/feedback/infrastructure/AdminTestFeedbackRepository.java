package com.xinghuo.feedback.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminTestFeedbackRepository extends JpaRepository<AdminTestFeedback, Long> {
    List<AdminTestFeedback> findTop100ByOrderByUpdatedAtDesc();

    long countByStatus(AdminTestFeedback.Status status);
}
