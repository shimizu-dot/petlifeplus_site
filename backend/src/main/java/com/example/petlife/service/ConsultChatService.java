package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.entity.ConsultChatMessageEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.mapper.ConsultChatMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ConsultChatService {

    private final ConsultChatMapper consultChatMapper;
    private final PlanAccessService planAccessService;

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
        boolean hasSymptom = false;
        boolean hasTiming = false;
        boolean hasFrequency = false;
        boolean hasCondition = false;

        for (ConsultChatMessageEntity row : recent) {
            if (!"USER".equals(row.senderType()) || row.message() == null) {
                continue;
            }
            String msg = row.message().toLowerCase(Locale.ROOT);
            hasSymptom = hasSymptom || containsAny(msg, Set.of("吐", "嘔吐", "下痢", "咳", "熱", "発熱", "震え", "食欲", "元気", "痛", "歩き"));
            hasTiming = hasTiming || containsAny(msg, Set.of("から", "昨日", "今日", "今朝", "夜", "時間", "日", "週"));
            hasFrequency = hasFrequency || containsAny(msg, Set.of("回", "毎", "たまに", "ずっと", "頻繁", "時々"));
            hasCondition = hasCondition || containsAny(msg, Set.of("食欲", "元気", "水", "便", "尿", "睡眠", "体温"));
        }

        return List.of(
                new FlowStepProgress("1. 症状の種類", hasSymptom),
                new FlowStepProgress("2. いつから", hasTiming),
                new FlowStepProgress("3. 頻度", hasFrequency),
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
        save(user.id(), "BOT", generateReply(user.id(), trimmed));
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

    private String generateReply(Long userId, String userMessage) {
        List<ConsultChatMessageEntity> recent = consultChatMapper.findRecentByUserId(userId, 12);
        String m = userMessage.toLowerCase(Locale.ROOT);

        boolean hasSymptom = containsAny(m, Set.of("吐", "嘔吐", "下痢", "咳", "熱", "発熱", "震え", "食欲", "元気", "痛", "歩き"));
        boolean hasTiming = containsAny(m, Set.of("から", "昨日", "今日", "今朝", "夜", "時間", "日", "週"));
        boolean hasFrequency = containsAny(m, Set.of("回", "毎", "たまに", "ずっと", "頻繁", "時々"));
        boolean hasCondition = containsAny(m, Set.of("食欲", "元気", "水", "便", "尿", "睡眠", "体温"));
        boolean hasEmergencyWord = containsAny(m, Set.of("痙攣", "けいれん", "呼吸", "息が", "血", "ぐったり", "意識", "倒れ"));

        for (ConsultChatMessageEntity row : recent) {
            if (!"USER".equals(row.senderType()) || row.message() == null) {
                continue;
            }
            String msg = row.message().toLowerCase(Locale.ROOT);
            hasSymptom = hasSymptom || containsAny(msg, Set.of("吐", "嘔吐", "下痢", "咳", "熱", "発熱", "震え", "食欲", "元気", "痛", "歩き"));
            hasTiming = hasTiming || containsAny(msg, Set.of("から", "昨日", "今日", "今朝", "夜", "時間", "日", "週"));
            hasFrequency = hasFrequency || containsAny(msg, Set.of("回", "毎", "たまに", "ずっと", "頻繁", "時々"));
            hasCondition = hasCondition || containsAny(msg, Set.of("食欲", "元気", "水", "便", "尿", "睡眠", "体温"));
            hasEmergencyWord = hasEmergencyWord || containsAny(msg, Set.of("痙攣", "けいれん", "呼吸", "息が", "血", "ぐったり", "意識", "倒れ"));
        }

        int riskScore = 0;
        if (hasEmergencyWord) {
            riskScore += 3;
        }
        if (containsAny(m, Set.of("吐", "嘔吐", "下痢", "発熱", "熱", "咳"))) {
            riskScore += 1;
        }
        if (hasCondition && containsAny(m, Set.of("食欲ない", "水を飲まない", "元気がない"))) {
            riskScore += 1;
        }

        String triage = riskScore >= 3 ? "今すぐ受診を推奨" : (riskScore >= 2 ? "本日中の受診を推奨" : "経過観察しつつ記録継続");
        String reason = riskScore >= 3
                ? "緊急サイン（呼吸・意識・けいれん・出血・強いぐったり）に該当する可能性があります。"
                : (riskScore >= 2
                ? "症状が複数要素にまたがっており、家庭観察だけでは悪化判断が遅れる可能性があります。"
                : "現時点では緊急性の高いワードが少ないため、短期観察での変化確認が有効です。");

        String nextQuestion;
        if (!hasSymptom) {
            nextQuestion = "症状の種類を教えてください（例: 嘔吐、下痢、咳、食欲低下）。";
        } else if (!hasTiming) {
            nextQuestion = "いつから症状が出ていますか？（例: 昨日夜から、3日前から）";
        } else if (!hasFrequency) {
            nextQuestion = "頻度を教えてください（例: 1日2回、断続的、ずっと続く）。";
        } else if (!hasCondition) {
            nextQuestion = "食欲・水分・元気・便/尿の変化を教えてください。";
        } else {
            nextQuestion = "追加で変化があれば追記してください。情報は揃っているため受診判断を優先できます。";
        }

        String action = riskScore >= 3
                ? "1) 直ちに受診先へ連絡\n2) 呼吸・意識・出血の有無を確認\n3) 移動中も体温低下/転倒に注意"
                : (riskScore >= 2
                ? "1) 本日中に受診予約\n2) 受診までの間に体温・飲水・排便を記録\n3) 悪化時は即時受診へ切替"
                : "1) 12〜24時間の観察記録\n2) 回数・食欲・元気の変化を記録\n3) 改善なし/悪化時は受診予約");

        return "【要約】\n"
                + "受け取った内容を一次トリアージしました。\n\n"
                + "【判定】\n"
                + triage + "\n\n"
                + "【判断理由】\n"
                + reason + "\n\n"
                + "【次の行動】\n"
                + action + "\n\n"
                + "【次に確認したいこと】\n"
                + nextQuestion;
    }

    private boolean containsAny(String input, Set<String> words) {
        for (String w : words) {
            if (input.contains(w)) {
                return true;
            }
        }
        return false;
    }

    public record FlowStepProgress(String label, boolean completed) {}
}
