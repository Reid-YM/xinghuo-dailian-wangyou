package com.xinghuo.dashboard.web;

import com.xinghuo.dashboard.application.DashboardApplicationService;
import com.xinghuo.dto.ApiDtos;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/admin"})
public class AdminDashboardController {
    private final DashboardApplicationService dashboard;

    @GetMapping(value={"/dashboard"})
    ApiDtos.DashboardResponse dashboard() {
        return DashboardWebMapper.toResponse(this.dashboard.get());
    }

    @Generated
    public AdminDashboardController(DashboardApplicationService dashboard) {
        this.dashboard = dashboard;
    }
}
