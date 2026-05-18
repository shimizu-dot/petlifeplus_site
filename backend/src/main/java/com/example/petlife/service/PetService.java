package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.common.PageResponse;
import com.example.petlife.dto.pet.PetCreateRequest;
import com.example.petlife.dto.pet.PetResponse;
import com.example.petlife.dto.pet.PetUpdateRequest;
import com.example.petlife.entity.PetEntity;
import com.example.petlife.exception.NotFoundException;
import com.example.petlife.mapper.PetMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PetService {

    private final PetMapper petMapper;

    public PetService(PetMapper petMapper) {
        this.petMapper = petMapper;
    }

    // ---- 一覧（ロール別） ----

    public PageResponse<PetResponse> list(int page, int size, LoginUser currentUser) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;

        if (currentUser.isAdmin()) {
            List<PetResponse> items = petMapper.findAll(safeSize, offset).stream().map(this::toResponse).toList();
            return new PageResponse<>(items, safePage, safeSize, petMapper.countAll());
        } else {
            List<PetResponse> items = petMapper.findByOwnerUserId(currentUser.id(), safeSize, offset)
                    .stream().map(this::toResponse).toList();
            return new PageResponse<>(items, safePage, safeSize, petMapper.countByOwnerUserId(currentUser.id()));
        }
    }

    // ---- 単件（オーナー確認） ----

    public PetResponse get(Long id, LoginUser currentUser) {
        return toResponse(resolvePet(id, currentUser));
    }

    public PetEntity getEntity(Long id, LoginUser currentUser) {
        return resolvePet(id, currentUser);
    }

    // ---- 作成 ----

    public PetResponse create(PetCreateRequest req, LoginUser currentUser) {
        Long ownerId = currentUser.isAdmin() ? req.ownerUserId() : currentUser.id();
        PetEntity row = new PetEntity(
                null, ownerId, req.name(), req.species(), req.breed(),
                req.sex(), req.birthDate(), req.weightBaselineKg(),
                null, null, null
        );
        Long newId = petMapper.insertReturningId(row);
        return toResponse(petMapper.findById(newId));
    }

    // ---- 更新 ----

    public PetResponse update(Long id, PetUpdateRequest req, LoginUser currentUser) {
        PetEntity existing = resolvePet(id, currentUser);
        PetEntity row = new PetEntity(
                id, existing.ownerUserId(), req.name(), req.species(), req.breed(),
                req.sex(), req.birthDate(), req.weightBaselineKg(),
                existing.deletedAt(), existing.createdAt(), existing.updatedAt()
        );
        petMapper.update(row);
        return toResponse(petMapper.findById(id));
    }

    // ---- 削除 ----

    public void delete(Long id, LoginUser currentUser) {
        resolvePet(id, currentUser);
        if (petMapper.softDelete(id, LocalDateTime.now()) == 0) {
            throw new NotFoundException("Pet not found: " + id);
        }
    }

    // ---- 内部ヘルパー ----

    private PetEntity resolvePet(Long id, LoginUser currentUser) {
        PetEntity row = currentUser.isAdmin()
                ? petMapper.findById(id)
                : petMapper.findByIdAndOwnerUserId(id, currentUser.id());
        if (row == null) throw new NotFoundException("Pet not found: " + id);
        return row;
    }

    public PetResponse toResponse(PetEntity row) {
        return new PetResponse(row.id(), row.ownerUserId(), row.name(), row.species(),
                row.breed(), row.sex(), row.birthDate(), row.weightBaselineKg());
    }
}
