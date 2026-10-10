package com.xinghuo.audit.application;

import java.util.List;

public interface AuditQueryPort {
    List<AuditEntryView> latest100();
}
