package com.example.petlife.controller.line;

import com.example.petlife.service.line.LineBotService;
import com.example.petlife.service.line.LineRequestVerifier;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/line/events")
public class LineEventController {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final LineBotService lineBotService;
    private final LineRequestVerifier lineRequestVerifier;
    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

    public LineEventController(LineBotService lineBotService, LineRequestVerifier lineRequestVerifier) {
        this.lineBotService = lineBotService;
        this.lineRequestVerifier = lineRequestVerifier;
    }

    @PostMapping
    public ResponseEntity<?> events(
            @RequestHeader(value = "X-Line-Signature", required = false) String signature,
            @RequestBody String payloadJson
    ) {
        if (!lineRequestVerifier.isValid(signature, payloadJson)) {
            auditLog.warn("action=line_signature_invalid sigPresent={}", signature != null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ok", false));
        }

        Map<String, Object> payload = jsonParser.parseMap(payloadJson);
        Object eventsObj = payload.get("events");
        if (!(eventsObj instanceof List<?> events)) {
            return ResponseEntity.ok(Map.of("ok", true));
        }

        for (Object eventObj : events) {
            if (!(eventObj instanceof Map<?, ?> event)) continue;

            if (!"message".equals(event.get("type"))) continue;

            Object msgObj = event.get("message");
            if (!(msgObj instanceof Map<?, ?> message)) continue;
            if (!"text".equals(message.get("type"))) continue;

            String replyToken = (String) event.get("replyToken");
            String text = (String) message.get("text");

            if (replyToken != null && text != null) {
                lineBotService.replyMessage(replyToken, lineBotService.buildReply(text));
                auditLog.info("action=line_event_processed type=message");
            }
        }

        return ResponseEntity.ok(Map.of("ok", true));
    }
}
