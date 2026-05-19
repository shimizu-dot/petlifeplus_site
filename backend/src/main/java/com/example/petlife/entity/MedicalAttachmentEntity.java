package com.example.petlife.entity;

import java.time.LocalDateTime;

public record MedicalAttachmentEntity(
        Long id,
        Long medicalHistoryId,
        String fileName,
        String filePath,
        String fileMimeType,
        Long fileSizeBytes,
        String description,
        LocalDateTime uploadedAt,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
