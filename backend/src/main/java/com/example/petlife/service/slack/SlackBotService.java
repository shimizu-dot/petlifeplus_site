package com.example.petlife.service.slack;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class SlackBotService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${slack.bot-token:}")
    private String botToken;

    @Async
    public void postMessage(String channel, String text) {
        if (botToken == null || botToken.isBlank()) {
            return;
        }

        String url = "https://slack.com/api/chat.postMessage";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(botToken);

        Map<String, Object> body = new HashMap<>();
        body.put("channel", channel);
        body.put("text", text);

        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    }

    public String buildReply(String text) {
        String m = text == null ? "" : text.toLowerCase();
        if (m.contains("吐") || m.contains("下痢") || m.contains("血") || m.contains("呼吸")) {
            return "重めの症状の可能性があります。早めの受診をご検討ください。必要なら管理画面の相談チャットもご利用ください。";
        }
        if (m.contains("食欲") || m.contains("元気") || m.contains("咳") || m.contains("便")) {
            return "症状の経過（いつから・頻度・食欲/元気）を記録して、悪化時は受診してください。";
        }
        return "ご相談ありがとうございます。症状・開始時期・頻度を送っていただければ、次の対応を案内します。";
    }
}
