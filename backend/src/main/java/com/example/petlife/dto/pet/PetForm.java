package com.example.petlife.dto.pet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PetForm {
    @NotBlank @Size(max = 100) private String name;
    @NotBlank @Size(max = 30)  private String species;
    @Size(max = 100)           private String breed;
    @Size(max = 10)            private String sex;
    @PastOrPresent             private LocalDate birthDate;
                               private BigDecimal weightBaselineKg;

    public PetCreateRequest toCreateRequest(Long ownerUserId) {
        return new PetCreateRequest(ownerUserId, name, species, breed, sex, birthDate, weightBaselineKg, null);
    }

    public PetUpdateRequest toUpdateRequest(String imagePath) {
        return new PetUpdateRequest(name, species, breed, sex, birthDate, weightBaselineKg, imagePath);
    }
}
