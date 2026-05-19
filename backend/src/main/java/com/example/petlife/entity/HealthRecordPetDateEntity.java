package com.example.petlife.entity;

import java.time.LocalDate;

public record HealthRecordPetDateEntity(
        Long petId,
        LocalDate recordDate
) {
}
