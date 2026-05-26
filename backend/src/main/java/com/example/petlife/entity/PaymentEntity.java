package com.example.petlife.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentEntity(
        Long id,
        Long invoiceId,
        BigDecimal paidAmount,
        LocalDateTime paidAt,
        String paymentMethod,
        String transactionRef,
        String status,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
