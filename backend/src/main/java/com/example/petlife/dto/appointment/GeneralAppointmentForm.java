package com.example.petlife.dto.appointment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class GeneralAppointmentForm {

    @NotNull(message = "対象ペットは必須です")
    private Long petId;

    @NotNull(message = "予約時間を選択してください")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime scheduledAt;

    @Size(max = 500, message = "相談内容は500文字以内で入力してください")
    private String note;

    @Pattern(regexp = "^(VISIT|ONLINE)$", message = "予約方法は来院またはオンラインを選択してください")
    private String channel = "VISIT";

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
}
