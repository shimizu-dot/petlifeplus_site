package com.example.petlife.mapper;

import com.example.petlife.entity.SymptomCheckEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SymptomCheckMapper {

    @Select("""
        INSERT INTO symptom_checks(
            pet_id, requested_by_user_id, symptom_type, onset_text, memo,
            severity, recommendation, guidance, ai_model, created_at
        )
        VALUES(
            #{petId}, #{requestedByUserId}, #{symptomType}, #{onsetText}, #{memo},
            #{severity}, #{recommendation}, #{guidance}, #{aiModel}, CURRENT_TIMESTAMP
        )
        RETURNING id
        """)
    Long insertReturningId(SymptomCheckEntity row);

    @Select("""
        SELECT id, pet_id, requested_by_user_id, symptom_type, onset_text, memo,
               severity, recommendation, guidance, ai_model, created_at
        FROM symptom_checks
        WHERE pet_id = #{petId}
          AND deleted_at IS NULL
        ORDER BY id DESC
        LIMIT #{limit}
        """)
    List<SymptomCheckEntity> findRecentByPetId(@Param("petId") Long petId, @Param("limit") int limit);
}
