package com.example.petlife.dto.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AppointmentCreateRequest(
        @NotNull Long petId,
        @NotNull Long ownerUserId,
        Long staffUserId,
        @NotBlank String appointmentType,
        @NotBlank String channel,
        @NotNull @Future LocalDateTime scheduledAt,
        @NotBlank String status,
        @Size(max = 500) String note
) {
}
