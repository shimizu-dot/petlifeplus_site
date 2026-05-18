package com.example.petlife.dto.health;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HealthRecordUpdateRequest(
        @NotNull @PastOrPresent LocalDate recordDate,
        BigDecimal weightKg,
        @Size(max = 300) String mealMemo,
        Integer exerciseMinutes,
        String note
) {
}
