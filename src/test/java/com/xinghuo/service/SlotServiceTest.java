package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.HotSlot;
import com.xinghuo.model.ServiceItem;
import com.xinghuo.repository.HotSlotRepository;
import com.xinghuo.repository.ServiceItemRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SlotServiceTest {

    @Test
    void publicHotServicesPrefersManualSlotsThenFillsWithoutDuplicates() {
        HotSlotRepository slots = mock(HotSlotRepository.class);
        ServiceItemRepository services = mock(ServiceItemRepository.class);
        TaxonomyService taxonomy = mock(TaxonomyService.class);
        SlotService service = new SlotService(slots, services, mock(AuditService.class), taxonomy);

        ServiceItem first = serviceItem(1L, "第一服务", ServiceItem.Status.AVAILABLE, true, null);
        ServiceItem second = serviceItem(2L, "第二服务", ServiceItem.Status.AVAILABLE, true, null);
        when(services.findPublic(any(Instant.class))).thenReturn(List.of(first, second));
        when(taxonomy.publicAllowed("CODMW4", "WEAPON")).thenReturn(true);
        when(slots.findAllByOrderBySlotNoAsc()).thenReturn(List.of(
                HotSlot.builder().slotNo(1).mode(HotSlot.Mode.MANUAL).service(second).updatedAt(Instant.now()).build(),
                HotSlot.builder().slotNo(2).mode(HotSlot.Mode.MANUAL).service(second).updatedAt(Instant.now()).build()));

        List<ApiDtos.ServiceResponse> result = service.publicHotServices();

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).id());
        assertEquals(1L, result.get(1).id());
    }

    @Test
    void autoModeStillResolvesSuppliedServiceThenClearsSlot() {
        HotSlotRepository slots = mock(HotSlotRepository.class);
        ServiceItemRepository services = mock(ServiceItemRepository.class);
        AuditService audit = mock(AuditService.class);
        TaxonomyService taxonomy = mock(TaxonomyService.class);
        SlotService service = new SlotService(slots, services, audit, taxonomy);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        ServiceItem existingService = serviceItem(7L, "旧服务", ServiceItem.Status.AVAILABLE, true, null);
        ServiceItem submittedService = serviceItem(8L, "提交服务", ServiceItem.Status.AVAILABLE, true, null);
        HotSlot slot = HotSlot.builder()
                .slotNo(1)
                .mode(HotSlot.Mode.MANUAL)
                .service(existingService)
                .updatedAt(Instant.now())
                .build();
        when(slots.findBySlotNo(1)).thenReturn(Optional.of(slot));
        when(services.findById(8L)).thenReturn(Optional.of(submittedService));

        ApiDtos.SlotResponse response = service.update(
                new ApiDtos.SlotUpdate(1, 8L, "AUTO", "切换自动推荐"),
                "admin",
                httpRequest);

        verify(services).findById(8L);
        assertEquals("AUTO", response.mode());
        assertNull(response.serviceId());
        assertNull(slot.getService());
    }

    @Test
    void manualModeRejectsServiceThatIsNotPubliclyDisplayable() {
        HotSlotRepository slots = mock(HotSlotRepository.class);
        ServiceItemRepository services = mock(ServiceItemRepository.class);
        TaxonomyService taxonomy = mock(TaxonomyService.class);
        SlotService service = new SlotService(slots, services, mock(AuditService.class), taxonomy);

        HotSlot slot = HotSlot.builder()
                .slotNo(2)
                .mode(HotSlot.Mode.AUTO)
                .updatedAt(Instant.now())
                .build();
        ServiceItem offline = serviceItem(9L, "下架服务", ServiceItem.Status.OFFLINE, true, null);
        when(slots.findBySlotNo(2)).thenReturn(Optional.of(slot));
        when(services.findById(9L)).thenReturn(Optional.of(offline));

        assertThrows(BadRequestException.class, () -> service.update(
                new ApiDtos.SlotUpdate(2, 9L, "MANUAL", "手动指定"),
                "admin",
                mock(HttpServletRequest.class)));

        assertEquals(HotSlot.Mode.AUTO, slot.getMode());
        assertNull(slot.getService());
    }

    private ServiceItem serviceItem(
            Long id,
            String name,
            ServiceItem.Status status,
            boolean active,
            Instant expiresAt) {
        Instant now = Instant.parse("2026-10-12T00:00:00Z");
        return ServiceItem.builder()
                .id(id)
                .slug("service-" + id)
                .gameCode("CODMW4")
                .name(name)
                .category("WEAPON")
                .shortDesc("测试服务")
                .priceMode(ServiceItem.PriceMode.FIXED)
                .durationText("1小时")
                .status(status)
                .active(active)
                .sortOrder(id.intValue())
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(expiresAt)
                .build();
    }
}
