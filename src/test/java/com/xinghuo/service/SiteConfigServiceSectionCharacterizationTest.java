package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.SiteConfig;
import com.xinghuo.repository.ServiceItemRepository;
import com.xinghuo.repository.SiteConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SiteConfigServiceSectionCharacterizationTest {
    @Mock
    private SiteConfigRepository repo;
    @Mock
    private ServiceItemRepository serviceRepo;
    @Mock
    private SlotService slotService;
    @Mock
    private AuditService audit;
    @Mock
    private GameStatusService gameStatuses;
    @Mock
    private SiteSettingService siteSettings;

    private SiteConfigService service;

    @BeforeEach
    void setUp() {
        service = new SiteConfigService(repo, serviceRepo, slotService, audit, gameStatuses, siteSettings);
    }

    @Test
    void invalidSectionFailsBeforeSyncTimestampAndAudit() {
        SiteConfig config = mock(SiteConfig.class);
        ApiDtos.SiteConfigResponse before = mock(ApiDtos.SiteConfigResponse.class);
        SiteConfigService subject = spy(service);
        doReturn(config).when(subject).get();
        doReturn(before).when(subject).publicConfig();

        assertThrows(BadRequestException.class,
                () -> subject.updateSection("missing", Map.of(), "admin", null));

        verify(config, never()).setUpdatedAt(any(Instant.class));
        verifyNoInteractions(siteSettings, audit);
    }

    @Test
    void visualSectionRejectsOutOfRangeHeightBeforeSyncAndAudit() {
        SiteConfig config = mock(SiteConfig.class);
        ApiDtos.SiteConfigResponse before = mock(ApiDtos.SiteConfigResponse.class);
        SiteConfigService subject = spy(service);
        doReturn(config).when(subject).get();
        doReturn(before).when(subject).publicConfig();

        assertThrows(BadRequestException.class,
                () -> subject.updateSection(
                        "visual",
                        Map.of("heroImageHeight", 421),
                        "admin",
                        null));

        verify(config).setHeroImage(null);
        verify(config, never()).setHeroImageHeight(421);
        verify(config, never()).setUpdatedAt(any(Instant.class));
        verifyNoInteractions(siteSettings, audit);
    }

    @Test
    void homeSectionTrimsDropsBlankAndLimitsTrustTagsToSix() {
        SiteConfig config = mock(SiteConfig.class);
        when(config.getHeroTitle()).thenReturn("Hero");
        when(config.getHeroSubtitle()).thenReturn("Subtitle");
        when(config.getStartingPriceMode()).thenReturn(SiteConfig.StartingPriceMode.MANUAL);
        when(config.getStartingPrice()).thenReturn(BigDecimal.TEN);
        when(config.isShowStartingPrice()).thenReturn(true);
        when(config.getHomeServiceLimit()).thenReturn(6);

        HttpServletRequest request = mock(HttpServletRequest.class);
        ApiDtos.SiteConfigResponse before = mock(ApiDtos.SiteConfigResponse.class);
        ApiDtos.SiteConfigResponse after = mock(ApiDtos.SiteConfigResponse.class);
        SiteConfigService subject = spy(service);
        doReturn(config).when(subject).get();
        doReturn(before, after).when(subject).publicConfig();

        Map<String, Object> payload = new HashMap<>();
        payload.put("trustTags", List.of(" 可靠 ", "", "纯绿", " 手打 ", "进度可查", "沟通", "安全", "第七个"));

        ApiDtos.SiteConfigResponse result = subject.updateSection(" HOME ", payload, "admin", request);

        assertSame(after, result);
        verify(config).setTrustTags("可靠|纯绿|手打|进度可查|沟通|安全");
        verify(config).setUpdatedAt(any(Instant.class));
        verify(audit).log(
                eq("SITE_CONFIG_HOME"),
                eq("SITE_CONFIG"),
                eq("home"),
                eq("admin"),
                eq(before),
                eq(after),
                eq("后台分区保存"),
                eq(request));
    }
}
