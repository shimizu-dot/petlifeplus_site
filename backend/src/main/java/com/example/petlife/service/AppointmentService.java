package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.appointment.AppointmentCreateRequest;
import com.example.petlife.dto.appointment.AppointmentListRow;
import com.example.petlife.dto.appointment.AppointmentResponse;
import com.example.petlife.dto.appointment.AppointmentUpdateRequest;
import com.example.petlife.dto.common.PageResponse;
import com.example.petlife.entity.AppointmentEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.exception.NotFoundException;
import com.example.petlife.mapper.AppointmentMapper;
import com.example.petlife.mapper.PetMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {
    private final AppointmentMapper appointmentMapper;
    private final PlanAccessService planAccessService;
    private final ZoomLinkService zoomLinkService;
    private final PetMapper petMapper;

    public AppointmentService(AppointmentMapper appointmentMapper,
                              PlanAccessService planAccessService,
                              ZoomLinkService zoomLinkService,
                              PetMapper petMapper) {
        this.appointmentMapper = appointmentMapper;
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
        AppointmentEntity row = new AppointmentEntity(null, req.petId(), req.ownerUserId(), req.staffUserId(), req.appointmentType(), req.channel(),
                req.scheduledAt(), req.status(), null, req.note(), null, null, null);
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
        AppointmentEntity row = new AppointmentEntity(id, existing.petId(), existing.ownerUserId(), req.staffUserId(), req.appointmentType(), req.channel(),
                req.scheduledAt(), req.status(), existing.zoomJoinUrl(), req.note(), existing.deletedAt(), existing.createdAt(), existing.updatedAt());
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
        if (pet == null) {
            throw new NotFoundException("Pet not found: " + petId);
        }
        if (pet.deceasedAt() != null) {
            throw new BadRequestException("永眠登録済みのペットはこの操作を利用できません");
        }
        ZoomLinkService.ZoomMeetingResult zoomResult = zoomLinkService.createMeetingOrFallback(
                scheduledAt,
                "Pet Life Plus Premium Online Care"
        );
        AppointmentEntity row = new AppointmentEntity(
                null,
                petId,
                currentUser.id(),
                null,
                "MEDICAL",
                "ONLINE",
                scheduledAt,
                "REQUESTED",
                zoomResult.joinUrl(),
                note,
                null,
                null,
                null
        );
        Long createdId = appointmentMapper.insert(row);
        AppointmentResponse created = get(createdId);
        return new PremiumOnlineCareResult(created, zoomResult.fallbackUsed(), zoomResult.fallbackReason());
    }

    public AppointmentResponse createGeneralCare(Long petId,
                                                 LocalDateTime scheduledAt,
                                                 String note,
                                                 LoginUser currentUser) {
        if (!currentUser.isAdmin() && !planAccessService.canUseAiSymptom(currentUser)) {
            throw new BadRequestException("この機能はスタンダード以上で利用できます");
        }
        if (scheduledAt == null || !scheduledAt.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("予約日時は未来日時を指定してください");
        }
        if (currentUser.canManagePets()) {
            var pet = petMapper.findById(petId);
            if (pet == null) {
                throw new NotFoundException("Pet not found: " + petId);
            }
            if (pet.deceasedAt() != null) {
                throw new BadRequestException("永眠登録済みのペットはこの操作を利用できません");
            }
        } else {
            var pet = petMapper.findByIdAndOwnerUserId(petId, currentUser.id());
            if (pet == null) {
                throw new NotFoundException("Pet not found: " + petId);
            }
            if (pet.deceasedAt() != null) {
                throw new BadRequestException("永眠登録済みのペットはこの操作を利用できません");
            }
        }
        AppointmentEntity row = new AppointmentEntity(
                null,
                petId,
                currentUser.id(),
                null,
                "MEDICAL",
                "VISIT",
                scheduledAt,
                "REQUESTED",
                null,
                note,
                null,
                null,
                null
        );
        Long createdId = appointmentMapper.insert(row);
        return get(createdId);
    }

    public void delete(Long id) {
        if (appointmentMapper.softDelete(id, LocalDateTime.now()) == 0) throw new NotFoundException("Appointment not found: " + id);
    }

    private void ensureNoDuplicate(Long staffUserId, LocalDateTime scheduledAt, Long excludeId) {
        if (staffUserId == null) return;
        if (appointmentMapper.countDuplicatedSlot(staffUserId, scheduledAt, excludeId) > 0) {
            throw new BadRequestException("Duplicated appointment slot for staff");
        }
    }

    private AppointmentResponse toResponse(AppointmentEntity row) {
        return new AppointmentResponse(row.id(), row.petId(), row.ownerUserId(), row.staffUserId(), row.appointmentType(),
                row.channel(), row.scheduledAt(), row.status(), row.zoomJoinUrl(), row.note());
    }

    public record PremiumOnlineCareResult(
            AppointmentResponse appointment,
            boolean zoomFallbackUsed,
            String zoomFallbackReason
    ) {}
}
