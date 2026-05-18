package com.example.petlife.dto.symptom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SymptomCheckForm {
    @NotBlank
    @Size(max = 100)
    private String symptomType;

    @Size(max = 100)
    private String onsetText;

    @Size(max = 500)
    private String memo;
}
