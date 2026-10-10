package com.xinghuo.tracking.web;

import com.xinghuo.tracking.application.TrackingAdminApplicationService;
import com.xinghuo.tracking.application.TrackingSummaryView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tracking")
public class AdminTrackingController {
    private final TrackingAdminApplicationService tracking;

    public AdminTrackingController(TrackingAdminApplicationService tracking) {
        this.tracking = tracking;
    }

    @GetMapping("/summary")
    public TrackingSummaryView summary(@RequestParam(name = "days", defaultValue = "7") int days) {
        return tracking.summary(days);
    }
}
