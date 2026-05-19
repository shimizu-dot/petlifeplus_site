package com.example.petlife.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CalendarMarkEntity(
        Long id,
        Long petId,
        Long createdByUserId,
        LocalDate markDate,
        String markType,
        String memo,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
