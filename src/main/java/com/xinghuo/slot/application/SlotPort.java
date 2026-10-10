package com.xinghuo.slot.application;

import java.util.List;

/** Application-facing slot persistence/selection contract. */
public interface SlotPort {
    List<SlotPublicServiceView> publicHotServices();

    List<SlotView> list();

    SlotView update(UpdateSlotCommand command, String operator);
}
