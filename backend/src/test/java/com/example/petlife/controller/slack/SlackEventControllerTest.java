package com.example.petlife.controller.slack;

import com.example.petlife.service.AnnouncementService;
import com.example.petlife.service.slack.SlackBotService;
import com.example.petlife.service.slack.SlackRequestVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class SlackEventControllerTest {

    @Test
    void shouldReturnUnauthorizedWhenSignatureInvalid() {
        SlackBotService bot = mock(SlackBotService.class);
        SlackRequestVerifier verifier = mock(SlackRequestVerifier.class);
        when(verifier.isValid(any(), any(), any())).thenReturn(false);

        SlackEventController controller = new SlackEventController(bot, verifier, mock(AnnouncementService.class), "");
        ResponseEntity<?> res = controller.events("123", "v0=bad", null, "{\"type\":\"event_callback\"}");

        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }

    @Test
    void shouldReturnChallengeOnUrlVerification() {
        SlackBotService bot = mock(SlackBotService.class);
        SlackRequestVerifier verifier = mock(SlackRequestVerifier.class);
        when(verifier.isValid(any(), any(), any())).thenReturn(true);

        SlackEventController controller = new SlackEventController(bot, verifier, mock(AnnouncementService.class), "");
        ResponseEntity<?> res = controller.events("123", "v0=ok", null, "{\"type\":\"url_verification\",\"challenge\":\"abc123\"}");

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertTrue(res.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals("abc123", body.get("challenge"));
    }
}
