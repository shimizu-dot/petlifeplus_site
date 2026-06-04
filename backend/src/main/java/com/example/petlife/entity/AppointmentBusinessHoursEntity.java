package com.example.petlife.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record AppointmentBusinessHoursEntity(
        Long id,
        LocalTime businessStart,
        LocalTime businessEnd,
        Integer slotMinutes,
        Long updatedByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
