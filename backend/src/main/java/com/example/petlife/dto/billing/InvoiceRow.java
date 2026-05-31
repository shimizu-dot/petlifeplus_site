package com.example.petlife.dto.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceRow(
        Long invoiceId,
        String invoiceNumber,
        LocalDate invoiceDate,
        LocalDate dueDate,
        BigDecimal amount,
        String paymentStatus,
        LocalDateTime issuedAt,
        LocalDateTime paidAt,
        Long subscriptionId,
        String planName,
        BigDecimal monthlyFee,
        Long ownerUserId,
        String ownerName,
        String ownerEmail,
        String lineUserId,
        LocalDate subscriptionEndDate
) {}
