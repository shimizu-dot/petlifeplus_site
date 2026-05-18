package com.example.petlife.entity;

import java.time.LocalDateTime;

public record ConsultChatMessageEntity(
        Long id,
        Long userId,
        String senderType,
        String message,
        LocalDateTime createdAt
) {
}
