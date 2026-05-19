package com.example.petlife.dto.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class GeneralAppointmentForm {

    @NotNull(message = "対象ペットは必須です")
    private Long petId;

    @NotNull(message = "予約日時は必須です")
    @Future(message = "予約日時は未来日時を指定してください")
    private LocalDateTime scheduledAt;

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
