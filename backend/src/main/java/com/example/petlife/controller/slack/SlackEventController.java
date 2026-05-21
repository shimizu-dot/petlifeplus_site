package com.example.petlife.controller.slack;

import com.example.petlife.service.slack.SlackBotService;
import com.example.petlife.service.slack.SlackRequestVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/slack/events")
public class SlackEventController {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final SlackBotService slackBotService;
    private final SlackRequestVerifier slackRequestVerifier;
    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

    public SlackEventController(SlackBotService slackBotService, SlackRequestVerifier slackRequestVerifier) {
        this.slackBotService = slackBotService;
        this.slackRequestVerifier = slackRequestVerifier;
    }

    @PostMapping
    public ResponseEntity<?> events(
            @RequestHeader(value = "X-Slack-Request-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Slack-Signature", required = false) String signature,
            @RequestHeader(value = "X-Slack-Retry-Num", required = false) String retryNum,
            @RequestBody String payloadJson
    ) {
        if (retryNum != null) {
            return ResponseEntity.ok(Map.of("ok", true));
        }

        if (!slackRequestVerifier.isValid(timestamp, signature, payloadJson)) {
            auditLog.warn("action=slack_signature_invalid timestamp={} sigPresent={}", timestamp, signature != null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ok", false));
        }

        Map<String, Object> payload = jsonParser.parseMap(payloadJson);
        Object type = payload.get("type");

        if ("url_verification".equals(type)) {
            return ResponseEntity.ok(Map.of("challenge", payload.get("challenge")));
        }

        if (!"event_callback".equals(type)) {
            return ResponseEntity.ok(Map.of("ok", true));
        }

        Object eventObj = payload.get("event");
        if (!(eventObj instanceof Map<?, ?> event)) {
            return ResponseEntity.ok(Map.of("ok", true));
        }

        String eventType = (String) event.get("type");
        String subtype = (String) event.get("subtype");
        if (!"message".equals(eventType) || subtype != null) {
            return ResponseEntity.ok(Map.of("ok", true));
        }

        String channel = (String) event.get("channel");
        String text = (String) event.get("text");
        if (channel != null && text != null) {
            slackBotService.postMessage(channel, slackBotService.buildReply(text));
            auditLog.info("action=slack_event_processed eventType=message channel={}", channel);
        }

        return ResponseEntity.ok(Map.of("ok", true));
    }
}
