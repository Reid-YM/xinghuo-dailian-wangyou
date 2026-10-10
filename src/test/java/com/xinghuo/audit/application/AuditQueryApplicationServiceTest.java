package com.xinghuo.audit.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditQueryApplicationServiceTest {
    @Test
    void returnsLatestEntriesFromPortUnchanged() {
        Instant createdAt = Instant.parse("2026-10-10T12:00:00Z");
        AuditEntryView entry = new AuditEntryView(
                7L,
                "UPDATE",
                "SERVICE",
                "42",
                "admin",
                "reason",
                "127.0.0.1",
                createdAt);
        AuditQueryApplicationService service = new AuditQueryApplicationService(() -> List.of(entry));

        assertThat(service.latest100()).containsExactly(entry);
    }
}
