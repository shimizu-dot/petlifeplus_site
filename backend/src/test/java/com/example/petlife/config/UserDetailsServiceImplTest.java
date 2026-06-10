package com.example.petlife.config;

import com.example.petlife.entity.UserEntity;
import com.example.petlife.mapper.AuthMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTest {

    @Test
    void activeGeneralUserCanLogInWithoutActiveSubscription() {
        AuthMapper authMapper = mock(AuthMapper.class);
        when(authMapper.findByEmail("owner@example.com"))
                .thenReturn(new UserEntity(10L, 3L, "オーナー", "owner@example.com", "hash", null, null, null, "ACTIVE", null, null, null, null));

        UserDetailsServiceImpl service = new UserDetailsServiceImpl(authMapper);

        LoginUser user = (LoginUser) service.loadUserByUsername("owner@example.com");

        assertTrue(user.isEnabled());
    }

    @Test
    void suspendedUserCannotLogIn() {
        AuthMapper authMapper = mock(AuthMapper.class);
        when(authMapper.findByEmail("owner@example.com"))
                .thenReturn(new UserEntity(10L, 3L, "オーナー", "owner@example.com", "hash", null, null, null, "SUSPENDED", null, null, null, null));

        UserDetailsServiceImpl service = new UserDetailsServiceImpl(authMapper);

        LoginUser user = (LoginUser) service.loadUserByUsername("owner@example.com");

        assertFalse(user.isEnabled());
    }
}
