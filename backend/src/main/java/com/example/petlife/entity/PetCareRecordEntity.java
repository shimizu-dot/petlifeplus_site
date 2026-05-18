package com.example.petlife.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PetCareRecordEntity(
        Long id,
        Long petId,
        Long recordedByUserId,
        String careType,
        LocalDate administeredOn,
        LocalDate nextDueOn,
        String memo,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
