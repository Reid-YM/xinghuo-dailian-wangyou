package com.xinghuo.slot.application;

import java.util.List;
import org.springframework.stereotype.Service;

/** Application boundary for home-page recommendation slots. */
@Service
public class SlotApplicationService {
    private final SlotPort port;

    public SlotApplicationService(SlotPort port) {
        this.port = port;
    }

    public List<SlotPublicServiceView> publicHotServices() {
        return port.publicHotServices();
    }

    public List<SlotView> list() {
        return port.list();
    }

    public SlotView update(UpdateSlotCommand command, String operator) {
        return port.update(command, operator);
    }
}
