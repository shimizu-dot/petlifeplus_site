package com.example.petlife.service.line;

import com.example.petlife.entity.LineLinkTokenEntity;
import com.example.petlife.entity.UserEntity;
import com.example.petlife.mapper.LineLinkTokenMapper;
import com.example.petlife.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class LineUserLinkService {

    private static final String LINK_COMMAND_PREFIX = "連携";
    private static final int TOKEN_EXPIRE_MINUTES   = 10;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserMapper userMapper;
    private final LineLinkTokenMapper tokenMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public LineUserLinkService(UserMapper userMapper, LineLinkTokenMapper tokenMapper) {
        this.userMapper   = userMapper;
        this.tokenMapper  = tokenMapper;
    }

    /**
     * アプリ内でユーザー用のワンタイムトークン（6桁）を生成する。
     * 既存の未使用トークンは無効化してから新規発行する。
     */
    public String generateToken(Long userId) {
        tokenMapper.invalidateByUserId(userId);
        String token = String.format("%06d", 100000 + secureRandom.nextInt(900000));
        tokenMapper.insert(new LineLinkTokenEntity(
                null, userId, token,
                LocalDateTime.now().plusMinutes(TOKEN_EXPIRE_MINUTES),
                null, null));
        return token;
    }

    /**
     * LINE メッセージを解析し、ワンタイムトークンで LINE ID と PetLife アカウントを紐付ける。
     * コマンド形式: 「連携 {6桁コード}」
     */
    public LinkResult linkByMessage(String lineUserId, String text) {
        if (lineUserId == null || lineUserId.isBlank()) return LinkResult.NO_ACTION;
        if (text == null || text.isBlank())             return LinkResult.NO_ACTION;

        String normalized = text.strip();
        if (!normalized.startsWith(LINK_COMMAND_PREFIX)) {
            if (isEmail(normalized)) {
                return linkByEmail(lineUserId, normalized);
            }
            return LinkResult.NO_ACTION;
        }

        String[] tokens = normalized.split("\\s+", 2);
        if (tokens.length < 2 || tokens[1].isBlank()) return LinkResult.INVALID_FORMAT;

        String code = tokens[1].strip();
        if (code.matches("\\d{6}")) {
            return linkByToken(lineUserId, code);
        }
        if (isEmail(code)) {
            return linkByEmail(lineUserId, code);
        }
        return LinkResult.INVALID_FORMAT;
    }

    private LinkResult linkByToken(String lineUserId, String code) {
        LineLinkTokenEntity linkToken = tokenMapper.findValidByToken(code);
        if (linkToken == null) return LinkResult.TOKEN_INVALID;

        UserEntity user = userMapper.findById(linkToken.userId());
        if (user == null) return LinkResult.USER_NOT_FOUND;

        LinkResult linkResult = saveLineUserId(user, lineUserId);
        if (linkResult != LinkResult.LINKED) return linkResult;

        tokenMapper.markUsed(linkToken.id());
        return LinkResult.LINKED;
    }

    private LinkResult linkByEmail(String lineUserId, String email) {
        UserEntity user = userMapper.findByEmail(email);
        if (user == null) return LinkResult.USER_NOT_FOUND;
        return saveLineUserId(user, lineUserId);
    }

    private LinkResult saveLineUserId(UserEntity user, String lineUserId) {
        if (user.lineUserId() != null && !user.lineUserId().isBlank()) {
            if (user.lineUserId().equals(lineUserId)) {
                return LinkResult.LINKED;
            }
            return LinkResult.ALREADY_LINKED;
        }
        int updated = userMapper.saveLineUserId(user.id(), lineUserId);
        return updated > 0 ? LinkResult.LINKED : LinkResult.USER_NOT_FOUND;
    }

    private boolean isEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }

    public enum LinkResult {
        NO_ACTION,
        INVALID_FORMAT,
        TOKEN_INVALID,
        USER_NOT_FOUND,
        ALREADY_LINKED,
        LINKED
    }
}
