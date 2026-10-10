package com.xinghuo.siteconfig.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xinghuo.model.ServiceItem;
import com.xinghuo.model.SiteConfig;
import com.xinghuo.repository.ServiceItemRepository;
import com.xinghuo.repository.SiteConfigRepository;
import com.xinghuo.siteconfig.application.SiteConfigPublicSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SiteConfigPublicPersistenceAdapterTest {

    @Test
    void mapsAutoPriceAndEncodedPublicLists() {
        SiteConfigRepository repo = mock(SiteConfigRepository.class);
        ServiceItemRepository services = mock(ServiceItemRepository.class);
        SiteConfig config = baseConfig();
        when(config.getStartingPriceMode()).thenReturn(SiteConfig.StartingPriceMode.AUTO);
        when(config.getStartingPrice()).thenReturn(new BigDecimal("99.00"));
        when(config.getTrustTags()).thenReturn("可靠|纯绿|手打");
        when(config.getTrustCases()).thenReturn("案例\t标题\t指标\t详情");
        when(config.getTrustFaq()).thenReturn("问题\t回答");
        when(config.getContactChannels()).thenReturn("主客服\tWX100\t/qr.png\t备注\thttps://example.com");
        when(repo.findById(1L)).thenReturn(Optional.of(config));

        ServiceItem fixed = mock(ServiceItem.class);
        when(fixed.getPriceMode()).thenReturn(ServiceItem.PriceMode.FIXED);
        when(fixed.getPriceFixed()).thenReturn(new BigDecimal("35.00"));
        ServiceItem range = mock(ServiceItem.class);
        when(range.getPriceMode()).thenReturn(ServiceItem.PriceMode.RANGE);
        when(range.getPriceMin()).thenReturn(new BigDecimal("20.00"));
        ServiceItem quote = mock(ServiceItem.class);
        when(quote.getPriceMode()).thenReturn(ServiceItem.PriceMode.QUOTE);
        when(services.findPublic(any(Instant.class))).thenReturn(List.of(fixed, range, quote));

        SiteConfigPublicSnapshot out = new SiteConfigPublicPersistenceAdapter(repo, services).load();

        assertEquals(new BigDecimal("20.00"), out.startingPrice());
        assertEquals("auto", out.startingPriceMode());
        assertEquals("comfortable", out.serviceCardDensity());
        assertEquals("compact", out.serviceCardMobileDensity());
        assertEquals(List.of("可靠", "纯绿", "手打"), out.trustTags());
        assertEquals("案例", out.trustCases().getFirst().tag());
        assertEquals("问题", out.trustFaq().getFirst().question());
        assertEquals("WX100", out.contactChannels().getFirst().wechatId());
    }

    @Test
    void preservesLegacyFallbacksWhenEncodedListsAreEmpty() {
        SiteConfigRepository repo = mock(SiteConfigRepository.class);
        ServiceItemRepository services = mock(ServiceItemRepository.class);
        SiteConfig config = baseConfig();
        when(config.getStartingPriceMode()).thenReturn(SiteConfig.StartingPriceMode.MANUAL);
        when(config.getStartingPrice()).thenReturn(new BigDecimal("66.00"));
        when(repo.findById(1L)).thenReturn(Optional.of(config));

        SiteConfigPublicSnapshot out = new SiteConfigPublicPersistenceAdapter(repo, services).load();

        assertEquals(new BigDecimal("66.00"), out.startingPrice());
        assertEquals(4, out.trustCases().size());
        assertEquals(8, out.trustFaq().size());
        assertEquals("主客服", out.contactChannels().getFirst().label());
        assertEquals("AAC9878", out.contactChannels().getFirst().wechatId());
    }

    private static SiteConfig baseConfig() {
        SiteConfig config = mock(SiteConfig.class);
        when(config.getServiceCardDensity()).thenReturn("COMFORTABLE");
        when(config.getServiceCardMobileDensity()).thenReturn("COMPACT");
        when(config.getHeroImageFit()).thenReturn("COVER");
        when(config.getHeroImageMobileFit()).thenReturn("CONTAIN");
        when(config.getHeroBackgroundFit()).thenReturn("COVER");
        when(config.getHeroBackgroundMobileFit()).thenReturn("FILL");
        when(config.getServiceCoverFit()).thenReturn("COVER");
        when(config.getServiceCoverMobileFit()).thenReturn("CONTAIN");
        return config;
    }
}
