package com.example.petlife.dto.subscription;

import java.time.LocalDateTime;

public record RenewalHistoryRow(
        Long subscriptionId,
        String planName,
        LocalDateTime requestedAt,
        String status
) {
}
