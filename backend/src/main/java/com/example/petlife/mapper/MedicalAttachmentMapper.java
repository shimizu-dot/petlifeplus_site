package com.example.petlife.mapper;

import com.example.petlife.entity.MedicalAttachmentEntity;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MedicalAttachmentMapper {

    @Select("""
        SELECT id, medical_history_id, file_name, file_path, file_mime_type,
               file_size_bytes, description, uploaded_at, deleted_at, created_at, updated_at
        FROM medical_attachments
        WHERE medical_history_id = #{historyId} AND deleted_at IS NULL
        ORDER BY uploaded_at DESC
        """)
    List<MedicalAttachmentEntity> findByHistoryId(@Param("historyId") Long historyId);

    @Select("""
        SELECT id, medical_history_id, file_name, file_path, file_mime_type,
               file_size_bytes, description, uploaded_at, deleted_at, created_at, updated_at
        FROM medical_attachments WHERE id = #{id} AND deleted_at IS NULL
        """)
    MedicalAttachmentEntity findById(@Param("id") Long id);

    @Select("""
        INSERT INTO medical_attachments(medical_history_id, file_name, file_path,
            file_mime_type, file_size_bytes, description, uploaded_at, created_at, updated_at)
        VALUES(#{medicalHistoryId}, #{fileName}, #{filePath},
            #{fileMimeType}, #{fileSizeBytes}, #{description}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insertReturningId(MedicalAttachmentEntity attachment);

    @Update("""
        UPDATE medical_attachments
        SET deleted_at = #{deletedAt}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
