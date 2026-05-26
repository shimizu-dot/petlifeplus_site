package com.example.petlife.dto.report;

public record ReportStats(
        long userCount,
        long userCountAdmin,
        long userCountSuper,
        long userCountUser,
        long userCountVet,
        long userCountStaff,
        long petCount,
        long healthRecordCount,
        long appointmentCount,
        long appointmentRequested,
        long appointmentConfirmed,
        long appointmentCompleted,
        long appointmentCanceled,
        long subscriptionCount
) {}
