package com.example.petlife.mapper;

import com.example.petlife.entity.AppointmentEntity;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AppointmentMapper {
    @Select("""
        SELECT id, pet_id, owner_user_id, staff_user_id, appointment_type, channel, scheduled_at, status, note, deleted_at, created_at, updated_at
        FROM appointments WHERE deleted_at IS NULL ORDER BY scheduled_at DESC, id DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<AppointmentEntity> findAll(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM appointments WHERE deleted_at IS NULL")
    long countAll();

    @Select("""
        SELECT id, pet_id, owner_user_id, staff_user_id, appointment_type, channel, scheduled_at, status, note, deleted_at, created_at, updated_at
        FROM appointments WHERE id = #{id} AND deleted_at IS NULL
        """)
    AppointmentEntity findById(@Param("id") Long id);

    @Insert("""
        INSERT INTO appointments(pet_id, owner_user_id, staff_user_id, appointment_type, channel, scheduled_at, status, note, created_at, updated_at)
        VALUES(#{petId}, #{ownerUserId}, #{staffUserId}, #{appointmentType}, #{channel}, #{scheduledAt}, #{status}, #{note}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AppointmentEntity row);

    @Update("""
        UPDATE appointments
        SET staff_user_id = #{staffUserId}, appointment_type = #{appointmentType}, channel = #{channel},
            scheduled_at = #{scheduledAt}, status = #{status}, note = #{note}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int update(AppointmentEntity row);

    @Update("UPDATE appointments SET deleted_at = #{deletedAt}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND deleted_at IS NULL")
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

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
}
