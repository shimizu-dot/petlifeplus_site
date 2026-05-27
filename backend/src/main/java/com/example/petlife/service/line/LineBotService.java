package com.example.petlife.service.line;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LineBotService {

    private static final Logger log = LoggerFactory.getLogger(LineBotService.class);

    private static final String REPLY_URL      = "https://api.line.me/v2/bot/message/reply";
    private static final String PUSH_URL       = "https://api.line.me/v2/bot/message/push";
    private static final String MULTICAST_URL  = "https://api.line.me/v2/bot/message/multicast";
    private static final String BROADCAST_URL  = "https://api.line.me/v2/bot/message/broadcast";
    private static final int    MULTICAST_MAX  = 500;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${line.channel-token:}")
    private String channelToken;

    // ---------------------------------------------------------------
    // Reply（受信メッセージへの返信）
    // ---------------------------------------------------------------

    @Async
    public void replyMessage(String replyToken, String text) {
        if (!isConfigured()) return;

        HttpHeaders headers = buildHeaders();
        Map<String, Object> body = Map.of(
                "replyToken", replyToken,
                "messages", List.of(Map.of("type", "text", "text", text))
        );
        try {
            restTemplate.postForEntity(REPLY_URL, new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            log.warn("LINE reply failed: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Push（特定ユーザーへの能動的送信）
    // ---------------------------------------------------------------

    @Async
    public void pushMessage(String lineUserId, String text) {
        if (!isConfigured() || lineUserId == null || lineUserId.isBlank()) return;

        HttpHeaders headers = buildHeaders();
        Map<String, Object> body = Map.of(
                "to", lineUserId,
                "messages", List.of(Map.of("type", "text", "text", text))
        );
        try {
            restTemplate.postForEntity(PUSH_URL, new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            log.warn("LINE push failed to {}: {}", lineUserId, e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Broadcast（Bot の全フォロワーへ一斉送信）
    // ---------------------------------------------------------------

    /**
     * Bot を友達追加している全ユーザーにメッセージを送信する。
     * ユーザーIDの登録不要。LINE Messaging API の Broadcast エンドポイントを使用。
     */
    public void broadcastMessage(String text) {
        if (!isConfigured()) return;

        HttpHeaders headers = buildHeaders();
        Map<String, Object> body = Map.of(
                "messages", List.of(Map.of("type", "text", "text", text))
        );
        try {
            restTemplate.postForEntity(BROADCAST_URL, new HttpEntity<>(body, headers), String.class);
            log.info("LINE broadcast sent");
        } catch (Exception e) {
            log.warn("LINE broadcast failed: {}", e.getMessage());
            throw new RuntimeException("LINE broadcast failed: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Multicast（指定ユーザーへの一斉送信・将来利用向けに保持）
    // ---------------------------------------------------------------

    public int multicastMessage(List<String> lineUserIds, String text) {
        if (!isConfigured() || lineUserIds == null || lineUserIds.isEmpty()) return 0;

        List<String> valid = lineUserIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .toList();
        if (valid.isEmpty()) return 0;

        for (int i = 0; i < valid.size(); i += MULTICAST_MAX) {
            List<String> chunk = valid.subList(i, Math.min(i + MULTICAST_MAX, valid.size()));
            sendMulticastChunk(new ArrayList<>(chunk), text);
        }
        return valid.size();
    }

    private void sendMulticastChunk(List<String> lineUserIds, String text) {
        HttpHeaders headers = buildHeaders();
        Map<String, Object> body = Map.of(
                "to", lineUserIds,
                "messages", List.of(Map.of("type", "text", "text", text))
        );
        try {
            restTemplate.postForEntity(MULTICAST_URL, new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            log.warn("LINE multicast failed (chunk size={}): {}", lineUserIds.size(), e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // キーワード返答テキスト生成
    // ---------------------------------------------------------------

    public String buildReply(String text) {
        String m = text == null ? "" : text.toLowerCase();
        if (m.contains("痙攣") || m.contains("けいれん") || m.contains("呼吸") || m.contains("血") || m.contains("ぐったり")) {
            return "【緊急】重篤なサインの可能性があります。すぐに動物病院へご連絡ください。";
        }
        if (m.contains("吐") || m.contains("嘔吐") || m.contains("下痢") || m.contains("発熱") || m.contains("咳")) {
            return "症状が確認されました。本日中の受診をご検討ください。\n受診までの間、体温・飲水・排便の状態を記録しておくと診察がスムーズです。";
        }
        if (m.contains("食欲") || m.contains("元気") || m.contains("便") || m.contains("尿") || m.contains("睡眠")) {
            return "症状の経過（いつから・頻度・食欲/元気）を記録して、悪化時は受診してください。\nアプリの健康記録機能もご活用ください。";
        }
        return "ご相談ありがとうございます。\n症状の種類・開始時期・頻度を教えていただければ、次の対応をご案内します。";
    }

    // ---------------------------------------------------------------
    // Utilities
    // ---------------------------------------------------------------

    public boolean isConfigured() {
        return channelToken != null && !channelToken.isBlank();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(channelToken);
        return headers;
    }
}
