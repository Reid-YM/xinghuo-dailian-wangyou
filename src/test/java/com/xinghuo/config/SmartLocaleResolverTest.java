package com.xinghuo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.security.GeoLanguageResolver;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class SmartLocaleResolverTest {

    @Test
    void public172Dot2AddressFromForwardedHeaderUsesGeoLookup() {
        GeoLanguageResolver geo = org.mockito.Mockito.mock(GeoLanguageResolver.class);
        when(geo.detectLanguage("172.2.1.1")).thenReturn("en");
        SmartLocaleResolver resolver = new SmartLocaleResolver(geo);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "172.2.1.1");

        assertEquals(Locale.ENGLISH, resolver.resolveLocale(request));
        verify(geo).detectLanguage("172.2.1.1");
    }

    @Test
    void private172Dot20AddressIsIgnoredAsForwardedClientIp() {
        GeoLanguageResolver geo = org.mockito.Mockito.mock(GeoLanguageResolver.class);
        when(geo.detectLanguage("203.0.113.10")).thenReturn("en");
        SmartLocaleResolver resolver = new SmartLocaleResolver(geo);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "172.20.1.1");

        assertEquals(Locale.ENGLISH, resolver.resolveLocale(request));
        verify(geo, never()).detectLanguage("172.20.1.1");
        verify(geo).detectLanguage("203.0.113.10");
    }

    @Test
    void public172Dot32BoundaryIsNotTreatedAsPrivate() {
        GeoLanguageResolver geo = org.mockito.Mockito.mock(GeoLanguageResolver.class);
        when(geo.detectLanguage("172.32.0.1")).thenReturn("en");
        SmartLocaleResolver resolver = new SmartLocaleResolver(geo);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "172.32.0.1");

        assertEquals(Locale.ENGLISH, resolver.resolveLocale(request));
        verify(geo).detectLanguage("172.32.0.1");
    }

    @Test
    void uniqueLocalIpv6ForwardedAddressFallsBackToRemoteAddress() {
        GeoLanguageResolver geo = org.mockito.Mockito.mock(GeoLanguageResolver.class);
        when(geo.detectLanguage("203.0.113.10")).thenReturn("en");
        when(geo.detectLanguage(anyString())).thenAnswer(invocation -> {
            String ip = invocation.getArgument(0);
            return "203.0.113.10".equals(ip) ? "en" : null;
        });
        SmartLocaleResolver resolver = new SmartLocaleResolver(geo);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "fd12:3456::1");

        assertEquals(Locale.ENGLISH, resolver.resolveLocale(request));
        verify(geo, never()).detectLanguage("fd12:3456::1");
        verify(geo).detectLanguage("203.0.113.10");
    }
}
