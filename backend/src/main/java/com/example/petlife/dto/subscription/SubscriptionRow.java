package com.example.petlife.dto.subscription;

import java.time.LocalDate;

public record SubscriptionRow(
        Long id,
        String planName,
        String petName,
        String ownerName,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        boolean autoRenew
) {
}
