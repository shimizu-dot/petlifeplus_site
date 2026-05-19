package com.example.petlife.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MedicalHistoryEntity(
        Long id,
        Long petId,
        Long appointmentId,
        Long handledByUserId,
        LocalDate performedOn,
        String treatmentDetail,
        String diagnosis,
        String prescription,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
