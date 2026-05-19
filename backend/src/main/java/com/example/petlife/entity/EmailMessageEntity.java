package com.example.petlife.entity;

import java.time.LocalDateTime;

public record EmailMessageEntity(
        Long id,
        Long templateId,
        Long recipientUserId,
        Long petId,
        Long appointmentId,
        Long invoiceId,
        String subject,
        String body,
        LocalDateTime sendTimingAt,
        String status,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime sentAt
) {
}
