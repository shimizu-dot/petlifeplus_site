package com.example.petlife.dto.user;

public record UserResponse(
        Long id,
        Long roleId,
        String roleDisplay,
        String name,
        String email,
        String phone,
        String status
) {
}
