package com.xinghuo.mobilevisual.application;

import org.springframework.stereotype.Service;

@Service
public class MobileVisualConfigApplicationService {
    private final MobileVisualConfigPort port;

    public MobileVisualConfigApplicationService(MobileVisualConfigPort port) {
        this.port = port;
    }

    public MobileVisualConfigView get() {
        return port.get();
    }

    public MobileVisualConfigView update(UpdateMobileVisualConfigCommand command) {
        return port.update(command);
    }
}
