package com.example.petlife.mapper;

import com.example.petlife.entity.AppointmentSlotEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AppointmentSlotMapper {

    @Select("""
        INSERT INTO appointment_slots(slot_datetime, note, is_blocked, created_by_user_id, created_at)
        VALUES(#{slotDatetime}, #{note}, #{isBlocked}, #{createdByUserId}, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insert(AppointmentSlotEntity row);

    @Select("""
        SELECT id, slot_datetime AS "slotDatetime", note, is_blocked AS "isBlocked",
               created_by_user_id AS "createdByUserId",
               deleted_at AS "deletedAt", created_at AS "createdAt"
        FROM appointment_slots WHERE id = #{id} AND deleted_at IS NULL
        """)
    AppointmentSlotEntity findById(@Param("id") Long id);

    @Select("""
        SELECT id, slot_datetime AS "slotDatetime", note, is_blocked AS "isBlocked",
               created_by_user_id AS "createdByUserId",
               deleted_at AS "deletedAt", created_at AS "createdAt"
        FROM appointment_slots
        WHERE deleted_at IS NULL AND slot_datetime > CURRENT_TIMESTAMP
          AND is_blocked = FALSE
          AND NOT EXISTS (
            SELECT 1 FROM appointments a
            WHERE a.slot_id = appointment_slots.id
              AND a.status IN ('REQUESTED', 'CONFIRMED')
              AND a.deleted_at IS NULL
          )
        ORDER BY slot_datetime
        """)
    List<AppointmentSlotEntity> findAvailable();

    @Select("""
        SELECT id, slot_datetime AS "slotDatetime", note, is_blocked AS "isBlocked",
               created_by_user_id AS "createdByUserId",
               deleted_at AS "deletedAt", created_at AS "createdAt"
        FROM appointment_slots
        WHERE deleted_at IS NULL
          AND CAST(slot_datetime AS DATE) BETWEEN #{start} AND #{finish}
          AND is_blocked = FALSE
          AND NOT EXISTS (
            SELECT 1 FROM appointments a
            WHERE a.slot_id = appointment_slots.id
              AND a.status IN ('REQUESTED', 'CONFIRMED')
              AND a.deleted_at IS NULL
          )
        ORDER BY slot_datetime
        """)
    List<AppointmentSlotEntity> findAvailableInDateRange(
            @Param("start") LocalDate start,
            @Param("finish") LocalDate finish);

    /** 指定日の全登録枠（ブロック枠 + 追加枠）一覧 — 予約枠管理画面で使用 */
    @Select("""
        SELECT id, slot_datetime AS "slotDatetime", note, is_blocked AS "isBlocked",
               created_by_user_id AS "createdByUserId",
               deleted_at AS "deletedAt", created_at AS "createdAt"
        FROM appointment_slots
        WHERE deleted_at IS NULL
          AND CAST(slot_datetime AS DATE) = #{date}
        ORDER BY slot_datetime
        """)
    List<AppointmentSlotEntity> findAllOnDate(@Param("date") LocalDate date);

    /** 指定日のブロック枠（is_blocked=true）一覧 — generateAvailableSlots() で自動生成スロットから除外するために使用 */
    @Select("""
        SELECT id, slot_datetime AS "slotDatetime", note, is_blocked AS "isBlocked",
               created_by_user_id AS "createdByUserId",
               deleted_at AS "deletedAt", created_at AS "createdAt"
        FROM appointment_slots
        WHERE deleted_at IS NULL
          AND is_blocked = TRUE
          AND CAST(slot_datetime AS DATE) = #{date}
        ORDER BY slot_datetime
        """)
    List<AppointmentSlotEntity> findBlockedOnDate(@Param("date") LocalDate date);

    /** 指定日の追加枠（is_blocked=false）一覧 — generateAvailableSlots() に上乗せするために使用 */
    @Select("""
        SELECT id, slot_datetime AS "slotDatetime", note, is_blocked AS "isBlocked",
               created_by_user_id AS "createdByUserId",
               deleted_at AS "deletedAt", created_at AS "createdAt"
        FROM appointment_slots
        WHERE deleted_at IS NULL
          AND is_blocked = FALSE
          AND CAST(slot_datetime AS DATE) = #{date}
        ORDER BY slot_datetime
        """)
    List<AppointmentSlotEntity> findExtraOnDate(@Param("date") LocalDate date);

    @Select("""
        SELECT s.id, s.slot_datetime AS "slotDatetime", s.note, s.is_blocked AS "isBlocked",
               s.created_by_user_id AS "createdByUserId",
               s.deleted_at AS "deletedAt", s.created_at AS "createdAt"
        FROM appointment_slots s
        WHERE s.deleted_at IS NULL
        ORDER BY s.slot_datetime DESC
        """)
    List<AppointmentSlotEntity> findAll();

    @Select("""
        SELECT COUNT(*) FROM appointments
        WHERE slot_id = #{slotId} AND status IN ('REQUESTED', 'CONFIRMED') AND deleted_at IS NULL
        """)
    int countBookings(@Param("slotId") Long slotId);

    @Update("""
        UPDATE appointment_slots SET deleted_at = #{deletedAt}
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
