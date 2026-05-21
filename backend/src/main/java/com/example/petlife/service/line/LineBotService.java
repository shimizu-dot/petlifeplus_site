package com.example.petlife.service.line;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class LineBotService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${line.channel-token:}")
    private String channelToken;

    private static final String REPLY_URL = "https://api.line.me/v2/bot/message/reply";

    @Async
    public void replyMessage(String replyToken, String text) {
        if (channelToken == null || channelToken.isBlank()) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(channelToken);

        Map<String, Object> body = Map.of(
                "replyToken", replyToken,
                "messages", List.of(Map.of("type", "text", "text", text))
        );

        restTemplate.postForEntity(REPLY_URL, new HttpEntity<>(body, headers), String.class);
    }

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
}
