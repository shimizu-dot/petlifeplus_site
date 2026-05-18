package com.example.petlife.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConsultChatForm {
    @NotBlank
    @Size(max = 1000)
    private String message;
}
