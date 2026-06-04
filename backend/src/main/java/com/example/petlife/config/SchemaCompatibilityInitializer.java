package com.example.petlife.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 旧スキーマ互換対応:
 * 既存環境の users テーブルに不足カラムがある場合、自動で追加する。
 */
@Component
@Order(0)
public class SchemaCompatibilityInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaCompatibilityInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS slack_user_id VARCHAR(100)");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS line_user_id VARCHAR(100)");
        jdbcTemplate.execute("ALTER TABLE appointment_slots ADD COLUMN IF NOT EXISTS is_blocked BOOLEAN NOT NULL DEFAULT FALSE");
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS appointment_business_hours (
                id                 BIGINT PRIMARY KEY,
                business_start     TIME      NOT NULL,
                business_end       TIME      NOT NULL,
                slot_minutes       INTEGER   NOT NULL,
                updated_by_user_id BIGINT    REFERENCES users(id),
                created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """);
        jdbcTemplate.execute("""
            INSERT INTO appointment_business_hours (id, business_start, business_end, slot_minutes)
            VALUES (1, TIME '09:30', TIME '17:00', 30)
            ON CONFLICT (id) DO NOTHING
            """);
        jdbcTemplate.execute("DROP INDEX IF EXISTS idx_subscriptions_pet_status");
        jdbcTemplate.execute("ALTER TABLE subscriptions DROP COLUMN IF EXISTS pet_id");

        // Subscription is owner-based: keep only one ACTIVE row per user (latest id).
        jdbcTemplate.execute("""
            WITH ranked AS (
                SELECT id,
                       ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY id DESC) AS rn
                FROM subscriptions
                WHERE deleted_at IS NULL AND status = 'ACTIVE'
            )
            UPDATE subscriptions s
            SET status = 'CANCELED',
                end_date = COALESCE(s.end_date, CURRENT_DATE),
                updated_at = CURRENT_TIMESTAMP
            FROM ranked r
            WHERE s.id = r.id
              AND r.rn > 1
            """);

        jdbcTemplate.execute("""
            CREATE UNIQUE INDEX IF NOT EXISTS uq_subscriptions_active_user
            ON subscriptions(user_id)
            WHERE deleted_at IS NULL AND status = 'ACTIVE'
            """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS line_link_tokens (
                id         BIGSERIAL    PRIMARY KEY,
                user_id    BIGINT       NOT NULL REFERENCES users(id),
                token      VARCHAR(6)   NOT NULL UNIQUE,
                expires_at TIMESTAMP    NOT NULL,
                used_at    TIMESTAMP,
                created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """);
    }
}
