package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.common.PageResponse;
import com.example.petlife.dto.health.HealthRecordCreateRequest;
import com.example.petlife.dto.health.HealthRecordResponse;
import com.example.petlife.dto.health.HealthRecordUpdateRequest;
import com.example.petlife.entity.HealthRecordEntity;
import com.example.petlife.entity.PetEntity;
import com.example.petlife.exception.NotFoundException;
import com.example.petlife.mapper.HealthRecordMapper;
import com.example.petlife.mapper.PetMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HealthRecordService {

    private final HealthRecordMapper healthRecordMapper;
    private final PetMapper petMapper;
    private final HealthRecordImageStorageService healthRecordImageStorageService;

    public HealthRecordService(HealthRecordMapper healthRecordMapper, PetMapper petMapper,
                               HealthRecordImageStorageService healthRecordImageStorageService) {
        this.healthRecordMapper = healthRecordMapper;
        this.petMapper = petMapper;
        this.healthRecordImageStorageService = healthRecordImageStorageService;
    }

    // ---- 一覧（ペット別・オーナー確認済み） ----

    public PageResponse<HealthRecordResponse> listForPet(Long petId, int page, int size, LoginUser currentUser) {
        verifyPetAccess(petId, currentUser);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<HealthRecordResponse> items = healthRecordMapper.findByPetId(petId, safeSize, offset)
                .stream().map(this::toResponse).toList();
        return new PageResponse<>(items, safePage, safeSize, healthRecordMapper.countByPetId(petId));
    }

    // ---- 単件 ----

    public HealthRecordResponse get(Long id, Long petId, LoginUser currentUser) {
        verifyPetAccess(petId, currentUser);
        HealthRecordEntity row = healthRecordMapper.findById(id);
        if (row == null || !row.petId().equals(petId)) throw new NotFoundException("Record not found: " + id);
        return toResponse(row);
    }

    public HealthRecordEntity getEntity(Long id, Long petId, LoginUser currentUser) {
        verifyPetAccess(petId, currentUser);
        HealthRecordEntity row = healthRecordMapper.findById(id);
        if (row == null || !row.petId().equals(petId)) throw new NotFoundException("Record not found: " + id);
        return row;
    }

    // ---- 作成 ----

    public HealthRecordResponse create(Long petId, HealthRecordCreateRequest req, MultipartFile imageFile, LoginUser currentUser) {
        verifyPetAccess(petId, currentUser);
        String imagePath = healthRecordImageStorageService.store(imageFile);
        HealthRecordEntity row = new HealthRecordEntity(
                null, petId, currentUser.id(),
                req.recordDate(), req.weightKg(), req.mealMemo(),
                req.exerciseMinutes(), req.mealScore(), req.exerciseScore(), req.sleepScore(), req.moodScore(),
                req.overallScore(),
                imagePath, req.note(),
                null, null, null
        );
        Long newId = healthRecordMapper.insertReturningId(row);
        return toResponse(healthRecordMapper.findById(newId));
    }

    // ---- 更新 ----

    public HealthRecordResponse update(Long id, Long petId, HealthRecordUpdateRequest req, MultipartFile imageFile, LoginUser currentUser) {
        HealthRecordEntity existing = getEntity(id, petId, currentUser);
        String imagePath = existing.imagePath();
        String uploaded = healthRecordImageStorageService.store(imageFile);
        if (uploaded != null) imagePath = uploaded;
        HealthRecordEntity row = new HealthRecordEntity(
                id, petId, existing.recordedByUserId(),
                req.recordDate(), req.weightKg(), req.mealMemo(),
                req.exerciseMinutes(), req.mealScore(), req.exerciseScore(), req.sleepScore(), req.moodScore(),
                req.overallScore(),
                imagePath, req.note(),
                existing.deletedAt(), existing.createdAt(), existing.updatedAt()
        );
        healthRecordMapper.update(row);
        return toResponse(healthRecordMapper.findById(id));
    }

    // ---- 削除 ----

    public void delete(Long id, Long petId, LoginUser currentUser) {
        getEntity(id, petId, currentUser);
        if (healthRecordMapper.softDelete(id, LocalDateTime.now()) == 0) {
            throw new NotFoundException("Record not found: " + id);
        }
    }

    // ---- 内部ヘルパー ----

    private void verifyPetAccess(Long petId, LoginUser currentUser) {
        PetEntity pet = currentUser.isAdmin()
                ? petMapper.findById(petId)
                : petMapper.findByIdAndOwnerUserId(petId, currentUser.id());
        if (pet == null) throw new NotFoundException("Pet not found: " + petId);
    }

    public HealthRecordResponse toResponse(HealthRecordEntity row) {
        return new HealthRecordResponse(row.id(), row.petId(), row.recordedByUserId(),
                row.recordDate(), row.weightKg(), row.mealMemo(), row.exerciseMinutes(),
                row.mealScore(), row.exerciseScore(), row.sleepScore(), row.moodScore(), row.overallScore(), row.imagePath(), row.note());
    }
}
