package com.xinghuo.siteconfig.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.SiteConfig;
import com.xinghuo.repository.SiteConfigRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SiteConfigWritePersistenceAdapterTest {
    @Mock
    private SiteConfigRepository repository;

    private SiteConfig config;
    private SiteConfigWritePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        config = new SiteConfig();
        config.setBrandName("星火");
        config.setLegalEntityName("星维网络服务工作室");
        config.setServiceStatus(SiteConfig.ServiceStatus.OPEN);
        config.setQuoteEnabled(true);
        config.setOrderQueryEnabled(true);
        config.setContactTitle("联系客服");
        config.setContactIntro("联系前请准备业务编号");
        config.setWechatQrSize(220);
        config.setContactNotice("请勿发送密码或验证码");
        when(repository.findById(1L)).thenReturn(Optional.of(config));
        adapter = new SiteConfigWritePersistenceAdapter(repository);
    }

    @Test
    void imageMutationPreservesNormalizationAndValidation() {
        adapter.updateSiteImage("hero", "  /api/site-assets/test  ");
        assertEquals("/api/site-assets/test", config.getHeroImage());

        adapter.updateSiteImage("hero", "   ");
        assertEquals(null, config.getHeroImage());

        assertThrows(BadRequestException.class,
                () -> adapter.updateSiteImage("unknown", "/invalid"));
    }

    @Test
    void basicSectionKeepsExistingDefaultsAndUpdatesRuntimeSwitches() {
        adapter.updateSection("basic", Map.of(
                "brandName", "星维",
                "quoteEnabled", false,
                "orderQueryEnabled", false));

        assertEquals("星维", config.getBrandName());
        assertEquals("星维网络服务工作室", config.getLegalEntityName());
        assertEquals(SiteConfig.ServiceStatus.OPEN, config.getServiceStatus());
        assertFalse(config.isQuoteEnabled());
        assertFalse(config.isOrderQueryEnabled());
    }

    @Test
    void contactSectionSerializesProtocolIndependentChannelMaps() {
        adapter.updateSection("contact", Map.of(
                "contactChannels", List.of(Map.of(
                        "label", " 主客服 ",
                        "wechatId", " WX-001 ",
                        "qrImage", "/qr.png",
                        "note", " 发送业务编号 ",
                        "jumpUrl", "https://example.test"))));

        assertTrue(config.getContactChannels().startsWith("主客服\tWX-001\t/qr.png"));
        assertTrue(config.getContactChannels().contains("发送业务编号"));
    }
}
