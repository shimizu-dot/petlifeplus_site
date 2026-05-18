package com.example.petlife.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Pattern(regexp = "^[0-9-]{10,13}$", message = "phone must be 10-13 digits/hyphen") String phone,
        @NotBlank String status
) {
}
