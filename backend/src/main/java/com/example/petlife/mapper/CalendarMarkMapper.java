package com.example.petlife.mapper;

import com.example.petlife.entity.CalendarMarkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CalendarMarkMapper {

    @Select("""
        INSERT INTO pet_calendar_marks(
            pet_id, created_by_user_id, mark_date, mark_type, memo, created_at, updated_at
        )
        VALUES(
            #{petId}, #{createdByUserId}, #{markDate}, #{markType}, #{memo}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        RETURNING id
        """)
    Long insertReturningId(CalendarMarkEntity row);

    @Update("""
        UPDATE pet_calendar_marks m
        SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
        FROM pets p
        WHERE m.id = #{id}
          AND p.id = m.pet_id
          AND p.owner_user_id = #{ownerUserId}
          AND m.deleted_at IS NULL
        """)
    int softDeleteOwnedMark(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);

    @Select("""
        SELECT m.id, m.pet_id, m.created_by_user_id, m.mark_date, m.mark_type, m.memo,
               m.deleted_at, m.created_at, m.updated_at
        FROM pet_calendar_marks m
        JOIN pets p ON p.id = m.pet_id
        WHERE p.owner_user_id = #{ownerUserId}
          AND p.deleted_at IS NULL
          AND m.deleted_at IS NULL
          AND m.mark_date >= #{fromDate}
          AND m.mark_date <= #{toDate}
        ORDER BY m.mark_date ASC, m.id ASC
        """)
    List<CalendarMarkEntity> findByOwnerUserIdAndDateRange(
            @Param("ownerUserId") Long ownerUserId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Select("""
        SELECT m.id, m.pet_id, m.created_by_user_id, m.mark_date, m.mark_type, m.memo,
               m.deleted_at, m.created_at, m.updated_at
        FROM pet_calendar_marks m
        JOIN pets p ON p.id = m.pet_id
        WHERE m.id = #{id}
          AND p.owner_user_id = #{ownerUserId}
          AND m.deleted_at IS NULL
        """)
    CalendarMarkEntity findOwnedActiveById(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);
}
