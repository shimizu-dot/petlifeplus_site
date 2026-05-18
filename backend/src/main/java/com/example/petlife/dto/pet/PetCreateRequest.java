package com.example.petlife.dto.pet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PetCreateRequest(
        @NotNull Long ownerUserId,
        @NotBlank @Size(max = 40) String name,
        @NotBlank @Size(max = 30) String species,
        @Size(max = 100) String breed,
        @Size(max = 10) String sex,
        @PastOrPresent LocalDate birthDate,
        BigDecimal weightBaselineKg,
        @Size(max = 500) String imagePath
) {
}
