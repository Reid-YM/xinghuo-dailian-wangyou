package com.xinghuo.slot.web;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.slot.application.SlotApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Generated;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/admin/slots"})
public class AdminSlotController {
    private final SlotApplicationService slots;

    @GetMapping
    List<ApiDtos.SlotResponse> slots() {
        return SlotWebMapper.toSlotResponses(this.slots.list());
    }

    @PatchMapping
    ApiDtos.SlotResponse updateSlot(@Valid @RequestBody ApiDtos.SlotUpdate r, Authentication a, HttpServletRequest req) {
        return SlotWebMapper.toResponse(
                this.slots.update(SlotWebMapper.toCommand(r), a.getName()));
    }

    @Generated
    public AdminSlotController(SlotApplicationService slots) {
        this.slots = slots;
    }
}
