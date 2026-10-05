package com.xinghuo.guide;

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
            GuideModels.GuideImportResult result = guides.importGames(
                    new GuideModels.GuideImportRequest(true, false, "P0-03 首次启动自动生成多游戏攻略"),
                    "system",
                    null
            );
            log.info("P0-03 guide bootstrap complete: candidates={}, created={}, refreshed={}, skipped={}",
                    result.totalCandidates(), result.created(), result.refreshed(), result.skipped());
        };
    }
}