package com.example.petlife.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotNull Long roleId,
        @Pattern(regexp = "^(LIGHT|STANDARD|PREMIUM)?$", message = "planTier must be LIGHT/STANDARD/PREMIUM")
        String planTier,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 255) String email,
        String password,
        @Pattern(regexp = "^[0-9-]{10,13}$", message = "phone must be 10-13 digits/hyphen") String phone,
        @Size(max = 100) String slackUserId,
        @Size(max = 100) String lineUserId,
        @Pattern(regexp = "^(ACTIVE|INACTIVE|SUSPENDED)$", message = "status must be ACTIVE/INACTIVE/SUSPENDED")
        String status
) {
}
