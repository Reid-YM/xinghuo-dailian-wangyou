/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.guide.GuideBootstrapConfig
 *  com.xinghuo.guide.GuideContentService
 *  com.xinghuo.guide.GuideModels$GuideImportRequest
 *  com.xinghuo.guide.GuideModels$GuideImportResult
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.CommandLineRunner
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package com.xinghuo.guide;

import com.xinghuo.guide.GuideContentService;
import com.xinghuo.guide.GuideModels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GuideBootstrapConfig {
    private static final Logger log = LoggerFactory.getLogger(GuideBootstrapConfig.class);

    @Bean
    CommandLineRunner bootstrapMultiGameGuides(GuideContentService guides) {
        return args -> {
            if (!guides.adminList().isEmpty()) {
                return;
            }
            GuideModels.GuideImportResult result = guides.importGames(new GuideModels.GuideImportRequest(Boolean.valueOf(true), Boolean.valueOf(false), "P0-03 \u9996\u6b21\u542f\u52a8\u81ea\u52a8\u751f\u6210\u591a\u6e38\u620f\u653b\u7565"), "system", null);
            log.info("P0-03 guide bootstrap complete: candidates={}, created={}, refreshed={}, skipped={}", new Object[]{result.totalCandidates(), result.created(), result.refreshed(), result.skipped()});
        };
    }
}

