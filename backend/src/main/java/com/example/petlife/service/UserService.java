package com.example.petlife.service;

import com.example.petlife.dto.common.PageResponse;
import com.example.petlife.dto.user.UserCreateRequest;
import com.example.petlife.dto.user.UserResponse;
import com.example.petlife.dto.user.UserUpdateRequest;
import com.example.petlife.entity.UserEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.exception.NotFoundException;
import com.example.petlife.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, BCryptPasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResponse<UserResponse> list(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<UserResponse> items = userMapper.findAll(safeSize, offset).stream().map(this::toResponse).toList();
        return new PageResponse<>(items, safePage, safeSize, userMapper.countAll());
    }

    public UserResponse get(Long id) {
        UserEntity row = userMapper.findById(id);
        if (row == null) throw new NotFoundException("User not found: " + id);
        return toResponse(row);
    }

    public UserResponse create(UserCreateRequest req) {
        if (userMapper.existsByEmail(req.email()) > 0) {
            throw new BadRequestException("Email already exists");
        }
        Long roleId = req.roleId() != null ? req.roleId() : 2L;
        UserEntity row = new UserEntity(
                null, roleId, req.name(), req.email(),
                passwordEncoder.encode(req.password()),
                req.phone(), "ACTIVE", null, null, null, null
        );
        Long newId = userMapper.insertReturningId(row);
        auditLog.info("action=user_create userId={} email={} roleId={}", newId, req.email(), roleId);
        return get(newId);
    }

    public UserResponse update(Long id, UserUpdateRequest req) {
        UserEntity existing = userMapper.findById(id);
        if (existing == null) throw new NotFoundException("User not found: " + id);
        if (userMapper.existsByEmailExcludingId(req.email(), id) > 0) {
            throw new BadRequestException("Email already exists");
        }
        Long nextRoleId = req.roleId() != null ? req.roleId() : existing.roleId();
        UserEntity row = new UserEntity(
                id, nextRoleId, req.name(), req.email(),
                existing.passwordHash(), req.phone(),
                req.status(), existing.lastLoginAt(), existing.deletedAt(),
                existing.createdAt(), existing.updatedAt()
        );
        userMapper.update(row);

        if (nextRoleId == 2L) {
            String desiredPlan = req.planTier() == null || req.planTier().isBlank() ? "LIGHT" : req.planTier().toUpperCase();
            Long planId = userMapper.findPlanIdByName(desiredPlan);
            if (planId == null) {
                throw new BadRequestException("Unknown plan: " + desiredPlan);
            }
            int updated = userMapper.updateActiveSubscriptionPlanByUserId(id, planId);
            if (updated == 0) {
                throw new BadRequestException("Active subscription not found for user");
            }
            auditLog.info("action=user_plan_update userId={} plan={}", id, desiredPlan);
        }
        auditLog.info("action=user_update userId={} roleId={} status={}", id, nextRoleId, req.status());
        return get(id);
    }

    public void delete(Long id) {
        if (userMapper.softDelete(id, LocalDateTime.now()) == 0) {
            throw new NotFoundException("User not found: " + id);
        }
        auditLog.info("action=user_delete userId={}", id);
    }

    public UserResponse toResponse(UserEntity row) {
        String roleDisplay = switch (row.roleId().intValue()) {
            case 1 -> "管理者";
            case 3 -> "獣医師";
            case 4 -> "スタッフ";
            default -> {
                String plan = userMapper.findActivePlanNameByUserId(row.id());
                if ("PREMIUM".equals(plan)) {
                    yield "Premium";
                } else if ("STANDARD".equals(plan)) {
                    yield "Standard";
                } else {
                    yield "Light";
                }
            }
        };
        return new UserResponse(row.id(), row.roleId(), roleDisplay, row.name(), row.email(), row.phone(), row.status());
    }

    public UserEntity findEntity(Long id) {
        UserEntity row = userMapper.findById(id);
        if (row == null) throw new NotFoundException("User not found: " + id);
        return row;
    }

    public String findActivePlanNameByUserId(Long userId) {
        return userMapper.findActivePlanNameByUserId(userId);
    }
}
