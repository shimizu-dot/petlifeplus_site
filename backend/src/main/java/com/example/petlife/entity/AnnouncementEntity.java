package com.example.petlife.entity;

import java.time.LocalDateTime;

public record AnnouncementEntity(
        Long id,
        String title,
        String body,
        boolean isActive,
        Long createdByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
