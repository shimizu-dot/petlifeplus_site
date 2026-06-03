package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.appointment.AppointmentCreateRequest;
import com.example.petlife.dto.appointment.AppointmentResponse;
import com.example.petlife.dto.appointment.AppointmentUpdateRequest;
import com.example.petlife.dto.common.PageResponse;
import com.example.petlife.exception.ForbiddenException;
import com.example.petlife.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /** 一覧: 管理者は全件、一般ユーザーは自分の予約のみ */
    @GetMapping
    public ResponseEntity<PageResponse<AppointmentResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal LoginUser currentUser) {
        return ResponseEntity.ok(appointmentService.list(page, size, currentUser));
    }

    /** 詳細: 自分の予約、または VET/STAFF/ADMIN のみアクセス可 */
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser currentUser) {
        AppointmentResponse resp = appointmentService.get(id);
        if (!currentUser.hasStaffAccess() && !resp.ownerUserId().equals(currentUser.id())) {
            throw new ForbiddenException("この予約にアクセスする権限がありません");
        }
        return ResponseEntity.ok(resp);
    }

    /**
     * 新規作成: ownerUserId はログインユーザーから設定。
     * VET/STAFF/ADMIN はリクエスト本文の ownerUserId をそのまま使用可。
     */
    @PostMapping
    public ResponseEntity<AppointmentResponse> create(
            @Valid @RequestBody AppointmentCreateRequest request,
            @AuthenticationPrincipal LoginUser currentUser) {
        // status は常に REQUESTED で開始（スタッフも含め外部から任意のステータスを注入させない）
        AppointmentCreateRequest safeRequest = currentUser.hasStaffAccess()
                ? new AppointmentCreateRequest(
                        request.petId(), request.ownerUserId(), request.staffUserId(),
                        request.appointmentType(), request.channel(),
                        request.scheduledAt(), "REQUESTED", request.note())
                : new AppointmentCreateRequest(
                        request.petId(), currentUser.id(), null,
                        request.appointmentType(), request.channel(),
                        request.scheduledAt(), "REQUESTED", request.note());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.create(safeRequest, currentUser));
    }

    /** 更新: 自分の予約、または VET/STAFF/ADMIN のみ */
    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentUpdateRequest request,
            @AuthenticationPrincipal LoginUser currentUser) {
        AppointmentResponse existing = appointmentService.get(id);
        if (!currentUser.hasStaffAccess() && !existing.ownerUserId().equals(currentUser.id())) {
            throw new ForbiddenException("この予約を更新する権限がありません");
        }
        return ResponseEntity.ok(appointmentService.update(id, request));
    }

    /** 削除: 申請者本人の6か月超過予約、または ADMIN/SUPER のみ */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser currentUser) {
        appointmentService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
