package com.example.petlife.entity;

import java.time.LocalDateTime;

public record SymptomCheckEntity(
        Long id,
        Long petId,
        Long requestedByUserId,
        String symptomType,
        String onsetText,
        String memo,
        String severity,
        String recommendation,
        String aiModel,
        LocalDateTime createdAt
) {
}
