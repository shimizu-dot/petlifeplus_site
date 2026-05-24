package com.example.petlife.service;

import com.example.petlife.entity.AnnouncementEntity;
import com.example.petlife.mapper.AnnouncementMapper;
import com.example.petlife.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnnouncementService {

    private static final String COMMAND_KEYWORD = "お知らせ";

    private final AnnouncementMapper announcementMapper;
    private final UserMapper userMapper;

    public AnnouncementService(AnnouncementMapper announcementMapper, UserMapper userMapper) {
        this.announcementMapper = announcementMapper;
        this.userMapper = userMapper;
    }

    public List<AnnouncementEntity> findActive() {
        return announcementMapper.findActive();
    }

    /** 管理者Webフォームからの登録 */
    public void create(String title, String body, Long createdByUserId) {
        AnnouncementEntity row = new AnnouncementEntity(null, title.strip(), body.strip(), true,
                createdByUserId, null, null);
        announcementMapper.insertReturningId(row);
    }

    public void updateIsActive(Long id, boolean isActive) {
        announcementMapper.updateIsActive(id, isActive);
    }

    public void delete(Long id) {
        announcementMapper.deleteById(id);
    }

    public List<AnnouncementEntity> findAll() {
        return announcementMapper.findAll();
    }

    /**
     * Slack / LINE Bot からのお知らせ登録。
     * メッセージ形式:
     *   1行目: "お知らせ"
     *   2行目: タイトル
     *   3行目以降: 本文
     * 形式が一致すれば登録して true を返す。一致しなければ false。
     */
    public boolean tryCreateFromBot(String text) {
        if (text == null) return false;
        String[] lines = text.strip().split("\n", -1);
        if (lines.length < 3) return false;
        if (!COMMAND_KEYWORD.equals(lines[0].strip())) return false;

        String title = lines[1].strip();
        String body  = String.join("\n", java.util.Arrays.copyOfRange(lines, 2, lines.length)).strip();
        if (title.isBlank() || body.isBlank()) return false;

        List<Long> adminIds = userMapper.findAdminUserIds();
        if (adminIds.isEmpty()) return false;

        AnnouncementEntity row = new AnnouncementEntity(null, title, body, true, adminIds.get(0), null, null);
        announcementMapper.insertReturningId(row);
        return true;
    }

    public static String usageMessage() {
        return "お知らせを投稿するには以下の形式で送信してください:\nお知らせ\n（タイトル）\n（本文）";
    }
}
