package com.example.petlife.dto.premium;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class PremiumOnlineCareForm {

    @NotNull(message = "対象ペットを選択してください")
    private Long petId;

    @NotNull(message = "予約時間を選択してください")
    @Future(message = "予約時間は未来の日時を選択してください")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime scheduledAt;

    @NotBlank(message = "相談内容を入力してください")
    @Size(max = 500, message = "相談内容は500文字以内で入力してください")
    private String note;

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
