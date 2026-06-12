package com.example.petlife.service.line;

import com.example.petlife.entity.LineLinkTokenEntity;
import com.example.petlife.entity.UserEntity;
import com.example.petlife.mapper.LineLinkTokenMapper;
import com.example.petlife.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LineUserLinkServiceTest {

    @Mock UserMapper userMapper;
    @Mock LineLinkTokenMapper tokenMapper;

    @InjectMocks LineUserLinkService lineUserLinkService;

    @Test
    void linkByMessageLinksWithOneTimeToken() {
        LineLinkTokenEntity token = new LineLinkTokenEntity(
                1L, 10L, "123456", LocalDateTime.now().plusMinutes(10), null, LocalDateTime.now());
        UserEntity user = new UserEntity(10L, 3L, "オーナー", "owner@example.com", "hash",
                null, null, null, "ACTIVE", null, null, null, null);
        when(tokenMapper.findValidByToken("123456")).thenReturn(token);
        when(userMapper.findById(10L)).thenReturn(user);
        when(userMapper.saveLineUserId(10L, "U_LINE_123")).thenReturn(1);

        LineUserLinkService.LinkResult result = lineUserLinkService.linkByMessage("U_LINE_123", "連携 123456");

        assertEquals(LineUserLinkService.LinkResult.LINKED, result);
        verify(userMapper).saveLineUserId(10L, "U_LINE_123");
        verify(tokenMapper).markUsed(1L);
    }

    @Test
    void linkByMessageLinksWithRegisteredEmailWithoutPrefix() {
        UserEntity user = new UserEntity(10L, 3L, "オーナー", "owner@example.com", "hash",
                null, null, null, "ACTIVE", null, null, null, null);
        when(userMapper.findByEmail("owner@example.com")).thenReturn(user);
        when(userMapper.saveLineUserId(10L, "U_LINE_456")).thenReturn(1);

        LineUserLinkService.LinkResult result = lineUserLinkService.linkByMessage("U_LINE_456", "owner@example.com");

        assertEquals(LineUserLinkService.LinkResult.LINKED, result);
        verify(userMapper).saveLineUserId(10L, "U_LINE_456");
        verify(tokenMapper, never()).markUsed(1L);
    }

    @Test
    void linkByMessageRejectsAlreadyLinkedAccount() {
        UserEntity user = new UserEntity(10L, 3L, "オーナー", "owner@example.com", "hash",
                null, null, "U_EXISTING", "ACTIVE", null, null, null, null);
        when(userMapper.findByEmail("owner@example.com")).thenReturn(user);

        LineUserLinkService.LinkResult result = lineUserLinkService.linkByMessage("U_LINE_789", "owner@example.com");

        assertEquals(LineUserLinkService.LinkResult.ALREADY_LINKED, result);
        verify(userMapper, never()).saveLineUserId(10L, "U_LINE_789");
        verify(tokenMapper, never()).markUsed(1L);
    }
}
