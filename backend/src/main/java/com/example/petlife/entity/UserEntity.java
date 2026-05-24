package com.example.petlife.entity;

import java.time.LocalDateTime;

public record UserEntity(
        Long id,
        Long roleId,
        String name,
        String email,
        String passwordHash,
        String phone,
        String slackUserId,
        String lineUserId,
        String status,
        LocalDateTime lastLoginAt,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
