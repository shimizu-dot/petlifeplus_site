package com.example.petlife.service.line;

import com.example.petlife.entity.UserEntity;
import com.example.petlife.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class LineUserLinkService {

    private static final String LINK_COMMAND_PREFIX = "連携";

    private final UserMapper userMapper;

    public LineUserLinkService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public LinkResult linkByMessage(String lineUserId, String text) {
        if (lineUserId == null || lineUserId.isBlank()) return LinkResult.NO_ACTION;
        if (text == null || text.isBlank()) return LinkResult.NO_ACTION;

        String normalized = text.strip();
        if (!normalized.startsWith(LINK_COMMAND_PREFIX)) {
            return LinkResult.NO_ACTION;
        }

        String[] tokens = normalized.split("\\s+", 2);
        if (tokens.length < 2 || tokens[1].isBlank()) {
            return LinkResult.INVALID_FORMAT;
        }

        String email = tokens[1].strip();
        UserEntity target = userMapper.findByEmail(email);
        if (target == null) {
            return LinkResult.USER_NOT_FOUND;
        }

        UserEntity existing = userMapper.findByLineUserId(lineUserId);
        if (existing != null && !existing.id().equals(target.id())) {
            return LinkResult.ALREADY_LINKED_TO_OTHER;
        }

        int updated = userMapper.saveLineUserIdByEmail(email, lineUserId);
        return updated > 0 ? LinkResult.LINKED : LinkResult.USER_NOT_FOUND;
    }

    public enum LinkResult {
        NO_ACTION,
        INVALID_FORMAT,
        USER_NOT_FOUND,
        ALREADY_LINKED_TO_OTHER,
        LINKED
    }
}
