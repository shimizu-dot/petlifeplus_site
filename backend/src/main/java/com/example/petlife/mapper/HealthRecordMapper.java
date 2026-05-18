package com.example.petlife.mapper;

import com.example.petlife.entity.HealthRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface HealthRecordMapper {

    // ---- 全件（管理者用） ----
    @Select("""
        SELECT id, pet_id, recorded_by_user_id, record_date, weight_kg, meal_memo,
               exercise_minutes, meal_score, exercise_score, sleep_score, mood_score, overall_score, image_path,
               note, deleted_at, created_at, updated_at
        FROM health_records WHERE deleted_at IS NULL ORDER BY record_date DESC, id DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<HealthRecordEntity> findAll(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM health_records WHERE deleted_at IS NULL")
    long countAll();

    // ---- ペット別（オーナーのペット絞り込み済み前提） ----
    @Select("""
        SELECT id, pet_id, recorded_by_user_id, record_date, weight_kg, meal_memo,
               exercise_minutes, meal_score, exercise_score, sleep_score, mood_score, overall_score, image_path,
               note, deleted_at, created_at, updated_at
        FROM health_records WHERE pet_id = #{petId} AND deleted_at IS NULL
        ORDER BY record_date DESC, id DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<HealthRecordEntity> findByPetId(@Param("petId") Long petId,
                                          @Param("limit") int limit,
                                          @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM health_records WHERE pet_id = #{petId} AND deleted_at IS NULL")
    long countByPetId(@Param("petId") Long petId);

    // ---- 単件 ----
    @Select("""
        SELECT id, pet_id, recorded_by_user_id, record_date, weight_kg, meal_memo,
               exercise_minutes, meal_score, exercise_score, sleep_score, mood_score, overall_score, image_path,
               note, deleted_at, created_at, updated_at
        FROM health_records WHERE id = #{id} AND deleted_at IS NULL
        """)
    HealthRecordEntity findById(@Param("id") Long id);

    // ---- 更新系 ----
    @Select("""
        INSERT INTO health_records(pet_id, recorded_by_user_id, record_date, weight_kg,
                                   meal_memo, exercise_minutes, meal_score, exercise_score, sleep_score, mood_score, overall_score,
                                   image_path, note, created_at, updated_at)
        VALUES(#{petId}, #{recordedByUserId}, #{recordDate}, #{weightKg}, #{mealMemo},
               #{exerciseMinutes}, #{mealScore}, #{exerciseScore}, #{sleepScore}, #{moodScore}, #{overallScore},
               #{imagePath}, #{note}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insertReturningId(HealthRecordEntity row);

    @Update("""
        UPDATE health_records
        SET record_date = #{recordDate}, weight_kg = #{weightKg}, meal_memo = #{mealMemo},
            exercise_minutes = #{exerciseMinutes}, meal_score = #{mealScore}, exercise_score = #{exerciseScore},
            sleep_score = #{sleepScore}, mood_score = #{moodScore}, overall_score = #{overallScore}, image_path = #{imagePath},
            note = #{note}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int update(HealthRecordEntity row);

    @Update("""
        UPDATE health_records SET deleted_at = #{deletedAt}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
