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

    public void postUserMessage(LoginUser user, String message) {
        assertEligible(user);
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("message is required");
        }
        save(user.id(), "USER", trimmed);
        save(user.id(), "BOT", generateReply(trimmed));
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

    private String generateReply(String userMessage) {
        String m = userMessage.toLowerCase();
        if (m.contains("吐") || m.contains("下痢") || m.contains("ぐったり") || m.contains("血")) {
            return "重い症状の可能性があります。至急、受診導線から診療予約をご検討ください。";
        }
        if (m.contains("食欲") || m.contains("元気") || m.contains("便") || m.contains("咳")) {
            return "症状の経過を24時間記録し、悪化時は早めに相談してください。必要なら受診導線へ進めます。";
        }
        return "ご相談ありがとうございます。症状の種類・いつから・頻度・食欲/元気の変化を教えていただけると、より具体的に案内できます。";
    }
}
