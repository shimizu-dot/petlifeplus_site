package com.example.petlife.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserForm {
    @NotNull                                     private Long roleId;
    @NotBlank @Size(max = 100)                   private String name;
    @NotBlank @Email @Size(max = 255)            private String email;
    @Size(min = 8, max = 64)                     private String password;
    @Pattern(regexp = "^[0-9-]{10,13}$|^$")     private String phone;
    private String status = "ACTIVE";

    public UserCreateRequest toCreateRequest() {
        return new UserCreateRequest(roleId, name, email, password, phone);
    }

    public UserUpdateRequest toUpdateRequest() {
        return new UserUpdateRequest(name, email, phone, status);
    }
}
