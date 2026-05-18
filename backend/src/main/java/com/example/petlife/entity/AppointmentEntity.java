package com.example.petlife.entity;

import java.time.LocalDateTime;

public record AppointmentEntity(
        Long id,
        Long petId,
        Long ownerUserId,
        Long staffUserId,
        String appointmentType,
        String channel,
        LocalDateTime scheduledAt,
        String status,
        String note,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
