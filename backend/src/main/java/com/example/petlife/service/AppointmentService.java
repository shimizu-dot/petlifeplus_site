package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.appointment.AppointmentCreateRequest;
import com.example.petlife.dto.appointment.AppointmentListRow;
import com.example.petlife.dto.appointment.AppointmentResponse;
import com.example.petlife.dto.appointment.AppointmentUpdateRequest;
import com.example.petlife.dto.common.PageResponse;
import com.example.petlife.entity.AppointmentEntity;
import com.example.petlife.entity.NotificationEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.exception.ForbiddenException;
import com.example.petlife.exception.NotFoundException;
import com.example.petlife.mapper.AppointmentMapper;
import com.example.petlife.mapper.AppointmentSlotMapper;
import com.example.petlife.mapper.NotificationMapper;
import com.example.petlife.mapper.PetMapper;
import com.example.petlife.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AppointmentService {
    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);
    private static final LocalTime SLOT_OPEN  = LocalTime.of(9, 30);
    private static final LocalTime SLOT_CLOSE = LocalTime.of(17, 0);

    private final AppointmentMapper appointmentMapper;
    private final AppointmentSlotMapper appointmentSlotMapper;
    private final PlanAccessService planAccessService;
    private final ZoomLinkService zoomLinkService;
    private final PetMapper petMapper;
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    public AppointmentService(AppointmentMapper appointmentMapper,
                              AppointmentSlotMapper appointmentSlotMapper,
                              PlanAccessService planAccessService,
                              ZoomLinkService zoomLinkService,
                              PetMapper petMapper,
                              NotificationMapper notificationMapper,
                              UserMapper userMapper) {
        this.appointmentMapper = appointmentMapper;
        this.appointmentSlotMapper = appointmentSlotMapper;
        this.planAccessService = planAccessService;
        this.zoomLinkService = zoomLinkService;
        this.petMapper = petMapper;
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
    }

    public PageResponse<AppointmentResponse> list(int page, int size, LoginUser currentUser) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        if (currentUser.canManageClinical()) {
            return new PageResponse<>(
                    appointmentMapper.findAll(safeSize, offset).stream().map(this::toResponse).toList(),
                    safePage, safeSize, appointmentMapper.countAll());
        }
        return new PageResponse<>(
                appointmentMapper.findByOwnerUserId(currentUser.id(), safeSize, offset).stream().map(this::toResponse).toList(),
                safePage, safeSize, appointmentMapper.countByOwnerUserId(currentUser.id()));
    }

    public AppointmentResponse get(Long id) {
        AppointmentEntity row = appointmentMapper.findById(id);
        if (row == null) throw new NotFoundException("Appointment not found: " + id);
        return toResponse(row);
    }

    public AppointmentResponse create(AppointmentCreateRequest req, LoginUser currentUser) {
        // スタッフ以外はプランチェック（LIGHT プランは予約不可）
        if (!currentUser.hasStaffAccess() && !planAccessService.canUseAppointments(currentUser)) {
            throw new BadRequestException("この機能はスタンダード以上で利用できます");
        }
        // スタッフ以外は petId が自分所有かを確認（他ユーザーのペットIDを指定させない）
        if (!currentUser.hasStaffAccess()) {
            var pet = petMapper.findByIdAndOwnerUserId(req.petId(), currentUser.id());
            if (pet == null) {
                throw new ForbiddenException("指定されたペットへのアクセス権がありません");
            }
        }
        validateBusinessHours(req.scheduledAt());
        ensureNoDuplicate(req.staffUserId(), req.scheduledAt(), null);
        AppointmentEntity row = new AppointmentEntity(null, req.petId(), req.ownerUserId(), req.staffUserId(),
                req.appointmentType(), req.channel(), req.scheduledAt(), req.status(), null, req.note(),
                null, null, null, null);
        Long createdId = appointmentMapper.insert(row);
        return get(createdId);
    }

    public PageResponse<AppointmentListRow> listForApp(int page, int size, LoginUser currentUser, String sortBy) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        boolean sortByPet = "pet".equalsIgnoreCase(sortBy);
        if (currentUser.canManageClinical()) {
            List<AppointmentListRow> items = sortByPet
                    ? appointmentMapper.findAllRowsOrderByPet(safeSize, offset)
                    : appointmentMapper.findAllRows(safeSize, offset);
            return new PageResponse<>(items, safePage, safeSize, appointmentMapper.countAll());
        }
        List<AppointmentListRow> items = sortByPet
                ? appointmentMapper.findRowsByOwnerUserIdOrderByPet(currentUser.id(), safeSize, offset)
                : appointmentMapper.findRowsByOwnerUserId(currentUser.id(), safeSize, offset);
        return new PageResponse<>(items, safePage, safeSize, appointmentMapper.countByOwnerUserId(currentUser.id()));
    }

    public AppointmentResponse update(Long id, AppointmentUpdateRequest req) {
        AppointmentEntity existing = appointmentMapper.findById(id);
        if (existing == null) throw new NotFoundException("Appointment not found: " + id);
        validateStatusTransition(existing.status(), req.status());
        ensureNoDuplicate(req.staffUserId(), req.scheduledAt(), id);
        AppointmentEntity row = new AppointmentEntity(id, existing.petId(), existing.ownerUserId(), req.staffUserId(),
                req.appointmentType(), req.channel(), req.scheduledAt(), req.status(), existing.zoomJoinUrl(),
                req.note(), existing.slotId(), existing.deletedAt(), existing.createdAt(), existing.updatedAt());
        appointmentMapper.update(row);
        return get(id);
    }

    public PremiumOnlineCareResult createPremiumOnlineCare(Long petId,
                                                            LocalDateTime scheduledAt,
                                                            String note,
                                                            LoginUser currentUser) {
        if (!planAccessService.canUsePrioritySupport(currentUser)) {
            throw new BadRequestException("この機能はプレミアムプランで利用できます");
        }
        if (scheduledAt == null || !scheduledAt.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("予約日時は未来日時を指定してください");
        }
        LocalTime t = scheduledAt.toLocalTime();
        if (t.isBefore(BUSINESS_START) || t.isAfter(BUSINESS_END.minusMinutes(1))) {
            throw new BadRequestException("診療受付時間外です（9:30〜17:00）");
        }
        if (appointmentMapper.countByScheduledAt(scheduledAt) > 0) {
            throw new BadRequestException("その時間帯はすでに予約済みです");
        }
        var pet = petMapper.findByIdAndOwnerUserId(petId, currentUser.id());
        if (pet == null) throw new NotFoundException("Pet not found: " + petId);
        if (pet.deceasedAt() != null) throw new BadRequestException("永眠登録済みのペットはこの操作を利用できません");

        AppointmentEntity row = new AppointmentEntity(
                null, petId, currentUser.id(), null, "MEDICAL", "ONLINE",
                scheduledAt, "REQUESTED", null, note,
                null, null, null, null);
        Long createdId = appointmentMapper.insert(row);
        AppointmentResponse created = get(createdId);
        return new PremiumOnlineCareResult(created, false, null);
    }

    private static final LocalTime BUSINESS_START = LocalTime.of(9, 30);
    private static final LocalTime BUSINESS_END   = LocalTime.of(17, 0);
    private static final int SLOT_MINUTES = 30;

    /**
     * 指定日の予約可能スロット一覧を返す。
     *
     * 算出ロジック:
     *   1. 9:30〜17:00 を 30 分刻みで自動生成（ベース）
     *   2. appointment_slots(is_blocked=true) に一致する時刻をベースから除外
     *   3. appointment_slots(is_blocked=false) の追加枠をマージ（重複は1つに集約）
     *   4. すでに予約済みの時刻は available=false としてマーク（表示はするが選択不可）
     *   5. 過去の時刻は結果から除外
     */
    public List<SlotInfo> generateAvailableSlots(LocalDate date) {
        Set<LocalDateTime> booked  = new HashSet<>(appointmentMapper.findBookedTimesOnDate(date));
        Set<LocalDateTime> blocked = appointmentSlotMapper.findBlockedOnDate(date).stream()
                .map(s -> s.slotDatetime()).collect(java.util.stream.Collectors.toSet());
        Set<LocalDateTime> extra   = appointmentSlotMapper.findExtraOnDate(date).stream()
                .map(s -> s.slotDatetime()).collect(java.util.stream.Collectors.toSet());

        // ベース自動生成 - ブロック枠
        Set<LocalDateTime> baseTimes = new java.util.LinkedHashSet<>();
        LocalTime t = BUSINESS_START;
        while (!t.isAfter(BUSINESS_END.minusMinutes(1))) {
            LocalDateTime dt = LocalDateTime.of(date, t);
            if (!blocked.contains(dt)) {
                baseTimes.add(dt);
            }
            t = t.plusMinutes(SLOT_MINUTES);
        }

        // + 追加枠（ブロック対象でないもの）
        for (LocalDateTime dt : extra) {
            if (!blocked.contains(dt)) {
                baseTimes.add(dt);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        return baseTimes.stream()
                .filter(dt -> dt.isAfter(now))
                .sorted()
                .map(dt -> new SlotInfo(dt, !booked.contains(dt)))
                .toList();
    }

    public record SlotInfo(LocalDateTime slotTime, boolean available) {}

    public AppointmentResponse createGeneralCare(Long petId, LocalDateTime scheduledAt, String note, String requestedChannel, LoginUser currentUser) {
        if (!currentUser.isAdmin() && !planAccessService.canUseAppointments(currentUser)) {
            throw new BadRequestException("この機能はスタンダード以上で利用できます");
        }
        if (scheduledAt == null || !scheduledAt.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("予約日時は未来日時を指定してください");
        }
        LocalTime t = scheduledAt.toLocalTime();
        if (t.isBefore(BUSINESS_START) || t.isAfter(BUSINESS_END.minusMinutes(1))) {
            throw new BadRequestException("診療受付時間外です（9:30〜17:00）");
        }
        if (appointmentMapper.countByScheduledAt(scheduledAt) > 0) {
            throw new BadRequestException("その時間帯はすでに予約済みです");
        }

        var pet = currentUser.hasStaffAccess()
                ? petMapper.findById(petId)
                : petMapper.findByIdAndOwnerUserId(petId, currentUser.id());
        if (pet == null) throw new NotFoundException("Pet not found: " + petId);
        if (pet.deceasedAt() != null) throw new BadRequestException("永眠登録済みのペットはこの操作を利用できません");

        String channel = "VISIT";
        if (!currentUser.isAdmin()) {
            PlanAccessService.PlanTier tier = planAccessService.resolvePlanTier(currentUser);
            if (tier == PlanAccessService.PlanTier.STANDARD && "ONLINE".equals(requestedChannel)) {
                throw new BadRequestException("スタンダードプランは来院予約のみ利用できます");
            }
            if (tier == PlanAccessService.PlanTier.PREMIUM && "ONLINE".equals(requestedChannel)) {
                channel = "ONLINE";
            }
        } else if ("ONLINE".equals(requestedChannel)) {
            channel = "ONLINE";
        }

        AppointmentEntity row = new AppointmentEntity(
                null, petId, currentUser.id(), null, "MEDICAL", channel,
                scheduledAt, "REQUESTED", null, note,
                null, null, null, null);
        Long createdId = appointmentMapper.insert(row);
        return get(createdId);
    }

    public AppointmentResponse approve(Long id, Long actorUserId) {
        AppointmentEntity existing = appointmentMapper.findById(id);
        if (existing == null) throw new NotFoundException("Appointment not found: " + id);
        if (!"REQUESTED".equals(existing.status())) throw new BadRequestException("申請中の予約のみ承認できます");

        String zoomJoinUrl = existing.zoomJoinUrl();
        if ("ONLINE".equals(existing.channel())) {
            ZoomLinkService.ZoomMeetingResult zoomResult = zoomLinkService.createMeetingOrFallback(
                    existing.scheduledAt(), "Pet Life Plus Premium Online Care");
            zoomJoinUrl = zoomResult.joinUrl();
        }

        AppointmentEntity updated = new AppointmentEntity(
                existing.id(), existing.petId(), existing.ownerUserId(), existing.staffUserId(),
                existing.appointmentType(), existing.channel(), existing.scheduledAt(), "CONFIRMED",
                zoomJoinUrl, existing.note(), existing.slotId(), existing.deletedAt(), existing.createdAt(), existing.updatedAt()
        );
        appointmentMapper.update(updated);
        notifyOwnerStatusChanged(updated, true, actorUserId);
        return toResponse(updated);
    }

    public AppointmentResponse reject(Long id, Long actorUserId) {
        AppointmentEntity existing = appointmentMapper.findById(id);
        if (existing == null) throw new NotFoundException("Appointment not found: " + id);
        if (!"REQUESTED".equals(existing.status())) throw new BadRequestException("申請中の予約のみ却下できます");
        AppointmentEntity canceled = new AppointmentEntity(
                existing.id(), existing.petId(), existing.ownerUserId(), existing.staffUserId(),
                existing.appointmentType(), existing.channel(), existing.scheduledAt(), "CANCELED",
                existing.zoomJoinUrl(), existing.note(), existing.slotId(), existing.deletedAt(),
                existing.createdAt(), existing.updatedAt());
        appointmentMapper.updateStatus(id, "CANCELED");
        notifyOwnerStatusChanged(canceled, false, actorUserId);
        return toResponse(canceled);
    }

    public AppointmentResponse cancelRequestedByOwner(Long id, LoginUser currentUser) {
        AppointmentEntity existing = appointmentMapper.findById(id);
        if (existing == null) throw new NotFoundException("Appointment not found: " + id);
        if (currentUser == null || !existing.ownerUserId().equals(currentUser.id())) {
            throw new BadRequestException("自分の予約のみキャンセルできます");
        }
        if (!"REQUESTED".equals(existing.status())) {
            throw new BadRequestException("申請中の予約のみキャンセルできます");
        }
        appointmentMapper.updateStatus(id, "CANCELED");
        notifyAdminsCanceledByOwner(existing, currentUser.id());
        return get(id);
    }

    public void delete(Long id) {
        if (appointmentMapper.softDelete(id, LocalDateTime.now()) == 0)
            throw new NotFoundException("Appointment not found: " + id);
    }

    public int deleteSelected(List<Long> appointmentIds, LoginUser currentUser) {
        if (appointmentIds == null || appointmentIds.isEmpty()) {
            throw new BadRequestException("削除対象を選択してください");
        }
        List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(appointmentIds));
        List<AppointmentEntity> entities = appointmentMapper.findByIds(uniqueIds);
        LocalDateTime now = LocalDateTime.now();
        for (AppointmentEntity e : entities) {
            if (!currentUser.canManageClinical() && !e.ownerUserId().equals(currentUser.id())) {
                throw new BadRequestException("自分の予約のみ削除できます");
            }
            if (e.scheduledAt() != null && e.scheduledAt().isAfter(now)) {
                throw new BadRequestException("未来の予約は削除できません");
            }
        }
        List<Long> ids = entities.stream().map(AppointmentEntity::id).toList();
        return appointmentMapper.softDeleteByIds(ids, now);
    }

    private void validateStatusTransition(String current, String next) {
        if (current.equals(next)) return;
        if ("CONFIRMED".equals(current) && "COMPLETED".equals(next)) return;
        throw new BadRequestException(
                "この操作では CONFIRMED → COMPLETED の遷移のみ許可されています。" +
                "承認・却下・キャンセルは専用エンドポイントを使用してください");
    }

    private void validateBusinessHours(LocalDateTime scheduledAt) {
        LocalTime time = scheduledAt.toLocalTime();
        if (time.isBefore(SLOT_OPEN) || time.isAfter(SLOT_CLOSE)) {
            throw new BadRequestException("予約時間は 09:30〜17:00 の範囲で指定してください");
        }
    }

    private void ensureNoDuplicate(Long staffUserId, LocalDateTime scheduledAt, Long excludeId) {
        if (staffUserId == null) return;
        if (appointmentMapper.countDuplicatedSlot(staffUserId, scheduledAt, excludeId) > 0) {
            throw new BadRequestException("Duplicated appointment slot for staff");
        }
    }

    private AppointmentResponse toResponse(AppointmentEntity row) {
        return new AppointmentResponse(row.id(), row.petId(), row.ownerUserId(), row.staffUserId(),
                row.appointmentType(), row.channel(), row.scheduledAt(), row.status(), row.zoomJoinUrl(), row.note());
    }

    private void notifyOwnerStatusChanged(AppointmentEntity appointment, boolean approved, Long actorUserId) {
        if (appointment.ownerUserId() == null) return;
        if (actorUserId == null) return;

        String petName = "ペット";
        var pet = petMapper.findById(appointment.petId());
        if (pet != null && pet.name() != null && !pet.name().isBlank()) {
            petName = pet.name();
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        String channelLabel = "ONLINE".equals(appointment.channel()) ? "Zoom診療" : "診療予約";
        String title = approved ? "診療予約が承認されました" : "診療予約が却下されました";
        StringBuilder body = new StringBuilder(petName + " の" + channelLabel + "（"
                + fmt.format(appointment.scheduledAt()) + "）は" + (approved ? "承認" : "却下") + "されました。");
        if (approved && "ONLINE".equals(appointment.channel()) && appointment.zoomJoinUrl() != null && !appointment.zoomJoinUrl().isBlank()) {
            body.append(" Zoom参加リンク: ").append(appointment.zoomJoinUrl());
        }

        NotificationEntity notification = new NotificationEntity(
                null, "INFO", title, body.toString(),
                null, LocalDateTime.now(), "SENT",
                actorUserId, null, null, null
        );
        Long notificationId = notificationMapper.insertReturningId(notification);
        if (notificationId == null) { log.warn("Failed to insert status-change notification for appointment {}", appointment.id()); return; }
        notificationMapper.insertRecipient(notificationId, appointment.ownerUserId());
        notificationMapper.updateRecipientStatus(notificationId, appointment.ownerUserId(), "SENT");
    }

    private void notifyAdminsCanceledByOwner(AppointmentEntity appointment, Long actorUserId) {
        List<Long> adminIds = userMapper.findAdminUserIds();
        if (adminIds.isEmpty()) return;

        String petName = "ペット";
        var pet = petMapper.findById(appointment.petId());
        if (pet != null && pet.name() != null && !pet.name().isBlank()) {
            petName = pet.name();
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        String channelLabel = "ONLINE".equals(appointment.channel()) ? "オンライン診療" : "来院診療";
        NotificationEntity notification = new NotificationEntity(
                null, "ALERT", "予約がキャンセルされました",
                petName + " の" + channelLabel + "予約（" + fmt.format(appointment.scheduledAt()) + "）が申請者によりキャンセルされました。",
                null, LocalDateTime.now(), "SENT",
                actorUserId, null, null, null
        );
        Long notificationId = notificationMapper.insertReturningId(notification);
        if (notificationId == null) { log.warn("Failed to insert cancel notification for appointment {}", appointment.id()); return; }
        for (Long adminId : adminIds) {
            notificationMapper.insertRecipient(notificationId, adminId);
            notificationMapper.updateRecipientStatus(notificationId, adminId, "SENT");
        }
    }

    public record PremiumOnlineCareResult(
            AppointmentResponse appointment,
            boolean zoomFallbackUsed,
            String zoomFallbackReason
    ) {}
}
