package com.example.petlife.service;

import com.example.petlife.dto.appointment.AppointmentCreateRequest;
import com.example.petlife.dto.appointment.AppointmentResponse;
import com.example.petlife.dto.appointment.AppointmentUpdateRequest;
import com.example.petlife.dto.common.PageResponse;
import com.example.petlife.entity.AppointmentEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.exception.NotFoundException;
import com.example.petlife.mapper.AppointmentMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {
    private final AppointmentMapper appointmentMapper;

    public AppointmentService(AppointmentMapper appointmentMapper) {
        this.appointmentMapper = appointmentMapper;
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
                req.scheduledAt(), req.status(), req.note(), null, null, null);
        appointmentMapper.insert(row);
        return get(row.id());
    }

    public AppointmentResponse update(Long id, AppointmentUpdateRequest req) {
        AppointmentEntity existing = appointmentMapper.findById(id);
        if (existing == null) throw new NotFoundException("Appointment not found: " + id);
        ensureNoDuplicate(req.staffUserId(), req.scheduledAt(), id);
        AppointmentEntity row = new AppointmentEntity(id, existing.petId(), existing.ownerUserId(), req.staffUserId(), req.appointmentType(), req.channel(),
                req.scheduledAt(), req.status(), req.note(), existing.deletedAt(), existing.createdAt(), existing.updatedAt());
        appointmentMapper.update(row);
        return get(id);
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
                row.channel(), row.scheduledAt(), row.status(), row.note());
    }
}
