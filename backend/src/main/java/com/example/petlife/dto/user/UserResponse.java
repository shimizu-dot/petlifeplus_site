package com.example.petlife.dto.user;

public record UserResponse(
        Long id,
        Long roleId,
        String name,
        String email,
        String phone,
        String status
) {
}
