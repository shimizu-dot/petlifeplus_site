package com.example.petlife.dto.pet;

public record PetCareContextRow(
        Long petId,
        Long ownerUserId,
        String ownerName,
        String planName
) {
}
