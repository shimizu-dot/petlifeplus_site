package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.entity.AppointmentBusinessHoursEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.mapper.AppointmentBusinessHoursMapper;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
public class AppointmentBusinessHoursService {

    private static final LocalTime DEFAULT_START = LocalTime.of(9, 30);
    private static final LocalTime DEFAULT_END = LocalTime.of(17, 0);
    private static final int DEFAULT_SLOT_MINUTES = 30;

    private final AppointmentBusinessHoursMapper mapper;

    public AppointmentBusinessHoursService(AppointmentBusinessHoursMapper mapper) {
        this.mapper = mapper;
    }

    public AppointmentBusinessHoursEntity getCurrent() {
        AppointmentBusinessHoursEntity row = mapper.findCurrent();
        if (row != null) {
            return row;
        }
        return new AppointmentBusinessHoursEntity(
                1L,
                DEFAULT_START,
                DEFAULT_END,
                DEFAULT_SLOT_MINUTES,
                null,
                null,
                null
        );
    }

    public void update(LocalTime businessStart, LocalTime businessEnd, Integer slotMinutes, LoginUser currentUser) {
        if (currentUser == null || !currentUser.canConfigureBusinessHours()) {
            throw new BadRequestException("管理者のみ営業時間を設定できます");
        }
        if (businessStart == null || businessEnd == null) {
            throw new BadRequestException("開始時刻と終了時刻は必須です");
        }
        int effectiveSlotMinutes = slotMinutes == null ? DEFAULT_SLOT_MINUTES : slotMinutes;
        if (!businessStart.isBefore(businessEnd)) {
            throw new BadRequestException("開始時刻は終了時刻より前にしてください");
        }
        if (effectiveSlotMinutes < 5 || effectiveSlotMinutes > 120) {
            throw new BadRequestException("枠の間隔は5〜120分で指定してください");
        }
        if (!businessStart.plusMinutes(effectiveSlotMinutes).isBefore(businessEnd.plusSeconds(1))) {
            throw new BadRequestException("営業時間に対して枠の間隔が長すぎます");
        }
        mapper.upsert(businessStart, businessEnd, effectiveSlotMinutes, currentUser.id());
    }
}
