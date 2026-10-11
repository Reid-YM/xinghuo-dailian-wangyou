package com.xinghuo.tracking.web;

import com.xinghuo.tracking.application.TrackingApplicationService;
import com.xinghuo.util.HashUtil;
import com.xinghuo.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Generated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/track"})
public class PublicTrackingController {
    private final TrackingApplicationService tracking;

    @PostMapping
    ResponseEntity<Void> track(@Valid @RequestBody TrackRequest body, HttpServletRequest req) {
        this.tracking.track(TrackingWebMapper.toCommand(
                body, HashUtil.sha256(RequestUtil.clientIp(req))));
        return ResponseEntity.noContent().build();
    }

    @Generated
    public PublicTrackingController(TrackingApplicationService tracking) {
        this.tracking = tracking;
    }
}
