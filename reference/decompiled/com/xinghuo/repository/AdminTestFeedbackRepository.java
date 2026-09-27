/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.AdminTestFeedback
 *  com.xinghuo.model.AdminTestFeedback$Status
 *  com.xinghuo.repository.AdminTestFeedbackRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.xinghuo.repository;

import com.xinghuo.model.AdminTestFeedback;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminTestFeedbackRepository
extends JpaRepository<AdminTestFeedback, Long> {
    public List<AdminTestFeedback> findTop100ByOrderByUpdatedAtDesc();

    public long countByStatus(AdminTestFeedback.Status var1);
}

