package com.example.petlife.dto.health;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class HealthRecordForm {
    private LocalDate       recordDate;
    @DecimalMin("0.0") @DecimalMax("100.0")
                            private BigDecimal weightKg;
    @Size(max = 300)        private String mealMemo;
    @Min(0) @Max(480)       private Integer exerciseMinutes;
    @Min(1) @Max(5)         private Integer mealScore;
    @Min(1) @Max(5)         private Integer exerciseScore;
    @Min(1) @Max(5)         private Integer sleepScore;
    @Min(1) @Max(5)         private Integer moodScore;
    @Min(1) @Max(5)         private Integer overallScore;
    @Size(max = 1000)       private String note;

    public HealthRecordCreateRequest toCreateRequest(Long petId, Long recordedByUserId, String imagePath) {
        return new HealthRecordCreateRequest(petId, recordedByUserId, recordDate, weightKg, mealMemo, exerciseMinutes,
                mealScore, exerciseScore, sleepScore, moodScore, overallScore, imagePath, note);
    }

    public HealthRecordUpdateRequest toUpdateRequest(String imagePath) {
        return new HealthRecordUpdateRequest(recordDate, weightKg, mealMemo, exerciseMinutes,
                mealScore, exerciseScore, sleepScore, moodScore, overallScore, imagePath, note);
    }
}
