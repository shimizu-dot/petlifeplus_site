package com.example.petlife.dto.calendar;

import java.time.LocalDateTime;

public record AppointmentCalendarRow(
        Long id,
        LocalDateTime scheduledAt,
        String channel,
        String status,
        String petName,
        String ownerName
) {}
