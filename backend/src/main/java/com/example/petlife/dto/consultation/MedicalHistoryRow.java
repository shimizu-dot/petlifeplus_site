package com.example.petlife.dto.consultation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MedicalHistoryRow(
        Long id,
        Long petId,
        String petName,
        String handlerName,
        LocalDate performedOn,
        String treatmentDetail,
        String diagnosis,
        String prescription,
        LocalDateTime createdAt
) {
}
