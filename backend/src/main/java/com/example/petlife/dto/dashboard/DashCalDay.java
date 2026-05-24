package com.example.petlife.dto.dashboard;

public record DashCalDay(
        int dayOfMonth,
        boolean inMonth,
        boolean isToday,
        boolean hasConfirmedAppt
) {}
