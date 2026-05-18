package com.example.petlife.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoomLinkServiceTest {

    @Test
    void shouldFallbackToGeneratedUrlWhenCredentialsMissing() {
        ZoomLinkService svc = new ZoomLinkService();
        ReflectionTestUtils.setField(svc, "zoomMeetingBaseUrl", "https://zoom.us/j");
        ReflectionTestUtils.setField(svc, "zoomAccountId", "");
        ReflectionTestUtils.setField(svc, "zoomClientId", "");
        ReflectionTestUtils.setField(svc, "zoomClientSecret", "");

        ZoomLinkService.ZoomMeetingResult result =
                svc.createMeetingOrFallback(java.time.LocalDateTime.now().plusHours(1), "test");
        assertTrue(result.joinUrl().startsWith("https://zoom.us/j/"));
    }
}
