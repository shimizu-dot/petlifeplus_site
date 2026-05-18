package com.example.petlife.dto.pet;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PetResponse(
        Long id,
        Long ownerUserId,
        String name,
        String species,
        String breed,
        String sex,
        LocalDate birthDate,
        BigDecimal weightBaselineKg,
        String imagePath
) {
}
