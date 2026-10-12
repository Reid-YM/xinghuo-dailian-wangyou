package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SiteConfigServiceCharacterizationTest {
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
    void getAlwaysLoadsSingletonConfigurationWithIdOne() {
        SiteConfig config = mock(SiteConfig.class);
        when(repo.findById(1L)).thenReturn(Optional.of(config));

        assertSame(config, service.get());

        verify(repo).findById(1L);
    }

    @Test
    void quoteAndOrderQueryFlagsReadDirectlyFromSingletonConfiguration() {
        SiteConfig config = mock(SiteConfig.class);
        when(repo.findById(1L)).thenReturn(Optional.of(config));
        when(config.isQuoteEnabled()).thenReturn(true);
        when(config.isOrderQueryEnabled()).thenReturn(false);

        assertTrue(service.isQuoteEnabled());
        assertFalse(service.isOrderQueryEnabled());
    }

    @Test
    void heroImageUpdateTrimsUrlAndAuditsBeforeAndAfterSnapshots() {
        SiteConfig config = mock(SiteConfig.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        ApiDtos.SiteConfigResponse before = mock(ApiDtos.SiteConfigResponse.class);
        ApiDtos.SiteConfigResponse after = mock(ApiDtos.SiteConfigResponse.class);
        SiteConfigService subject = spy(service);
        doReturn(config).when(subject).get();
        doReturn(before, after).when(subject).publicConfig();

        ApiDtos.SiteConfigResponse result = subject.updateSiteImage(
                " hero ", "  /uploads/site/hero.png  ", "admin", request);

        assertSame(after, result);
        verify(config).setHeroImage("/uploads/site/hero.png");
        verify(config).setUpdatedAt(any(Instant.class));
        verify(audit).log(
                eq("SITE_IMAGE_UPDATE"),
                eq("SITE_CONFIG"),
                eq("HERO"),
                eq("admin"),
                eq(before),
                eq(after),
                eq("更新站点图片"),
                eq(request));
    }

    @Test
    void blankBackgroundImageIsNormalizedToNull() {
        SiteConfig config = mock(SiteConfig.class);
        ApiDtos.SiteConfigResponse before = mock(ApiDtos.SiteConfigResponse.class);
        ApiDtos.SiteConfigResponse after = mock(ApiDtos.SiteConfigResponse.class);
        SiteConfigService subject = spy(service);
        doReturn(config).when(subject).get();
        doReturn(before, after).when(subject).publicConfig();

        subject.updateSiteImage("BACKGROUND", "   ", "admin", null);

        verify(config).setHeroBackgroundImage(null);
        verify(audit).log(
                eq("SITE_IMAGE_UPDATE"),
                eq("SITE_CONFIG"),
                eq("BACKGROUND"),
                eq("admin"),
                eq(before),
                eq(after),
                eq("更新站点图片"),
                eq(null));
    }

    @Test
    void invalidImageSlotIsRejectedWithoutMutationOrAudit() {
        SiteConfig config = mock(SiteConfig.class);
        ApiDtos.SiteConfigResponse before = mock(ApiDtos.SiteConfigResponse.class);
        SiteConfigService subject = spy(service);
        doReturn(config).when(subject).get();
        doReturn(before).when(subject).publicConfig();

        assertThrows(BadRequestException.class,
                () -> subject.updateSiteImage("unknown", "/uploads/x.png", "admin", null));

        verify(config, never()).setHeroImage(any());
        verify(config, never()).setHeroBackgroundImage(any());
        verify(config, never()).setWechatQrImage(any());
        verify(config, never()).setUpdatedAt(any());
        verifyNoInteractions(audit);
    }
}
