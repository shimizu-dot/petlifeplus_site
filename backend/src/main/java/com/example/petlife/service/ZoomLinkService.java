package com.example.petlife.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class ZoomLinkService {

    private static final Logger opsLog = LoggerFactory.getLogger("OPS");
    private static final SecureRandom RANDOM = new SecureRandom();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${zoom.meeting-base-url:https://zoom.us/j}")
    private String zoomMeetingBaseUrl;

    @Value("${zoom.api-base-url:https://api.zoom.us/v2}")
    private String zoomApiBaseUrl;

    @Value("${zoom.oauth-base-url:https://zoom.us/oauth/token}")
    private String zoomOauthBaseUrl;

    @Value("${zoom.account-id:}")
    private String zoomAccountId;

    @Value("${zoom.client-id:}")
    private String zoomClientId;

    @Value("${zoom.client-secret:}")
    private String zoomClientSecret;

    public String generateJoinUrl() {
        long roomId = 1000000000L + Math.abs(RANDOM.nextLong() % 8999999999L);
        return zoomMeetingBaseUrl + "/" + roomId;
    }

    public ZoomMeetingResult createMeetingOrFallback(LocalDateTime scheduledAt, String topic) {
        if (!hasZoomApiCredentials()) {
            String fallback = generateJoinUrl();
            return new ZoomMeetingResult(fallback, true, "ZOOM_CREDENTIALS_MISSING");
        }
        try {
            String accessToken = fetchAccessToken();
            String joinUrl = createMeeting(accessToken, scheduledAt, topic);
            return new ZoomMeetingResult(joinUrl, false, null);
        } catch (Exception e) {
            opsLog.warn("zoom_api_failed reason={}", e.getMessage());
            String fallback = generateJoinUrl();
            String reason = e.getMessage() == null || e.getMessage().isBlank()
                    ? "ZOOM_API_ERROR"
                    : "ZOOM_API_ERROR: " + e.getMessage();
            return new ZoomMeetingResult(fallback, true, reason);
        }
    }

    private boolean hasZoomApiCredentials() {
        return notBlank(zoomAccountId) && notBlank(zoomClientId) && notBlank(zoomClientSecret);
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String fetchAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String basic = Base64.getEncoder().encodeToString((zoomClientId + ":" + zoomClientSecret).getBytes());
        headers.add("Authorization", "Basic " + basic);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "account_credentials");
        form.add("account_id", zoomAccountId);

        ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                zoomOauthBaseUrl,
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                new ParameterizedTypeReference<>() {}
        );
        Object token = res.getBody() != null ? res.getBody().get("access_token") : null;
        if (token == null) {
            throw new IllegalStateException("zoom access token missing");
        }
        return token.toString();
    }

    private String createMeeting(String accessToken, LocalDateTime scheduledAt, String topic) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = new HashMap<>();
        body.put("topic", topic == null || topic.isBlank() ? "Pet Life Plus Online Care" : topic);
        body.put("type", 2);
        body.put("start_time", scheduledAt.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        body.put("timezone", "UTC");
        body.put("duration", 30);
        body.put("agenda", "Premium priority online care");

        ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                zoomApiBaseUrl + "/users/me/meetings",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<>() {}
        );
        Object joinUrl = res.getBody() != null ? res.getBody().get("join_url") : null;
        if (joinUrl == null) {
            throw new IllegalStateException("zoom join url missing");
        }
        return joinUrl.toString();
    }

    public record ZoomMeetingResult(
            String joinUrl,
            boolean fallbackUsed,
            String fallbackReason
    ) {}
}
