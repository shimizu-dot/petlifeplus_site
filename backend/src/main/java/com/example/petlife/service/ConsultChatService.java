package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.entity.ConsultChatMessageEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.mapper.ConsultChatMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ConsultChatService {

    private static final Set<String> SYMPTOM_WORDS  = Set.of("吐", "嘔吐", "下痢", "咳", "熱", "発熱", "震え", "食欲", "元気", "痛", "歩き", "鼻水", "目やに", "血便", "よだれ");
    private static final Set<String> TIMING_WORDS   = Set.of("から", "昨日", "今日", "今朝", "夜", "時間", "日前", "週", "先週", "朝から");
    private static final Set<String> FREQUENCY_WORDS = Set.of("回", "毎", "たまに", "ずっと", "頻繁", "時々", "続い", "断続");
    private static final Set<String> CONDITION_WORDS = Set.of("食欲", "元気", "水", "飲", "便", "尿", "おしっこ", "うんち", "睡眠", "体温", "体重");
    private static final Set<String> EMERGENCY_WORDS = Set.of("痙攣", "けいれん", "呼吸", "息が", "大量", "血", "ぐったり", "意識", "倒れ", "動けない");

    // 食欲不振 2日以上 → 即時受診ルール
    private static final Set<String> APPETITE_LOSS_WORDS = Set.of(
            "食べない", "食欲がない", "食欲ない", "食事を取らない", "食事をとらない",
            "ご飯を食べない", "ご飯食べない", "食べていない", "食べてない",
            "食欲低下", "食欲不振", "食べなくなった", "食べなく");
    private static final Set<String> TWO_OR_MORE_DAYS_WORDS = Set.of(
            "2日", "二日", "ふつか", "2日間", "丸2日", "3日", "三日", "みっか",
            "3日間", "4日", "5日", "1週間", "48時間", "二日間");

    private final ConsultChatMapper consultChatMapper;
    private final PlanAccessService planAccessService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api-key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-4.1-mini}")
    private String openaiModel;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    public ConsultChatService(ConsultChatMapper consultChatMapper, PlanAccessService planAccessService) {
        this.consultChatMapper = consultChatMapper;
        this.planAccessService = planAccessService;
    }

    public List<ConsultChatMessageEntity> getRecentMessages(LoginUser user) {
        assertEligible(user);
        List<ConsultChatMessageEntity> rows = consultChatMapper.findRecentByUserId(user.id(), 50);
        List<ConsultChatMessageEntity> ordered = new ArrayList<>(rows);
        Collections.reverse(ordered);
        return ordered;
    }

    public List<FlowStepProgress> getFlowProgress(LoginUser user) {
        assertEligible(user);
        List<ConsultChatMessageEntity> recent = consultChatMapper.findRecentByUserId(user.id(), 20);
        boolean hasSymptom = false, hasTiming = false, hasFrequency = false, hasCondition = false;

        for (ConsultChatMessageEntity row : recent) {
            if (!"USER".equals(row.senderType()) || row.message() == null) continue;
            String msg = row.message().toLowerCase(Locale.ROOT);
            hasSymptom   |= containsAny(msg, SYMPTOM_WORDS);
            hasTiming    |= containsAny(msg, TIMING_WORDS);
            hasFrequency |= containsAny(msg, FREQUENCY_WORDS);
            hasCondition |= containsAny(msg, CONDITION_WORDS);
        }

        return List.of(
                new FlowStepProgress("1. 症状の種類",               hasSymptom),
                new FlowStepProgress("2. いつから",                  hasTiming),
                new FlowStepProgress("3. 頻度",                      hasFrequency),
                new FlowStepProgress("4. 食欲・水分・元気・便/尿の変化", hasCondition)
        );
    }

    public void postUserMessage(LoginUser user, String message) {
        assertEligible(user);
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("message is required");
        }
        save(user.id(), "USER", trimmed);
        // Fetch history after saving so current message is included
        List<ConsultChatMessageEntity> historyDesc = consultChatMapper.findRecentByUserId(user.id(), 20);
        save(user.id(), "BOT", generateReply(historyDesc, trimmed));
    }

    private void assertEligible(LoginUser user) {
        if (!planAccessService.canUseAiSymptom(user)) {
            throw new BadRequestException("この機能はスタンダード以上で利用できます");
        }
    }

    private void save(Long userId, String senderType, String message) {
        consultChatMapper.insertReturningId(new ConsultChatMessageEntity(
                null, userId, senderType, message, LocalDateTime.now()
        ));
    }

    // ---------------------------------------------------------------
    // Reply generation
    // ---------------------------------------------------------------

    private String generateReply(List<ConsultChatMessageEntity> historyDesc, String userMessage) {
        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            return callOpenAi(historyDesc, userMessage);
        }
        return fallbackReply(historyDesc, userMessage);
    }

    private String callOpenAi(List<ConsultChatMessageEntity> historyDesc, String userMessage) {
        try {
            String systemPrompt = """
                    あなたは動物病院「ペットライフプラス」の受診相談チャットボットです。
                    犬を中心としたペットの症状について、飼い主と自然な多ターン会話を行い、受診優先度の判断を支援します。

                    【制約】
                    - 診断名を断言しない（「〜の可能性があります」にとどめる）
                    - 処方・投薬の具体的な指示はしない
                    - 緊急ワード（けいれん・意識不明・大量出血・呼吸困難・ぐったり動けない）が含まれる場合は即時受診を最優先で案内して会話を終える
                    - 食事をほとんど取れていない状態が2日以上続いている場合は、脱水・低血糖リスクを伝え「今すぐ受診」を強く推奨する（他の症状がなくても適用）

                    【会話の進め方】
                    - 症状の種類・発症時期・頻度・全身状態（食欲/元気/水分/便尿）が揃うまでは、自然な会話で1〜2点ずつ確認する
                    - 情報収集フェーズでは毎回トリアージ判定を出さない
                    - 情報が十分揃った段階で初めて「今すぐ受診」「本日中に受診」「経過観察」のいずれかを明示し、理由と行動を簡潔に伝える
                    - 追加情報が来たら判断を更新する
                    - 返答は日本語で 200 文字程度を目安に簡潔にまとめる
                    """;

            // historyDesc は新しい順。chronological order に並び替え
            List<ConsultChatMessageEntity> chronological = new ArrayList<>(historyDesc);
            Collections.reverse(chronological);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            for (ConsultChatMessageEntity msg : chronological) {
                String role = "USER".equals(msg.senderType()) ? "user" : "assistant";
                messages.add(Map.of("role", role, "content", msg.message()));
            }

            Map<String, Object> body = new HashMap<>();
            body.put("model", openaiModel);
            body.put("messages", messages);
            body.put("temperature", 0.7);
            body.put("max_tokens", 400);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            String res = restTemplate.postForObject(
                    openaiBaseUrl + "/chat/completions",
                    new HttpEntity<>(body, headers),
                    String.class
            );

            String content = extractContent(res);
            return (content != null && !content.isBlank()) ? content : fallbackReply(historyDesc, userMessage);
        } catch (Exception e) {
            return fallbackReply(historyDesc, userMessage);
        }
    }

    // ---------------------------------------------------------------
    // Turn-aware fallback (no OpenAI key)
    // ---------------------------------------------------------------

    private String fallbackReply(List<ConsultChatMessageEntity> historyDesc, String userMessage) {
        int botTurns = 0;
        boolean hasSymptom = false, hasTiming = false, hasFrequency = false, hasCondition = false, hasEmergency = false;
        boolean hasAppetiteLoss = false, hasTwoPlusDays = false;

        for (ConsultChatMessageEntity row : historyDesc) {
            if ("BOT".equals(row.senderType())) botTurns++;
            if (!"USER".equals(row.senderType()) || row.message() == null) continue;
            String msg = row.message().toLowerCase(Locale.ROOT);
            hasSymptom      |= containsAny(msg, SYMPTOM_WORDS);
            hasTiming       |= containsAny(msg, TIMING_WORDS);
            hasFrequency    |= containsAny(msg, FREQUENCY_WORDS);
            hasCondition    |= containsAny(msg, CONDITION_WORDS);
            hasEmergency    |= containsAny(msg, EMERGENCY_WORDS);
            hasAppetiteLoss |= containsAny(msg, APPETITE_LOSS_WORDS);
            hasTwoPlusDays  |= containsAny(msg, TWO_OR_MORE_DAYS_WORDS);
        }

        // Immediate escalation – emergency keywords
        if (hasEmergency) {
            return "⚠️ 緊急サインが含まれています。\n今すぐ動物病院へ連絡し、直ちに受診してください。\n移動中も呼吸・意識・出血の状態に注意してください。";
        }

        // Immediate escalation – 2+ days of appetite loss
        if (hasAppetiteLoss && hasTwoPlusDays) {
            return "⚠️ 食事を取れない状態が2日以上続いている場合、脱水や低血糖のリスクがあります。\n他に症状がなくても、今すぐ動物病院へ連絡し、できる限り本日中に受診してください。\n受診まで水分（水・スープ）を少量でも与えられるか確認してください。";
        }

        // Phase 1 – gather missing core info
        if (!hasSymptom) {
            return "ご連絡ありがとうございます。\nどのような症状が見られますか？（例: 嘔吐、下痢、食欲低下、咳など）";
        }
        if (!hasTiming) {
            return "症状をお知らせいただきありがとうございます。\nいつ頃から始まりましたか？（例: 昨日の夜から、3日前から）";
        }
        if (!hasFrequency) {
            return "了解しました。\nその症状はどのくらいの頻度で起きていますか？（例: 1日2回、ずっと続いている、たまに）";
        }
        if (!hasCondition) {
            return "ありがとうございます。\n現在の食欲・水分摂取・元気の様子、および便や尿の状態はいかがでしょうか？";
        }

        // Phase 2 – triage with accumulated context
        int risk = 0;
        String low = userMessage.toLowerCase(Locale.ROOT);
        if (containsAny(low, Set.of("吐", "嘔吐", "下痢", "発熱", "熱", "咳"))) risk++;
        if (containsAny(low, Set.of("食欲ない", "食欲がない", "水を飲まない", "元気がない", "元気ない", "ぐったり"))) risk++;
        if (hasTiming && hasFrequency) risk++;

        String triage = risk >= 2 ? "本日中の受診を推奨" : "経過観察しつつ記録継続";
        String reason = risk >= 2
                ? "複数の症状要素が確認されており、家庭観察のみでは悪化を見落とすリスクがあります。"
                : "現時点では緊急性の高いサインは見られません。引き続き状態の変化を記録してください。";
        String action = risk >= 2
                ? "・本日中に動物病院への相談をお勧めします\n・受診まで体温・飲水・排便の変化を記録してください\n・症状が急激に悪化した場合は即時受診に切り替えてください"
                : "・12〜24時間の観察を続けてください\n・食欲・元気・便尿の変化をメモしておいてください\n・改善が見られない場合や悪化した場合は受診予約をしてください";

        String followUp = botTurns >= 5
                ? "追加の変化があればいつでも教えてください。"
                : "他に気になる症状や変化があればお知らせください。";

        return "これまでの情報をまとめました。\n\n"
                + "【判定】" + triage + "\n\n"
                + "【理由】" + reason + "\n\n"
                + "【行動】\n" + action + "\n\n"
                + followUp;
    }

    // ---------------------------------------------------------------
    // Utilities
    // ---------------------------------------------------------------

    private String extractContent(String response) {
        if (response == null) return null;
        int idx = response.indexOf("\"content\":");
        if (idx < 0) return null;
        idx += 10;
        while (idx < response.length() && response.charAt(idx) != '"') idx++;
        if (idx >= response.length()) return null;
        idx++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (idx < response.length()) {
            char c = response.charAt(idx);
            if (c == '\\' && idx + 1 < response.length()) {
                char next = response.charAt(idx + 1);
                switch (next) {
                    case '"'  -> { sb.append('"');  idx += 2; continue; }
                    case 'n'  -> { sb.append('\n'); idx += 2; continue; }
                    case '\\'  -> { sb.append('\\'); idx += 2; continue; }
                    case 't'  -> { sb.append('\t'); idx += 2; continue; }
                    default -> {}
                }
            }
            if (c == '"') break;
            sb.append(c);
            idx++;
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private boolean containsAny(String input, Set<String> words) {
        for (String w : words) {
            if (input.contains(w)) return true;
        }
        return false;
    }

    public record FlowStepProgress(String label, boolean completed) {}
}
