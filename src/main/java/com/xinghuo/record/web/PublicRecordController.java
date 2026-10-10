package com.xinghuo.record.web;

import com.xinghuo.account.service.RateLimiter;
import com.xinghuo.account.service.RequestUtils;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.record.application.RecordApplicationService;
import com.xinghuo.siteconfig.application.SiteRuntimeApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/records"})
public class PublicRecordController {
    private static final int REQUEST_BUCKET_PER_MINUTE = 60;
    private final SiteRuntimeApplicationService runtimeConfig;
    private final RecordApplicationService records;
    private final RateLimiter rateLimiter;

    @GetMapping(value={"/{businessNo}"})
    ApiDtos.RecordPublicResponse record(@PathVariable String businessNo, HttpServletRequest req) {
        if (!this.runtimeConfig.orderQueryEnabled()) {
            throw new BadRequestException("进度查询正在维护，请联系客服查询");
        }
        this.limitByIp(req, "pub:record");
        return RecordWebMapper.toResponse(this.records.get(businessNo));
    }

    private void limitByIp(HttpServletRequest req, String bucket) {
        String ip = RequestUtils.clientIp(req);
        this.rateLimiter.check(bucket + ":" + (ip == null ? "unknown" : ip),
                REQUEST_BUCKET_PER_MINUTE, Duration.ofMinutes(1L));
    }

    @Generated
    public PublicRecordController(SiteRuntimeApplicationService runtimeConfig,
                                  RecordApplicationService records,
                                  RateLimiter rateLimiter) {
        this.runtimeConfig = runtimeConfig;
        this.records = records;
        this.rateLimiter = rateLimiter;
    }
}
