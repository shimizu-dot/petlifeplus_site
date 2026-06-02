package com.example.petlife.entity;

import java.time.LocalDateTime;

public record LineLinkTokenEntity(
        Long id,
        Long userId,
        String token,
        LocalDateTime expiresAt,
        LocalDateTime usedAt,
        LocalDateTime createdAt
) {
}
