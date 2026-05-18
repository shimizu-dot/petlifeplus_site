package com.example.petlife.config;

import com.example.petlife.entity.UserEntity;
import com.example.petlife.mapper.AuthMapper;
import com.example.petlife.mapper.UserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 初期ユーザーを BCrypt ハッシュで作成する。
 * デフォルト認証情報:
 *   管理者: admin@petlifeplus.local / admin123
 *   一般:   owner1@petlifeplus.local / user123
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final AuthMapper authMapper;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder encoder;

    public DataInitializer(AuthMapper authMapper, UserMapper userMapper, BCryptPasswordEncoder encoder) {
        this.authMapper = authMapper;
        this.userMapper = userMapper;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        upsertUser(1L, "管理 太郎",  "admin@petlifeplus.local",  "admin123", "090-1111-1111");
        upsertUser(2L, "飼主 花子",  "owner1@petlifeplus.local", "user123",  "090-2222-2222");
        upsertUser(2L, "飼主 次郎",  "owner2@petlifeplus.local", "user123",  "090-3333-3333");
        upsertUser(3L, "獣医 三郎",  "vet1@petlifeplus.local",   "vet123",   "090-4444-4444");
        upsertUser(4L, "受付 四郎",  "staff1@petlifeplus.local", "staff123", "090-5555-5555");
    }

    private void upsertUser(Long roleId, String name, String email, String rawPassword, String phone) {
        if (authMapper.findByEmail(email) == null) {
            UserEntity u = new UserEntity(
                    null, roleId, name, email,
                    encoder.encode(rawPassword),
                    phone, "ACTIVE", null, null, null, null
            );
            userMapper.insertReturningId(u);
        }
    }
}
