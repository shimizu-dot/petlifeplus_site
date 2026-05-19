package com.example.petlife.dto.appointment;

import java.time.LocalDateTime;

public record AppointmentListRow(
        Long id,
        String petName,
        String ownerName,
        String appointmentType,
        String channel,
        LocalDateTime scheduledAt,
        String status,
        String note
) {
}
