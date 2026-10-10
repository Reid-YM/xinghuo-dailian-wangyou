package com.xinghuo.mobilevisual.application;

public interface MobileVisualConfigPort {
    MobileVisualConfigView get();
    MobileVisualConfigView update(UpdateMobileVisualConfigCommand command);
}
