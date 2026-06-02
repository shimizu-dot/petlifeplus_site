package com.example.petlife.mapper;

import com.example.petlife.dto.consultation.MedicalHistoryRow;
import com.example.petlife.entity.HealthRecordPetDateEntity;
import com.example.petlife.entity.MedicalHistoryEntity;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MedicalHistoryMapper {

    @Select("""
        SELECT m.id, m.pet_id AS "petId", p.name AS "petName",
               u.name AS "handlerName",
               m.performed_on AS "performedOn",
               m.treatment_detail AS "treatmentDetail",
               m.diagnosis, m.prescription, m.created_at AS "createdAt"
        FROM medical_histories m
        JOIN pets p ON p.id = m.pet_id
        JOIN users u ON u.id = m.handled_by_user_id
        WHERE m.deleted_at IS NULL
        ORDER BY m.performed_on DESC, m.id DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<MedicalHistoryRow> findAllRows(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM medical_histories WHERE deleted_at IS NULL")
    long countAll();

    @Select("""
        SELECT m.id, m.pet_id AS "petId", p.name AS "petName",
               u.name AS "handlerName",
               m.performed_on AS "performedOn",
               m.treatment_detail AS "treatmentDetail",
               m.diagnosis, m.prescription, m.created_at AS "createdAt"
        FROM medical_histories m
        JOIN pets p ON p.id = m.pet_id
        JOIN users u ON u.id = m.handled_by_user_id
        WHERE m.deleted_at IS NULL AND m.pet_id = #{petId}
        ORDER BY m.performed_on DESC, m.id DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<MedicalHistoryRow> findRowsByPetId(@Param("petId") Long petId,
                                             @Param("limit") int limit,
                                             @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM medical_histories WHERE deleted_at IS NULL AND pet_id = #{petId}")
    long countByPetId(@Param("petId") Long petId);

    @Select("""
        SELECT id, pet_id, appointment_id, handled_by_user_id, performed_on,
               treatment_detail, diagnosis, prescription, deleted_at, created_at, updated_at
        FROM medical_histories
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    MedicalHistoryEntity findById(@Param("id") Long id);

    // INSERT...RETURNING は結果セットを返すため @Select を使用（@Insert では Long 戻り値に写像されない）
    @Select("""
        INSERT INTO medical_histories(pet_id, appointment_id, handled_by_user_id, performed_on,
            treatment_detail, diagnosis, prescription, created_at, updated_at)
        VALUES(#{petId}, #{appointmentId}, #{handledByUserId}, #{performedOn},
            #{treatmentDetail}, #{diagnosis}, #{prescription}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insertReturningId(MedicalHistoryEntity row);

    @Update("""
        UPDATE medical_histories
        SET pet_id = #{petId}, appointment_id = #{appointmentId},
            performed_on = #{performedOn},
            treatment_detail = #{treatmentDetail},
            diagnosis = #{diagnosis}, prescription = #{prescription},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int update(MedicalHistoryEntity row);

    @Update("""
        UPDATE medical_histories
        SET deleted_at = #{deletedAt}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    @Select("""
        SELECT DISTINCT m.pet_id AS "petId", m.performed_on AS "recordDate"
        FROM medical_histories m
        JOIN pets p ON p.id = m.pet_id
        WHERE p.owner_user_id = #{ownerUserId}
          AND p.deleted_at IS NULL
          AND m.deleted_at IS NULL
          AND m.performed_on >= #{fromDate}
          AND m.performed_on <= #{toDate}
        ORDER BY m.performed_on ASC
        """)
    List<HealthRecordPetDateEntity> findMedicalHistoryPetDatesByOwnerAndDateRange(
            @Param("ownerUserId") Long ownerUserId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
