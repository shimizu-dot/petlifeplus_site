package com.example.petlife.dto.health;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HealthRecordUpdateRequest(
        @NotNull @PastOrPresent LocalDate recordDate,
        BigDecimal weightKg,
        @Size(max = 300) String mealMemo,
        Integer exerciseMinutes,
        @Min(1) @Max(5) Integer mealScore,
        @Min(1) @Max(5) Integer exerciseScore,
        @Min(1) @Max(5) Integer sleepScore,
        @Min(1) @Max(5) Integer moodScore,
        @Min(1) @Max(5) Integer overallScore,
        @Size(max = 500) String imagePath,
        String note
) {
}
