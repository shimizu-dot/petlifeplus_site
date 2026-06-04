package com.example.petlife.mapper;

import com.example.petlife.entity.AppointmentBusinessHoursEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalTime;

@Mapper
public interface AppointmentBusinessHoursMapper {

    @Select("""
        SELECT id,
               business_start AS "businessStart",
               business_end AS "businessEnd",
               slot_minutes AS "slotMinutes",
               updated_by_user_id AS "updatedByUserId",
               created_at AS "createdAt",
               updated_at AS "updatedAt"
        FROM appointment_business_hours
        WHERE id = 1
        """)
    AppointmentBusinessHoursEntity findCurrent();

    @Update("""
        INSERT INTO appointment_business_hours(
            id, business_start, business_end, slot_minutes, updated_by_user_id, created_at, updated_at
        )
        VALUES(1, #{businessStart}, #{businessEnd}, #{slotMinutes}, #{updatedByUserId}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (id) DO UPDATE
        SET business_start = EXCLUDED.business_start,
            business_end = EXCLUDED.business_end,
            slot_minutes = EXCLUDED.slot_minutes,
            updated_by_user_id = EXCLUDED.updated_by_user_id,
            updated_at = CURRENT_TIMESTAMP
        """)
    int upsert(@Param("businessStart") LocalTime businessStart,
               @Param("businessEnd") LocalTime businessEnd,
               @Param("slotMinutes") int slotMinutes,
               @Param("updatedByUserId") Long updatedByUserId);
}
