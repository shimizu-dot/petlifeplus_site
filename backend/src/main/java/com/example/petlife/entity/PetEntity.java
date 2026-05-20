package com.example.petlife.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PetEntity(
        Long id,
        Long ownerUserId,
        String name,
        String species,
        String breed,
        String sex,
        LocalDate birthDate,
        BigDecimal weightBaselineKg,
        String imagePath,
        LocalDateTime deceasedAt,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
