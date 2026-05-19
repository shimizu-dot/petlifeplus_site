package com.example.petlife.dto.notification;

import java.time.LocalDateTime;

public record NotificationRow(
        Long id,
        String notificationType,
        String title,
        String body,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public boolean isUnread() {
        return readAt == null;
    }
}
