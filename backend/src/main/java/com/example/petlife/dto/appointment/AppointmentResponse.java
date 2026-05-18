package com.example.petlife.dto.appointment;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long petId,
        Long ownerUserId,
        Long staffUserId,
        String appointmentType,
        String channel,
        LocalDateTime scheduledAt,
        String status,
        String zoomJoinUrl,
        String note
) {
}
