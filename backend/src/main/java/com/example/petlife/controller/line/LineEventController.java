package com.example.petlife.controller.line;

import com.example.petlife.service.AnnouncementService;
import com.example.petlife.service.line.LineBotService;
import com.example.petlife.service.line.LineRequestVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/line/events")
public class LineEventController {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final LineBotService lineBotService;
    private final LineRequestVerifier lineRequestVerifier;
    private final AnnouncementService announcementService;
    private final Set<String> adminLineUserIds;
    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

    public LineEventController(LineBotService lineBotService,
                               LineRequestVerifier lineRequestVerifier,
                               AnnouncementService announcementService,
                               @Value("${admin.line-user-ids:}") String adminLineUserIdsCsv) {
        this.lineBotService = lineBotService;
        this.lineRequestVerifier = lineRequestVerifier;
        this.announcementService = announcementService;
        this.adminLineUserIds = Arrays.stream(adminLineUserIdsCsv.split(","))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
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
            String text       = (String) message.get("text");

            // source.userId を取得
            String senderId = null;
            Object sourceObj = event.get("source");
            if (sourceObj instanceof Map<?, ?> source) {
                senderId = (String) source.get("userId");
            }

            if (replyToken == null || text == null) continue;

            // 管理者からのメッセージ → お知らせ登録を試みる
            if (!adminLineUserIds.isEmpty() && adminLineUserIds.contains(senderId)) {
                if (announcementService.tryCreateFromBot(text)) {
                    lineBotService.replyMessage(replyToken, "✅ お知らせを登録・公開しました。");
                    auditLog.info("action=announcement_created_via_line user={}", senderId);
                } else if (text.strip().startsWith("お知らせ")) {
                    lineBotService.replyMessage(replyToken, AnnouncementService.usageMessage());
                } else {
                    lineBotService.replyMessage(replyToken, lineBotService.buildReply(text));
                }
            } else {
                lineBotService.replyMessage(replyToken, lineBotService.buildReply(text));
                auditLog.info("action=line_event_processed type=message");
            }
        }

        return ResponseEntity.ok(Map.of("ok", true));
    }
}
