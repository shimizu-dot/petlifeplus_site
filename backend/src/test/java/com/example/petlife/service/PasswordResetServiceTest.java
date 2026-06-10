package com.example.petlife.service;

import com.example.petlife.entity.PasswordResetTokenEntity;
import com.example.petlife.entity.UserEntity;
import com.example.petlife.mapper.PasswordResetTokenMapper;
import com.example.petlife.mapper.UserMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock UserMapper userMapper;
    @Mock PasswordResetTokenMapper tokenMapper;
    @Mock BCryptPasswordEncoder passwordEncoder;
    @Mock JavaMailSender mailSender;

    @InjectMocks PasswordResetService passwordResetService;

    @Test
    void initiateResetCreatesTokenAndSendsMailWithoutSendGridKey() {
        ReflectionTestUtils.setField(passwordResetService, "fromEmail", "noreply@petlife.local");
        ReflectionTestUtils.setField(passwordResetService, "fromName", "ペットライフプラス");
        ReflectionTestUtils.setField(passwordResetService, "baseUrl", "http://localhost:8080");

        UserEntity user = new UserEntity(10L, 3L, "オーナー", "owner@example.com", "hash", null, null, null, "ACTIVE", null, null, null, null);
        when(userMapper.findByEmail("owner@example.com")).thenReturn(user);
        when(tokenMapper.countRecentByUserId(eq(10L), any())).thenReturn(0L);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        passwordResetService.initiateReset("owner@example.com");

        verify(tokenMapper).invalidateByUserId(10L);
        verify(tokenMapper).insert(any(PasswordResetTokenEntity.class));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void unknownEmailDoesNotCreateToken() {
        when(userMapper.findByEmail("missing@example.com")).thenReturn(null);

        passwordResetService.initiateReset("missing@example.com");

        verifyNoInteractions(tokenMapper, mailSender);
    }
}
