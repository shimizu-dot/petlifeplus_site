package com.example.petlife.config;

import com.example.petlife.entity.UserEntity;
import com.example.petlife.mapper.AuthMapper;
import com.example.petlife.mapper.UserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 起動時のセーフティネット: data.sql で挿入されなかったユーザーのみ BCrypt ハッシュで作成する。
 * 通常は data.sql (spring.sql.init.mode=always) がユーザーを先に作成するためこのクラスは何もしない。
 * data.sql が無効な環境（手動 never 設定等）でも最低限のユーザーを保証する。
 */
@Component
@Order(100)
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
        insertIfAbsent(2L, "開発者アカウント", "super@petlife.local",  "super123",  "090-1455-3927", null);
        insertIfAbsent(1L, "管理アカウント",   "admin@petlife.local",  "admin123",  "090-1111-1111", null);
        insertIfAbsent(4L, "Dr.アカウント",    "vet1@petlife.local",   "vet123",    "090-4444-4444", null);
        insertIfAbsent(5L, "Staff.アカウント", "staff1@petlife.local", "staff123",  "090-5555-5555", null);
        insertIfAbsent(3L, "ライト会員",       "owner1@petlife.local", "user123",   "090-6666-0001", null);
        insertIfAbsent(3L, "スタンダード会員", "owner2@petlife.local", "user123",   "090-6666-0002", null);
        insertIfAbsent(3L, "プレミアム会員",   "owner3@petlife.local", "user123",   "090-6666-0003", null);
    }

    private void insertIfAbsent(Long roleId, String name, String email, String rawPassword, String phone, String lineUserId) {
        if (authMapper.findByEmail(email) != null) return;
        UserEntity u = new UserEntity(
                null, roleId, name, email,
                encoder.encode(rawPassword),
                phone, null, lineUserId, "ACTIVE", null, null, null, null
        );
        userMapper.insertUser(u);
    }
}

