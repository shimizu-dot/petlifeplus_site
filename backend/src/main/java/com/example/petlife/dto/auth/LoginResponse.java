package com.example.petlife.dto.auth;

public record LoginResponse(Long userId, String role, String message) {
}
