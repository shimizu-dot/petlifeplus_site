package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.entity.AppointmentSlotEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.exception.NotFoundException;
import com.example.petlife.mapper.AppointmentSlotMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentSlotService {

    private final AppointmentSlotMapper slotMapper;

    public AppointmentSlotService(AppointmentSlotMapper slotMapper) {
        this.slotMapper = slotMapper;
    }

    public List<AppointmentSlotEntity> list(LoginUser currentUser) {
        ensureAccess(currentUser);
        return slotMapper.findAll();
    }

    public void create(LocalDateTime slotDatetime, String note, LoginUser currentUser) {
        ensureAccess(currentUser);
        if (slotDatetime == null || !slotDatetime.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("予約枠は未来日時で指定してください");
        }
        slotMapper.insert(new AppointmentSlotEntity(null, slotDatetime, note, currentUser.id(), null, null));
    }

    public void delete(Long id, LoginUser currentUser) {
        ensureAccess(currentUser);
        if (slotMapper.findById(id) == null) {
            throw new NotFoundException("予約枠が見つかりません: " + id);
        }
        if (slotMapper.countBookings(id) > 0) {
            throw new BadRequestException("申請済みの予約枠は削除できません");
        }
        slotMapper.softDelete(id, LocalDateTime.now());
    }

    private void ensureAccess(LoginUser currentUser) {
        if (!currentUser.canManageOperations()) {
            throw new BadRequestException("管理者・スタッフのみアクセスできます");
        }
    }
}
