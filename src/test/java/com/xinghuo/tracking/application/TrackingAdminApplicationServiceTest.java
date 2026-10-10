package com.xinghuo.tracking.application;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class TrackingAdminApplicationServiceTest {
    @Test
    void clampsSummaryWindowToSupportedRange() {
        TrackingSummaryPort summaries = mock(TrackingSummaryPort.class);
        TrackingAdminApplicationService service = new TrackingAdminApplicationService(summaries);
        TrackingSummaryView minimum = new TrackingSummaryView(1, 0, 0, null, List.of(), List.of(), List.of());
        TrackingSummaryView maximum = new TrackingSummaryView(90, 0, 0, null, List.of(), List.of(), List.of());
        when(summaries.summary(1)).thenReturn(minimum);
        when(summaries.summary(90)).thenReturn(maximum);

        assertSame(minimum, service.summary(0));
        assertSame(maximum, service.summary(365));
        verify(summaries).summary(1);
        verify(summaries).summary(90);
    }
}
