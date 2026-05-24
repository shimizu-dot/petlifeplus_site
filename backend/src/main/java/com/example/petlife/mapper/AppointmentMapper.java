package com.example.petlife.mapper;

import com.example.petlife.entity.AppointmentEntity;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AppointmentMapper {
    @Select("""
        SELECT id, pet_id, owner_user_id, staff_user_id, appointment_type, channel, scheduled_at, status, zoom_join_url, note, slot_id, deleted_at, created_at, updated_at
        FROM appointments WHERE deleted_at IS NULL ORDER BY scheduled_at DESC, id DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<AppointmentEntity> findAll(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM appointments WHERE deleted_at IS NULL")
    long countAll();

    @Select("""
        SELECT id, pet_id, owner_user_id, staff_user_id, appointment_type, channel, scheduled_at, status, zoom_join_url, note, slot_id, deleted_at, created_at, updated_at
        FROM appointments WHERE id = #{id} AND deleted_at IS NULL
        """)
    AppointmentEntity findById(@Param("id") Long id);

    @Select("""
        INSERT INTO appointments(pet_id, owner_user_id, staff_user_id, appointment_type, channel, scheduled_at, status, zoom_join_url, note, slot_id, created_at, updated_at)
        VALUES(#{petId}, #{ownerUserId}, #{staffUserId}, #{appointmentType}, #{channel}, #{scheduledAt}, #{status}, #{zoomJoinUrl}, #{note}, #{slotId}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insert(AppointmentEntity row);

    @Update("""
        UPDATE appointments
        SET staff_user_id = #{staffUserId}, appointment_type = #{appointmentType}, channel = #{channel},
            scheduled_at = #{scheduledAt}, status = #{status}, zoom_join_url = #{zoomJoinUrl}, note = #{note}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int update(AppointmentEntity row);

    @Update("UPDATE appointments SET deleted_at = #{deletedAt}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND deleted_at IS NULL")
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    @Update("UPDATE appointments SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND deleted_at IS NULL")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Select("""
        SELECT COUNT(*) FROM appointments
        WHERE deleted_at IS NULL
          AND staff_user_id = #{staffUserId}
          AND scheduled_at = #{scheduledAt}
          AND status IN ('REQUESTED', 'CONFIRMED')
          AND (#{excludeId} IS NULL OR id <> #{excludeId})
        """)
    int countDuplicatedSlot(@Param("staffUserId") Long staffUserId,
                            @Param("scheduledAt") LocalDateTime scheduledAt,
                            @Param("excludeId") Long excludeId);

    @Select("""
        SELECT a.id, p.name AS "petName", u.name AS "ownerName", a.appointment_type AS "appointmentType",
               a.channel, a.scheduled_at AS "scheduledAt", a.status, a.note, a.zoom_join_url AS "zoomJoinUrl"
        FROM appointments a
        INNER JOIN pets p ON p.id = a.pet_id
        INNER JOIN users u ON u.id = a.owner_user_id
        WHERE a.deleted_at IS NULL
        ORDER BY a.scheduled_at DESC, a.id DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<com.example.petlife.dto.appointment.AppointmentListRow> findAllRows(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT a.id, p.name AS "petName", u.name AS "ownerName", a.appointment_type AS "appointmentType",
               a.channel, a.scheduled_at AS "scheduledAt", a.status, a.note, a.zoom_join_url AS "zoomJoinUrl"
        FROM appointments a
        INNER JOIN pets p ON p.id = a.pet_id
        INNER JOIN users u ON u.id = a.owner_user_id
        WHERE a.deleted_at IS NULL AND a.owner_user_id = #{ownerUserId}
        ORDER BY a.scheduled_at DESC, a.id DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<com.example.petlife.dto.appointment.AppointmentListRow> findRowsByOwnerUserId(@Param("ownerUserId") Long ownerUserId,
                                                                                         @Param("limit") int limit,
                                                                                         @Param("offset") int offset);

    @Select("""
        SELECT a.id, p.name AS "petName", u.name AS "ownerName", a.appointment_type AS "appointmentType",
               a.channel, a.scheduled_at AS "scheduledAt", a.status, a.note, a.zoom_join_url AS "zoomJoinUrl"
        FROM appointments a
        INNER JOIN pets p ON p.id = a.pet_id
        INNER JOIN users u ON u.id = a.owner_user_id
        WHERE a.deleted_at IS NULL
        ORDER BY p.name ASC, a.scheduled_at ASC, a.id ASC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<com.example.petlife.dto.appointment.AppointmentListRow> findAllRowsOrderByPet(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT a.id, p.name AS "petName", u.name AS "ownerName", a.appointment_type AS "appointmentType",
               a.channel, a.scheduled_at AS "scheduledAt", a.status, a.note, a.zoom_join_url AS "zoomJoinUrl"
        FROM appointments a
        INNER JOIN pets p ON p.id = a.pet_id
        INNER JOIN users u ON u.id = a.owner_user_id
        WHERE a.deleted_at IS NULL
          AND a.owner_user_id = #{ownerUserId}
        ORDER BY p.name ASC, a.scheduled_at ASC, a.id ASC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<com.example.petlife.dto.appointment.AppointmentListRow> findRowsByOwnerUserIdOrderByPet(@Param("ownerUserId") Long ownerUserId,
                                                                                                   @Param("limit") int limit,
                                                                                                   @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM appointments WHERE deleted_at IS NULL AND owner_user_id = #{ownerUserId}")
    long countByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    @Select("SELECT COUNT(*) FROM appointments WHERE deleted_at IS NULL AND status = #{status}")
    long countByStatus(@Param("status") String status);

    @Select("""
        SELECT COUNT(*) FROM appointments
        WHERE deleted_at IS NULL
          AND scheduled_at = #{scheduledAt}
          AND status IN ('REQUESTED', 'CONFIRMED')
        """)
    int countByScheduledAt(@Param("scheduledAt") java.time.LocalDateTime scheduledAt);

    @Select("""
        SELECT scheduled_at FROM appointments
        WHERE deleted_at IS NULL
          AND CAST(scheduled_at AS DATE) = #{date}
          AND status IN ('REQUESTED', 'CONFIRMED')
        """)
    List<java.time.LocalDateTime> findBookedTimesOnDate(@Param("date") java.time.LocalDate date);

    @Select("""
        <script>
        SELECT id, pet_id, owner_user_id, staff_user_id, appointment_type, channel,
               scheduled_at, status, zoom_join_url, note, slot_id,
               deleted_at, created_at, updated_at
        FROM appointments
        WHERE deleted_at IS NULL
          AND id IN
          <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
        </script>
        """)
    List<AppointmentEntity> findByIds(@Param("ids") List<Long> ids);

    @Select("""
        SELECT a.id, a.scheduled_at AS "scheduledAt", a.channel, a.status,
               p.name AS "petName", u.name AS "ownerName"
        FROM appointments a
        JOIN pets p ON p.id = a.pet_id
        JOIN users u ON u.id = a.owner_user_id
        WHERE a.deleted_at IS NULL
          AND a.status IN ('REQUESTED', 'CONFIRMED')
          AND CAST(a.scheduled_at AS DATE) BETWEEN #{start} AND #{finish}
        ORDER BY a.scheduled_at
        """)
    List<com.example.petlife.dto.calendar.AppointmentCalendarRow> findByScheduledDateRange(
            @Param("start") java.time.LocalDate start,
            @Param("finish") java.time.LocalDate finish);

    @Select("""
        SELECT DISTINCT CAST(scheduled_at AS DATE)
        FROM appointments
        WHERE deleted_at IS NULL
          AND owner_user_id = #{ownerUserId}
          AND status = 'CONFIRMED'
          AND CAST(scheduled_at AS DATE) BETWEEN #{start} AND #{finish}
        """)
    List<java.time.LocalDate> findConfirmedDatesByOwnerUserId(
            @Param("ownerUserId") Long ownerUserId,
            @Param("start") java.time.LocalDate start,
            @Param("finish") java.time.LocalDate finish);
}
