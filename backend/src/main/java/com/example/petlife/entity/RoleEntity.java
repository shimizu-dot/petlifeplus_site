package com.example.petlife.entity;

import java.time.LocalDateTime;

public record RoleEntity(
        Long id,
        String roleCode,
        String roleName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
