package com.xinghuo.mobilevisual.application;

public interface MobileVisualConfigAuditPort {
    void record(
            MobileVisualConfigView before,
            MobileVisualConfigView after,
            MobileVisualConfigAuditContext context);
}
