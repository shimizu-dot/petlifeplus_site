package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.common.PageResponse;
import com.example.petlife.dto.pet.PetCreateRequest;
import com.example.petlife.dto.pet.PetResponse;
import com.example.petlife.dto.pet.PetUpdateRequest;
import com.example.petlife.entity.PetEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.exception.NotFoundException;
import com.example.petlife.mapper.PetMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PetService {

    private final PetMapper petMapper;
    private final PetImageStorageService petImageStorageService;

    public PetService(PetMapper petMapper, PetImageStorageService petImageStorageService) {
        this.petMapper = petMapper;
        this.petImageStorageService = petImageStorageService;
    }

    // ---- 一覧（ロール別） ----

    public PageResponse<PetResponse> list(int page, int size, LoginUser currentUser) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;

        if (currentUser.hasStaffAccess()) {
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

    public List<PetEntity> listOwnedEntities(LoginUser currentUser) {
        if (currentUser.hasStaffAccess()) {
            return petMapper.findAll(200, 0);
        }
        return petMapper.findActiveByOwnerUserId(currentUser.id());
    }

    public List<PetResponse> listByOwnerUserIdForAdmin(Long ownerUserId) {
        return petMapper.findActiveByOwnerUserId(ownerUserId).stream().map(this::toResponse).toList();
    }

    public boolean canDeletePet(Long petId, LoginUser currentUser) {
        resolvePet(petId, currentUser);
        return petMapper.countLinkedDataFlags(petId) == 0;
    }

    // ---- 作成 ----

    public PetResponse create(PetCreateRequest req, MultipartFile imageFile, LoginUser currentUser) {
        validateDogOnly(req.species());
        Long ownerId = currentUser.hasStaffAccess() ? req.ownerUserId() : currentUser.id();
        String imagePath = petImageStorageService.store(imageFile);
        PetEntity row = new PetEntity(
                null, ownerId, req.name(), req.species(), req.breed(),
                req.sex(), req.birthDate(), req.weightBaselineKg(), imagePath,
                null,
                null, null, null
        );
        Long newId = petMapper.insertReturningId(row);
        return toResponse(petMapper.findById(newId));
    }

    // ---- 更新 ----

    public PetResponse update(Long id, PetUpdateRequest req, MultipartFile imageFile, LoginUser currentUser) {
        validateDogOnly(req.species());
        PetEntity existing = resolvePet(id, currentUser);
        String imagePath = existing.imagePath();
        String uploadedPath = petImageStorageService.store(imageFile);
        if (uploadedPath != null) {
            imagePath = uploadedPath;
        }
        PetEntity row = new PetEntity(
                id, existing.ownerUserId(), req.name(), req.species(), req.breed(),
                req.sex(), req.birthDate(), req.weightBaselineKg(), imagePath,
                existing.deceasedAt(),
                existing.deletedAt(), existing.createdAt(), existing.updatedAt()
        );
        petMapper.update(row);
        return toResponse(petMapper.findById(id));
    }

    // ---- 削除 ----

    public void delete(Long id, LoginUser currentUser) {
        resolvePet(id, currentUser);
        if (petMapper.countLinkedDataFlags(id) > 0) {
            throw new BadRequestException("このペットは他データと連携済みのため削除できません。代わりに「永眠」ボタンを使用してください。");
        }
        if (petMapper.softDelete(id, LocalDateTime.now()) == 0) {
            throw new NotFoundException("Pet not found: " + id);
        }
    }

    public void markDeceased(Long id, LoginUser currentUser) {
        resolvePet(id, currentUser);
        if (petMapper.markDeceased(id, LocalDateTime.now()) == 0) {
            throw new NotFoundException("Pet not found: " + id);
        }
    }

    // ---- 内部ヘルパー ----

    private PetEntity resolvePet(Long id, LoginUser currentUser) {
        PetEntity row = currentUser.hasStaffAccess()
                ? petMapper.findById(id)
                : petMapper.findByIdAndOwnerUserId(id, currentUser.id());
        if (row == null) throw new NotFoundException("Pet not found: " + id);
        return row;
    }

    public PetResponse toResponse(PetEntity row) {
        return new PetResponse(row.id(), row.ownerUserId(), row.name(), row.species(),
                row.breed(), row.sex(), row.birthDate(), row.weightBaselineKg(), row.imagePath(), row.deceasedAt());
    }

    public void ensurePetUsable(PetEntity pet) {
        if (pet.deceasedAt() != null) {
            throw new BadRequestException("永眠登録済みのペットはこの操作を利用できません");
        }
    }

    private void validateDogOnly(String species) {
        if (!"DOG".equals(species)) {
            throw new BadRequestException("現在対応している種別は犬（DOG）のみです");
        }
    }
}
