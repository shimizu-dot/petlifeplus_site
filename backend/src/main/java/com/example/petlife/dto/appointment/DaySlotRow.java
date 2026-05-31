package com.example.petlife.dto.appointment;

import java.time.LocalDateTime;

/**
 * 予約枠管理画面の1行 — 自動生成スロットと appointment_slots の状態を統合したビューモデル
 *
 * status 値:
 *   AUTO_AVAILABLE  — 自動生成（9:30-17:00）、予約可能
 *   AUTO_REQUESTED  — 自動生成、申請中（未承認）
 *   AUTO_BOOKED     — 自動生成、予約済み（操作不可）
 *   AUTO_BLOCKED    — 自動生成→ブロック枠で無効化済み
 *   EXTRA_AVAILABLE — 追加枠（appointment_slots.is_blocked=false）、予約可能
 *   EXTRA_REQUESTED — 追加枠、申請中（未承認）
 *   EXTRA_BOOKED    — 追加枠、予約済み（操作不可）
 */
public record DaySlotRow(
        LocalDateTime slotTime,
        String status,
        Long registeredSlotId,   // appointment_slots.id (ブロック枠・追加枠の場合)
        String note              // appointment_slots.note
) {}
