package com.example.petlife.dto.health;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class HealthRecordForm {
    @NotNull @PastOrPresent private LocalDate recordDate;
                            private BigDecimal weightKg;
    @Size(max = 300)        private String mealMemo;
                            private Integer exerciseMinutes;
    @Size(max = 1000)       private String note;

    public HealthRecordCreateRequest toCreateRequest(Long petId, Long recordedByUserId) {
        return new HealthRecordCreateRequest(petId, recordedByUserId, recordDate, weightKg, mealMemo, exerciseMinutes, note);
    }

    public HealthRecordUpdateRequest toUpdateRequest() {
        return new HealthRecordUpdateRequest(recordDate, weightKg, mealMemo, exerciseMinutes, note);
    }
}
