package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.appointment.AppointmentCreateRequest;
import com.example.petlife.dto.appointment.AppointmentUpdateRequest;
import com.example.petlife.entity.AppointmentBusinessHoursEntity;
import com.example.petlife.entity.AppointmentEntity;
import com.example.petlife.entity.AppointmentSlotEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.exception.ForbiddenException;
import com.example.petlife.mapper.AppointmentMapper;
import com.example.petlife.mapper.AppointmentSlotMapper;
import com.example.petlife.mapper.NotificationMapper;
import com.example.petlife.mapper.PetMapper;
import com.example.petlife.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock AppointmentMapper appointmentMapper;
    @Mock AppointmentSlotMapper appointmentSlotMapper;
    @Mock PlanAccessService planAccessService;
    @Mock AppointmentBusinessHoursService businessHoursService;
    @Mock ZoomLinkService zoomLinkService;
    @Mock PetMapper petMapper;
    @Mock NotificationMapper notificationMapper;
    @Mock UserMapper userMapper;

    @InjectMocks AppointmentService svc;

    // VET スタッフはプランチェック・所有権チェックをスキップするため
    // validateBusinessHours に直接到達できる
    private static final LoginUser VET = new LoginUser(10L, 4L, "獣医", "vet@petlife.local", "hash", true);
    private static final LoginUser ADMIN = new LoginUser(1L, 1L, "管理者", "admin@petlife.local", "hash", true);
    private static final LoginUser SUPER = new LoginUser(2L, 2L, "SUPER", "super@petlife.local", "hash", true);
    private static final LoginUser OWNER = new LoginUser(3L, 3L, "申請者", "owner@petlife.local", "hash", true);

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(businessHoursService.getCurrent()).thenReturn(defaultBusinessHours());
        org.mockito.Mockito.lenient().when(petMapper.findById(1L)).thenReturn(defaultPet());
    }

    // ── validateBusinessHours ────────────────────────────────────────────────

    @Test
    void shouldRejectTimeBeforeOpen() {
        LocalDateTime earlyMorning = tomorrow().withHour(8).withMinute(0);
        assertThrows(BadRequestException.class,
                () -> svc.create(buildCreateReq(earlyMorning), VET));
    }

    @Test
    void shouldRejectTimeAfterClose() {
        LocalDateTime afterClose = tomorrow().withHour(17).withMinute(1);
        assertThrows(BadRequestException.class,
                () -> svc.create(buildCreateReq(afterClose), VET));
    }

    @Test
    void shouldAcceptTimeAtOpenBoundary() {
        // 9:30 は境界値として許可される
        LocalDateTime atOpen = tomorrow().withHour(9).withMinute(30);
        // validateBusinessHours 通過後、insert が null を返す → get() で NotFoundException
        // ここでは BadRequestException が投げられないことだけを確認
        when(appointmentMapper.insert(any())).thenReturn(null);
        assertThrows(Exception.class, () -> svc.create(buildCreateReq(atOpen), VET));
        // BadRequestException でないことを確認
        try {
            svc.create(buildCreateReq(atOpen), VET);
        } catch (BadRequestException e) {
            fail("9:30 は営業時間内として許可されるべき: " + e.getMessage());
        } catch (Exception ignored) {
            // NotFoundException 等は想定内
        }
    }

    @Test
    void shouldAcceptTimeAtCloseBoundary() {
        // 17:00 ちょうどは isAfter(17:00) = false なので許可される
        LocalDateTime atClose = tomorrow().withHour(17).withMinute(0);
        when(appointmentMapper.insert(any())).thenReturn(null);
        try {
            svc.create(buildCreateReq(atClose), VET);
        } catch (BadRequestException e) {
            fail("17:00 は営業時間内として許可されるべき: " + e.getMessage());
        } catch (Exception ignored) {
            // NotFoundException 等は想定内
        }
    }

    // ── validateStatusTransition ─────────────────────────────────────────────

    @Test
    void shouldRejectCanceledToRequested() {
        when(appointmentMapper.findById(1L)).thenReturn(existingWith("CANCELED"));
        AppointmentUpdateRequest req = buildUpdateReq("REQUESTED");
        assertThrows(BadRequestException.class, () -> svc.update(1L, req));
    }

    @Test
    void shouldRejectRequestedToConfirmedViaUpdate() {
        // 承認は approve() 専用エンドポイントで行うべき
        when(appointmentMapper.findById(1L)).thenReturn(existingWith("REQUESTED"));
        AppointmentUpdateRequest req = buildUpdateReq("CONFIRMED");
        assertThrows(BadRequestException.class, () -> svc.update(1L, req));
    }

    @Test
    void shouldAllowConfirmedToCompleted() {
        when(appointmentMapper.findById(1L)).thenReturn(existingWith("CONFIRMED"));
        AppointmentUpdateRequest req = buildUpdateReq("COMPLETED");
        // validateStatusTransition 通過後、update → get(id) と進む
        // findById は2回呼ばれる（update内の get()）のでスタブは使い回される
        assertDoesNotThrow(() -> svc.update(1L, req));
    }

    @Test
    void shouldAllowSameStatusUpdate() {
        // 同一ステータスへの更新は常に許可
        when(appointmentMapper.findById(1L)).thenReturn(existingWith("REQUESTED"));
        AppointmentUpdateRequest req = buildUpdateReq("REQUESTED");
        assertDoesNotThrow(() -> svc.update(1L, req));
    }

    // ── generateAvailableSlots ───────────────────────────────────────────────

    @Test
    void shouldGenerate15BaseSlotsForFutureDate() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(appointmentMapper.findBookedTimesOnDate(tomorrow)).thenReturn(List.of());
        when(appointmentSlotMapper.findBlockedOnDate(tomorrow)).thenReturn(List.of());
        when(appointmentSlotMapper.findExtraOnDate(tomorrow)).thenReturn(List.of());

        List<AppointmentService.SlotInfo> slots = svc.generateAvailableSlots(tomorrow);

        // 9:30〜16:30 を 30 分刻み → 15 スロット（16:30 + 30min = 17:00 = BUSINESS_END で停止）
        assertEquals(15, slots.size());
        assertTrue(slots.stream().allMatch(AppointmentService.SlotInfo::available));
    }

    @Test
    void shouldMarkBookedSlotAsUnavailable() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime booked1000 = LocalDateTime.of(tomorrow, LocalTime.of(10, 0));

        when(appointmentMapper.findBookedTimesOnDate(tomorrow)).thenReturn(List.of(booked1000));
        when(appointmentSlotMapper.findBlockedOnDate(tomorrow)).thenReturn(List.of());
        when(appointmentSlotMapper.findExtraOnDate(tomorrow)).thenReturn(List.of());

        List<AppointmentService.SlotInfo> slots = svc.generateAvailableSlots(tomorrow);

        Optional<AppointmentService.SlotInfo> slot1000 = slots.stream()
                .filter(s -> s.slotTime().toLocalTime().equals(LocalTime.of(10, 0)))
                .findFirst();
        assertTrue(slot1000.isPresent());
        assertFalse(slot1000.get().available(), "予約済みスロットは available=false であること");
    }

    @Test
    void shouldExcludeBlockedSlot() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime blocked1100 = LocalDateTime.of(tomorrow, LocalTime.of(11, 0));
        AppointmentSlotEntity blockedEntity = new AppointmentSlotEntity(
                1L, blocked1100, null, true, null, null, null);

        when(appointmentMapper.findBookedTimesOnDate(tomorrow)).thenReturn(List.of());
        when(appointmentSlotMapper.findBlockedOnDate(tomorrow)).thenReturn(List.of(blockedEntity));
        when(appointmentSlotMapper.findExtraOnDate(tomorrow)).thenReturn(List.of());

        List<AppointmentService.SlotInfo> slots = svc.generateAvailableSlots(tomorrow);

        // ブロック枠は結果に含まれない
        boolean has1100 = slots.stream()
                .anyMatch(s -> s.slotTime().toLocalTime().equals(LocalTime.of(11, 0)));
        assertFalse(has1100, "ブロック枠は生成スロットから除外されること");
        assertEquals(14, slots.size());
    }

    @Test
    void shouldIncludeExtraSlot() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        // 通常生成されない 8:00 の追加枠
        LocalDateTime extra0800 = LocalDateTime.of(tomorrow, LocalTime.of(8, 0));
        AppointmentSlotEntity extraEntity = new AppointmentSlotEntity(
                2L, extra0800, null, false, null, null, null);

        when(appointmentMapper.findBookedTimesOnDate(tomorrow)).thenReturn(List.of());
        when(appointmentSlotMapper.findBlockedOnDate(tomorrow)).thenReturn(List.of());
        when(appointmentSlotMapper.findExtraOnDate(tomorrow)).thenReturn(List.of(extraEntity));

        List<AppointmentService.SlotInfo> slots = svc.generateAvailableSlots(tomorrow);

        boolean has0800 = slots.stream()
                .anyMatch(s -> s.slotTime().toLocalTime().equals(LocalTime.of(8, 0)));
        assertTrue(has0800, "追加枠は生成スロットに含まれること");
        assertEquals(16, slots.size());
    }

    // ── delete rules ─────────────────────────────────────────────────────────

    @Test
    void ownerShouldNotDeleteRequestedAppointment() {
        // オーナー自身の予約（ownerUserId=OWNER.id）が対象 — 所有権チェック通過後に status チェックへ
        when(appointmentMapper.findById(1L)).thenReturn(ownedBy(OWNER, "REQUESTED"));
        assertThrows(BadRequestException.class, () -> svc.delete(1L, OWNER));
    }

    @Test
    void ownerShouldNotDeleteRecentConfirmedAppointment() {
        // 直近の CONFIRMED 予約はオーナー自身でも削除不可
        when(appointmentMapper.findById(1L)).thenReturn(ownedBy(OWNER, "CONFIRMED"));
        assertThrows(BadRequestException.class, () -> svc.delete(1L, OWNER));
    }

    @Test
    void ownerShouldDeleteAppointmentOlderThanSixMonths() {
        AppointmentEntity oldConfirmed = new AppointmentEntity(
                1L, 1L, OWNER.id(), null, "MEDICAL", "VISIT",
                LocalDateTime.now().minusMonths(7), "CONFIRMED",
                null, null, null, null, null, null);
        when(appointmentMapper.findById(1L)).thenReturn(oldConfirmed);
        when(appointmentMapper.softDelete(anyLong(), any())).thenReturn(1);

        assertDoesNotThrow(() -> svc.delete(1L, OWNER));
        verify(appointmentMapper).softDelete(anyLong(), any());
    }

    @Test
    void vetShouldNotDeleteConfirmedAppointment() {
        when(appointmentMapper.findById(1L)).thenReturn(existingWith("CONFIRMED"));
        assertThrows(ForbiddenException.class, () -> svc.delete(1L, VET));
    }

    @Test
    void adminShouldDeleteUpcomingConfirmedAppointment() {
        when(appointmentMapper.findById(1L)).thenReturn(existingWith("CONFIRMED"));
        when(appointmentMapper.softDelete(anyLong(), any())).thenReturn(1);

        assertDoesNotThrow(() -> svc.delete(1L, ADMIN));
        verify(appointmentMapper).softDelete(anyLong(), any());
    }

    @Test
    void staffShouldNotCreateOnlineAppointmentForLightPlanPet() {
        LocalDateTime scheduledAt = LocalDate.now().plusDays(1).atTime(10, 0);
        when(petMapper.findById(1L)).thenReturn(new com.example.petlife.entity.PetEntity(
                1L, OWNER.id(), "ポチ", "DOG", "Shiba", "MALE", null, null, null,
                null, null, null, null));
        when(planAccessService.resolvePlanTierByUserId(OWNER.id())).thenReturn(PlanAccessService.PlanTier.LIGHT);
        when(planAccessService.planLabelByUserId(OWNER.id())).thenReturn("ライト");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> svc.createGeneralCare(1L, scheduledAt, "オンライン希望", "ONLINE", VET));

        assertEquals("このペットはライト会員のため、この予約はできません。", ex.getMessage());
    }

    @Test
    void superShouldBeAllowedToCreateGeneralCareAppointment() {
        LocalDateTime scheduledAt = LocalDate.now().plusDays(1).atTime(10, 0);
        when(appointmentMapper.countByScheduledAt(scheduledAt)).thenReturn(0);
        when(petMapper.findById(1L)).thenReturn(new com.example.petlife.entity.PetEntity(
                1L, OWNER.id(), "ポチ", "DOG", "Shiba", "MALE", null, null, null,
                null, null, null, null));
        when(appointmentMapper.insert(any())).thenReturn(null);

        assertDoesNotThrow(() -> {
            try {
                svc.createGeneralCare(1L, scheduledAt, "通常予約", "VISIT", SUPER);
            } catch (com.example.petlife.exception.NotFoundException ignored) {
                // insert() 後の get(null) による到達は許容し、権限制御だけ確認する
            }
        });
    }

    @Test
    void userRequestedAppointmentShouldNotifyVetAndStaff() {
        LocalDateTime scheduledAt = LocalDate.now().plusDays(1).atTime(10, 0);
        AppointmentEntity inserted = new AppointmentEntity(
                10L, 1L, OWNER.id(), null, "MEDICAL", "VISIT",
                scheduledAt, "REQUESTED", null, "初診希望", null, null, null, null);
        when(planAccessService.canUseAppointments(OWNER)).thenReturn(true);
        when(appointmentMapper.countByScheduledAt(scheduledAt)).thenReturn(0);
        when(petMapper.findByIdAndOwnerUserId(1L, OWNER.id())).thenReturn(new com.example.petlife.entity.PetEntity(
                1L, OWNER.id(), "ポチ", "DOG", "Shiba", "MALE", null, null, null,
                null, null, null, null));
        when(appointmentMapper.insert(any())).thenReturn(10L);
        when(appointmentMapper.findById(10L)).thenReturn(inserted);
        when(userMapper.findActiveUserIdsByRoleCode("VET")).thenReturn(List.of(11L));
        when(userMapper.findActiveUserIdsByRoleCode("STAFF")).thenReturn(List.of(12L, 13L));
        when(notificationMapper.insertReturningId(any())).thenReturn(200L);

        assertDoesNotThrow(() -> svc.createGeneralCare(1L, scheduledAt, "初診希望", "VISIT", OWNER));

        verify(notificationMapper).insertRecipient(200L, 11L);
        verify(notificationMapper).insertRecipient(200L, 12L);
        verify(notificationMapper).insertRecipient(200L, 13L);
        verify(notificationMapper).updateRecipientStatus(200L, 11L, "SENT");
        verify(notificationMapper).updateRecipientStatus(200L, 12L, "SENT");
        verify(notificationMapper).updateRecipientStatus(200L, 13L, "SENT");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static LocalDateTime tomorrow() {
        return LocalDate.now().plusDays(1).atStartOfDay();
    }

    private static AppointmentCreateRequest buildCreateReq(LocalDateTime scheduledAt) {
        return new AppointmentCreateRequest(1L, 1L, null, "MEDICAL", "VISIT", scheduledAt, "REQUESTED", null);
    }

    private static AppointmentUpdateRequest buildUpdateReq(String status) {
        return new AppointmentUpdateRequest(null, "MEDICAL", "VISIT",
                LocalDate.now().plusDays(1).atTime(10, 0), status, null);
    }

    private static AppointmentEntity existingWith(String status) {
        return new AppointmentEntity(1L, 1L, 1L, null, "MEDICAL", "VISIT",
                LocalDate.now().plusDays(1).atTime(10, 0), status,
                null, null, null, null, null, null);
    }

    /** 指定ユーザーが所有者の予約エンティティを生成する。所有権チェックが通過するテスト向け。 */
    private static AppointmentEntity ownedBy(LoginUser owner, String status) {
        return new AppointmentEntity(1L, 1L, owner.id(), null, "MEDICAL", "VISIT",
                LocalDate.now().plusDays(1).atTime(10, 0), status,
                null, null, null, null, null, null);
    }

    private static AppointmentBusinessHoursEntity defaultBusinessHours() {
        return new AppointmentBusinessHoursEntity(
                1L,
                LocalTime.of(9, 30),
                LocalTime.of(17, 0),
                30,
                1L,
                null,
                null
        );
    }

    private static com.example.petlife.entity.PetEntity defaultPet() {
        return new com.example.petlife.entity.PetEntity(
                1L, OWNER.id(), "ポチ", "DOG", "Shiba", "MALE", null, null, null,
                null, null, null, null
        );
    }
}
