package com.example.petlife.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record HealthRecordEntity(
        Long id,
        Long petId,
        Long recordedByUserId,
        LocalDate recordDate,
        BigDecimal weightKg,
        String mealMemo,
        Integer exerciseMinutes,
        Integer mealScore,
        Integer exerciseScore,
        Integer sleepScore,
        Integer moodScore,
        Integer overallScore,
        String imagePath,
        String note,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
