package com.example.petlife.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        Long roleId,
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 64) String password,
        @Pattern(regexp = "^[0-9-]{10,13}$", message = "phone must be 10-13 digits/hyphen") String phone,
        @Size(max = 100) String slackUserId,
        @Size(max = 100) String lineUserId
) {
}
