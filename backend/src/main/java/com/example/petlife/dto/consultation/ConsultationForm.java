package com.example.petlife.dto.consultation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class ConsultationForm {

    @NotNull(message = "ペットを選択してください")
    private Long petId;

    private Long appointmentId;

    @NotNull(message = "実施日は必須です")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate performedOn;

    @NotBlank(message = "治療内容は必須です")
    @Size(max = 1000)
    private String treatmentDetail;

    @Size(max = 1000)
    private String diagnosis;

    @Size(max = 1000)
    private String prescription;
}
