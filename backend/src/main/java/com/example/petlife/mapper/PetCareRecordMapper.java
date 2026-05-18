package com.example.petlife.mapper;

import com.example.petlife.entity.PetCareRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PetCareRecordMapper {

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
}
