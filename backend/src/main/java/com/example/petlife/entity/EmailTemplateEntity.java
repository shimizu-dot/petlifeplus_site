package com.example.petlife.entity;

import java.time.LocalDateTime;

public record EmailTemplateEntity(
        Long id,
        String templateCode,
        String subjectTemplate,
        String bodyTemplate,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
