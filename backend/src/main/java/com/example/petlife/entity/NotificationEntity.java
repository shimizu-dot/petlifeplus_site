package com.example.petlife.entity;

import java.time.LocalDateTime;

public record NotificationEntity(
        Long id,
        String notificationType,
        String title,
        String body,
        LocalDateTime scheduledAt,
        LocalDateTime sentAt,
        String deliveryStatus,
        Long createdByUserId,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
