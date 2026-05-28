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
    }
}
