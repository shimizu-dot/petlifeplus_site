package com.example.petlife.dto.health;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HealthRecordResponse(
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
        String note
) {
}
