package com.xinghuo.repository;

import com.xinghuo.model.AdminTestFeedback;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminTestFeedbackRepository
extends JpaRepository<AdminTestFeedback, Long> {
    public List<AdminTestFeedback> findTop100ByOrderByUpdatedAtDesc();

    public long countByStatus(AdminTestFeedback.Status var1);
}

