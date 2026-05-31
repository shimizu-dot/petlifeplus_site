package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.appointment.DaySlotRow;
import com.example.petlife.entity.AppointmentSlotEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.exception.NotFoundException;
import com.example.petlife.mapper.AppointmentMapper;
import com.example.petlife.mapper.AppointmentSlotMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AppointmentSlotService {

    private static final LocalTime BUSINESS_START = LocalTime.of(9, 30);
    private static final LocalTime BUSINESS_END   = LocalTime.of(17, 0);
    private static final int SLOT_MINUTES = 30;

    private final AppointmentSlotMapper slotMapper;
    private final AppointmentMapper appointmentMapper;

    public AppointmentSlotService(AppointmentSlotMapper slotMapper,
                                  AppointmentMapper appointmentMapper) {
        this.slotMapper = slotMapper;
        this.appointmentMapper = appointmentMapper;
    }

    public List<AppointmentSlotEntity> list(LoginUser currentUser) {
        ensureAccess(currentUser);
        return slotMapper.findAll();
    }

    /**
     * 指定日の全スロット状態一覧を返す（予約枠管理画面用）。
     * 自動生成スロット（9:30-17:00）と追加枠・ブロック枠を統合し、各行の状態を付与する。
     */
    public List<DaySlotRow> getDaySlotsWithStatus(LocalDate date) {
        // 承認済み/申請中の時刻を分離（管理画面表示用）
        Set<LocalDateTime> requested = new HashSet<>(appointmentMapper.findRequestedTimesOnDate(date));
        Set<LocalDateTime> booked = new HashSet<>(appointmentMapper.findBookedTimesOnDate(date));
        booked.removeAll(requested); // CONFIRMED のみを「予約済み」として扱う

        // 当日の登録枠（追加枠 + ブロック枠）を時刻でインデックス
        Map<LocalDateTime, AppointmentSlotEntity> registeredMap = new LinkedHashMap<>();
        for (AppointmentSlotEntity s : slotMapper.findAllOnDate(date)) {
            registeredMap.put(s.slotDatetime(), s);
        }

        // 全表示対象時刻 = 自動生成ベース + 追加枠
        Set<LocalDateTime> allTimes = new LinkedHashSet<>();
        LocalTime t = BUSINESS_START;
        while (!t.isAfter(BUSINESS_END.minusMinutes(1))) {
            allTimes.add(LocalDateTime.of(date, t));
            t = t.plusMinutes(SLOT_MINUTES);
        }
        for (AppointmentSlotEntity s : registeredMap.values()) {
            if (!Boolean.TRUE.equals(s.isBlocked())) {
                allTimes.add(s.slotDatetime());
            }
        }

        LocalDateTime now = LocalDateTime.now();

        List<DaySlotRow> rows = new ArrayList<>();
        for (LocalDateTime dt : allTimes) {
            AppointmentSlotEntity reg = registeredMap.get(dt);
            boolean isPast    = dt.isBefore(now);
            boolean isBlocked = reg != null && Boolean.TRUE.equals(reg.isBlocked());
            boolean isExtra   = reg != null && !Boolean.TRUE.equals(reg.isBlocked());
            boolean isRequested = requested.contains(dt);
            boolean isBooked  = booked.contains(dt);

            String status;
            if (isPast)          status = "PAST";
            else if (isBlocked)  status = "AUTO_BLOCKED";
            else if (isExtra)    status = isBooked ? "EXTRA_BOOKED" : (isRequested ? "EXTRA_REQUESTED" : "EXTRA_AVAILABLE");
            else                 status = isBooked ? "AUTO_BOOKED"  : (isRequested ? "AUTO_REQUESTED" : "AUTO_AVAILABLE");

            rows.add(new DaySlotRow(dt, status,
                    reg != null ? reg.id()   : null,
                    reg != null ? reg.note() : null));
        }

        return rows.stream().sorted(Comparator.comparing(DaySlotRow::slotTime)).toList();
    }

    public void create(LocalDateTime slotDatetime, String note, boolean isBlocked, LoginUser currentUser) {
        ensureAccess(currentUser);
        if (slotDatetime == null || !slotDatetime.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("予約枠は未来日時で指定してください");
        }
        slotMapper.insert(new AppointmentSlotEntity(null, slotDatetime, note, isBlocked, currentUser.id(), null, null));
    }

    public void delete(Long id, LoginUser currentUser) {
        ensureAccess(currentUser);
        if (slotMapper.findById(id) == null) {
            throw new NotFoundException("予約枠が見つかりません: " + id);
        }
        if (slotMapper.countBookings(id) > 0) {
            throw new BadRequestException("申請済みの予約枠は削除できません");
        }
        slotMapper.softDelete(id, LocalDateTime.now());
    }

    private void ensureAccess(LoginUser currentUser) {
        if (!currentUser.canManageOperations()) {
            throw new BadRequestException("管理者・スタッフのみアクセスできます");
        }
    }
}
