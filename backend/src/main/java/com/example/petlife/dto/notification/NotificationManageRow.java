package com.example.petlife.dto.notification;

import java.time.LocalDateTime;

public record NotificationManageRow(
        Long id,
        String notificationType,
        String title,
        String body,
        LocalDateTime scheduledAt,
        LocalDateTime sentAt,
        String deliveryStatus,
        LocalDateTime createdAt
) {}
