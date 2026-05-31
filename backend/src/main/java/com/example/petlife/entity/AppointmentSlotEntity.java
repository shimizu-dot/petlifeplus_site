package com.example.petlife.entity;

import java.time.LocalDateTime;

public record AppointmentSlotEntity(
        Long id,
        LocalDateTime slotDatetime,
        String note,
        Boolean isBlocked,
        Long createdByUserId,
        LocalDateTime deletedAt,
        LocalDateTime createdAt
) {
}
