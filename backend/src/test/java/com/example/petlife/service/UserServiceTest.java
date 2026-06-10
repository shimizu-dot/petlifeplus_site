package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.user.UserCreateRequest;
import com.example.petlife.dto.user.UserUpdateRequest;
import com.example.petlife.entity.UserEntity;
import com.example.petlife.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserMapper userMapper;
    @Mock BCryptPasswordEncoder passwordEncoder;
    @Mock PlanAccessService planAccessService;

    @InjectMocks UserService userService;

    private static LoginUser adminUser() {
        return new LoginUser(99L, 1L, "管理者", "admin@example.com", "hash", true);
    }

    @Test
    void createGeneralUserCreatesActiveSubscriptionForSelectedPlan() {
        when(userMapper.existsByEmail("owner@example.com")).thenReturn(0);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userMapper.findByEmail("owner@example.com"))
                .thenReturn(new UserEntity(10L, 3L, "オーナー", "owner@example.com", "hashed", null, null, null, "ACTIVE", null, null, null, null));
        when(userMapper.findById(10L))
                .thenReturn(new UserEntity(10L, 3L, "オーナー", "owner@example.com", "hashed", null, null, null, "ACTIVE", null, null, null, null));
        when(userMapper.findPlanIdByName("STANDARD")).thenReturn(2L);

        userService.create(new UserCreateRequest(3L, "オーナー", "owner@example.com", "password123", null, null, null, "STANDARD"), adminUser());

        verify(userMapper).insertUser(any());
        verify(userMapper).insertActiveSubscription(10L, 2L);
    }

    @Test
    void updateGeneralUserCreatesSubscriptionWhenNoneExists() {
        UserEntity existing = new UserEntity(10L, 3L, "オーナー", "owner@example.com", "hashed", null, null, null, "ACTIVE", null, null, null, null);
        when(userMapper.findById(10L)).thenReturn(existing);
        when(userMapper.existsByEmailExcludingId("owner@example.com", 10L)).thenReturn(0);
        when(userMapper.update(any())).thenReturn(1);
        when(userMapper.findPlanIdByName("LIGHT")).thenReturn(1L);
        when(userMapper.updateActiveSubscriptionPlanByUserId(10L, 1L)).thenReturn(0);
        when(userMapper.findById(10L)).thenReturn(existing);

        userService.update(10L, new UserUpdateRequest(3L, "LIGHT", "オーナー", "owner@example.com", null, null, null, null, "ACTIVE"), adminUser());

        verify(userMapper).insertActiveSubscription(10L, 1L);
    }
}
