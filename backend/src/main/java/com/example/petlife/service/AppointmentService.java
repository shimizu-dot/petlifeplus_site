package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.appointment.AppointmentCreateRequest;
import com.example.petlife.dto.appointment.AppointmentListRow;
import com.example.petlife.dto.appointment.AppointmentResponse;
import com.example.petlife.dto.appointment.AppointmentUpdateRequest;
import com.example.petlife.dto.common.PageResponse;
import com.example.petlife.entity.AppointmentEntity;
import com.example.petlife.entity.AppointmentSlotEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.exception.NotFoundException;
import com.example.petlife.mapper.AppointmentMapper;
import com.example.petlife.mapper.AppointmentSlotMapper;
import com.example.petlife.mapper.PetMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentService {
    private final AppointmentMapper appointmentMapper;
    private final AppointmentSlotMapper appointmentSlotMapper;
    private final PlanAccessService planAccessService;
    private final ZoomLinkService zoomLinkService;
    private final PetMapper petMapper;

    public AppointmentService(AppointmentMapper appointmentMapper,
                              AppointmentSlotMapper appointmentSlotMapper,
                              PlanAccessService planAccessService,
                              ZoomLinkService zoomLinkService,
                              PetMapper petMapper) {
        this.appointmentMapper = appointmentMapper;
        this.appointmentSlotMapper = appointmentSlotMapper;
        this.planAccessService = planAccessService;
        this.zoomLinkService = zoomLinkService;
        this.petMapper = petMapper;
    }

    public PageResponse<AppointmentResponse> list(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<AppointmentResponse> items = appointmentMapper.findAll(safeSize, offset).stream().map(this::toResponse).toList();
        return new PageResponse<>(items, safePage, safeSize, appointmentMapper.countAll());
    }

    public AppointmentResponse get(Long id) {
        AppointmentEntity row = appointmentMapper.findById(id);
        if (row == null) throw new NotFoundException("Appointment not found: " + id);
        return toResponse(row);
    }

    public AppointmentResponse create(AppointmentCreateRequest req) {
        ensureNoDuplicate(req.staffUserId(), req.scheduledAt(), null);
        AppointmentEntity row = new AppointmentEntity(null, req.petId(), req.ownerUserId(), req.staffUserId(),
                req.appointmentType(), req.channel(), req.scheduledAt(), req.status(), null, req.note(),
                null, null, null, null);
        Long createdId = appointmentMapper.insert(row);
        return get(createdId);
    }

    public PageResponse<AppointmentListRow> listForApp(int page, int size, LoginUser currentUser) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        if (currentUser.isAdmin()) {
            List<AppointmentListRow> items = appointmentMapper.findAllRows(safeSize, offset);
            return new PageResponse<>(items, safePage, safeSize, appointmentMapper.countAll());
        }
        List<AppointmentListRow> items = appointmentMapper.findRowsByOwnerUserId(currentUser.id(), safeSize, offset);
        return new PageResponse<>(items, safePage, safeSize, appointmentMapper.countByOwnerUserId(currentUser.id()));
    }

    public AppointmentResponse update(Long id, AppointmentUpdateRequest req) {
        AppointmentEntity existing = appointmentMapper.findById(id);
        if (existing == null) throw new NotFoundException("Appointment not found: " + id);
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
        var pet = petMapper.findByIdAndOwnerUserId(petId, currentUser.id());
        if (pet == null) throw new NotFoundException("Pet not found: " + petId);
        if (pet.deceasedAt() != null) throw new BadRequestException("永眠登録済みのペットはこの操作を利用できません");

        ZoomLinkService.ZoomMeetingResult zoomResult = zoomLinkService.createMeetingOrFallback(
                scheduledAt, "Pet Life Plus Premium Online Care");
        AppointmentEntity row = new AppointmentEntity(
                null, petId, currentUser.id(), null, "MEDICAL", "ONLINE",
                scheduledAt, "REQUESTED", zoomResult.joinUrl(), note,
                null, null, null, null);
        Long createdId = appointmentMapper.insert(row);
        AppointmentResponse created = get(createdId);
        return new PremiumOnlineCareResult(created, zoomResult.fallbackUsed(), zoomResult.fallbackReason());
    }

    private static final LocalTime BUSINESS_START = LocalTime.of(9, 30);
    private static final LocalTime BUSINESS_END   = LocalTime.of(17, 0);
    private static final int SLOT_MINUTES = 30;

    public List<LocalDateTime> generateAvailableSlots(LocalDate date) {
        List<LocalDateTime> slots = new ArrayList<>();
        LocalTime t = BUSINESS_START;
        while (!t.isAfter(BUSINESS_END.minusMinutes(1))) {
            LocalDateTime dt = LocalDateTime.of(date, t);
            if (dt.isAfter(LocalDateTime.now()) && appointmentMapper.countByScheduledAt(dt) == 0) {
                slots.add(dt);
            }
            t = t.plusMinutes(SLOT_MINUTES);
        }
        return slots;
    }

    public AppointmentResponse createGeneralCare(Long petId, LocalDateTime scheduledAt, String note, LoginUser currentUser) {
        if (!currentUser.isAdmin() && !planAccessService.canUseAiSymptom(currentUser)) {
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

        var pet = currentUser.canManagePets()
                ? petMapper.findById(petId)
                : petMapper.findByIdAndOwnerUserId(petId, currentUser.id());
        if (pet == null) throw new NotFoundException("Pet not found: " + petId);
        if (pet.deceasedAt() != null) throw new BadRequestException("永眠登録済みのペットはこの操作を利用できません");

        AppointmentEntity row = new AppointmentEntity(
                null, petId, currentUser.id(), null, "MEDICAL", "VISIT",
                scheduledAt, "REQUESTED", null, note,
                null, null, null, null);
        Long createdId = appointmentMapper.insert(row);
        return get(createdId);
    }

    public AppointmentResponse approve(Long id) {
        AppointmentEntity existing = appointmentMapper.findById(id);
        if (existing == null) throw new NotFoundException("Appointment not found: " + id);
        if (!"REQUESTED".equals(existing.status())) throw new BadRequestException("申請中の予約のみ承認できます");
        appointmentMapper.updateStatus(id, "CONFIRMED");
        return get(id);
    }

    public AppointmentResponse reject(Long id) {
        AppointmentEntity existing = appointmentMapper.findById(id);
        if (existing == null) throw new NotFoundException("Appointment not found: " + id);
        if (!"REQUESTED".equals(existing.status())) throw new BadRequestException("申請中の予約のみ却下できます");
        appointmentMapper.updateStatus(id, "CANCELED");
        return get(id);
    }

    public void delete(Long id) {
        if (appointmentMapper.softDelete(id, LocalDateTime.now()) == 0)
            throw new NotFoundException("Appointment not found: " + id);
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

    public record PremiumOnlineCareResult(
            AppointmentResponse appointment,
            boolean zoomFallbackUsed,
            String zoomFallbackReason
    ) {}
}
