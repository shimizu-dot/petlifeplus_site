package com.example.petlife.mapper;

import com.example.petlife.entity.HealthRecordPetDateEntity;
import com.example.petlife.entity.PetCareRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PetCareRecordMapper {

    // INSERT...RETURNING は結果セットを返すため @Select を使用（@Insert では Long 戻り値に写像されない）
    @Select("""
        INSERT INTO pet_care_records(
            pet_id, recorded_by_user_id, care_type, administered_on, next_due_on, memo,
            created_at, updated_at
        )
        VALUES(
            #{petId}, #{recordedByUserId}, #{careType}, #{administeredOn}, #{nextDueOn}, #{memo},
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        RETURNING id
        """)
    Long insertReturningId(PetCareRecordEntity row);

    @Select("""
        SELECT id, pet_id, recorded_by_user_id, care_type, administered_on, next_due_on, memo,
               deleted_at, created_at, updated_at
        FROM pet_care_records
        WHERE pet_id = #{petId} AND deleted_at IS NULL
        ORDER BY administered_on DESC, id DESC
        """)
    List<PetCareRecordEntity> findByPetId(@Param("petId") Long petId);

    @Select("""
        SELECT id, pet_id, recorded_by_user_id, care_type, administered_on, next_due_on, memo,
               deleted_at, created_at, updated_at
        FROM pet_care_records
        WHERE pet_id = #{petId}
          AND deleted_at IS NULL
          AND next_due_on >= #{fromDate}
          AND next_due_on <= #{toDate}
        ORDER BY next_due_on ASC, id ASC
        """)
    List<PetCareRecordEntity> findUpcomingByPetId(
            @Param("petId") Long petId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Select("""
        SELECT DISTINCT c.pet_id AS "petId", c.administered_on AS "recordDate"
        FROM pet_care_records c
        JOIN pets p ON p.id = c.pet_id
        WHERE p.owner_user_id = #{ownerUserId}
          AND p.deleted_at IS NULL
          AND c.deleted_at IS NULL
          AND c.care_type IN ('RABIES','HEARTWORM','COMBO_VACCINE')
          AND c.administered_on >= #{fromDate}
          AND c.administered_on <= #{toDate}
        ORDER BY c.administered_on ASC
        """)
    List<HealthRecordPetDateEntity> findVaccinePetDatesByOwnerAndDateRange(
            @Param("ownerUserId") Long ownerUserId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
