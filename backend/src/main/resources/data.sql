-- PetLifePlus seed data  (idempotent — ON CONFLICT / NOT EXISTS でリラン安全)
-- 開発者アカウント (h4mizoo@gmail.com) はここで管理。その他のユーザーは DataInitializer (BCrypt) で作成。
-- 実行: psql -U postgres -d petlifeplus -f data.sql

-- ─── Roles ───────────────────────────────────────────────────────────────────

INSERT INTO roles (id, role_code, role_name) VALUES
(1, 'ADMIN', '管理者'),
(2, 'SUPER', '開発者'),
(3, 'USER',  '一般ユーザー'),
(4, 'VET',   '獣医師'),
(5, 'STAFF', 'スタッフ')
ON CONFLICT (id) DO UPDATE
    SET role_code = EXCLUDED.role_code,
        role_name = EXCLUDED.role_name;

-- ─── Developer account ───────────────────────────────────────────────────────
-- h4mizoo@gmail.com / hs1015 / SUPER — ON CONFLICT DO NOTHING でパスワードを上書きしない

INSERT INTO users (role_id, name, email, password_hash, phone, line_user_id, status)
SELECT r.id,
       '開発者',
       'h4mizoo@gmail.com',
       '$2a$10$0nDUs6dnw0HXsF0BdTLxfuj2gocg.3QznPuWxhYJgW59RX9zY4XfW',
       '090-1455-3927',
       'nz-1015',
       'ACTIVE'
FROM roles r WHERE r.role_code = 'SUPER'
ON CONFLICT (email) DO NOTHING;

-- ─── Plans ───────────────────────────────────────────────────────────────────

INSERT INTO plans (id, name, monthly_fee, features_json, is_active) VALUES
(1, 'LIGHT',    980.00,  '{"healthRecord":true,"basicNotification":true,"aiSymptomCheck":false,"slackBot":false,"lineBot":false,"zoomConsult":false}'::jsonb, true),
(2, 'STANDARD', 1980.00, '{"healthRecord":true,"basicNotification":true,"aiSymptomCheck":true,"slackBot":true,"lineBot":true,"zoomConsult":false}'::jsonb, true),
(3, 'PREMIUM',  2980.00, '{"healthRecord":true,"basicNotification":true,"aiSymptomCheck":true,"slackBot":true,"lineBot":true,"zoomConsult":true}'::jsonb, true)
ON CONFLICT (id) DO UPDATE
    SET name          = EXCLUDED.name,
        monthly_fee   = EXCLUDED.monthly_fee,
        features_json = EXCLUDED.features_json,
        is_active     = EXCLUDED.is_active,
        updated_at    = CURRENT_TIMESTAMP;

-- ─── Plan features ────────────────────────────────────────────────────────────
-- LIGHT  (1): 基本機能のみ
-- STANDARD (2): AI症状チェック・Slack・LINE
-- PREMIUM (3): STANDARD 全機能 + Zoom オンライン診療

INSERT INTO plan_features (plan_id, feature_code) VALUES
(2, 'AI_SYMPTOM'),
(2, 'SLACK_BOT'),
(2, 'LINE_BOT'),
(3, 'AI_SYMPTOM'),
(3, 'SLACK_BOT'),
(3, 'LINE_BOT'),
(3, 'ZOOM_CONSULT')
ON CONFLICT DO NOTHING;

-- ─── Pets ────────────────────────────────────────────────────────────────────
-- owner1@petlife.local

INSERT INTO pets (id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg)
SELECT 1, u.id, 'ポチ', 'DOG', '雑種', 'MALE', '2021-03-01', 8.50
FROM users u WHERE u.email = 'owner1@petlife.local'
  AND NOT EXISTS (SELECT 1 FROM pets WHERE id = 1);

INSERT INTO pets (id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg)
SELECT 2, u.id, 'タロウ', 'DOG', '柴犬', 'FEMALE', '2022-07-12', 3.80
FROM users u WHERE u.email = 'owner1@petlife.local'
  AND NOT EXISTS (SELECT 1 FROM pets WHERE id = 2);

-- owner2@petlife.local

INSERT INTO pets (id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg)
SELECT 3, u.id, 'レオン', 'DOG', 'ノーフォークテリア', 'MALE', '2020-11-23', 5.20
FROM users u WHERE u.email = 'owner2@petlife.local'
  AND NOT EXISTS (SELECT 1 FROM pets WHERE id = 3);

-- プラン別テストアカウント用

INSERT INTO pets (id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg)
SELECT 101, u.id, 'ピーコ', 'DOG', 'チワワ', 'FEMALE', '2022-01-01', 7.40
FROM users u WHERE u.email = 'owner1@petlife.local'
  AND NOT EXISTS (SELECT 1 FROM pets p WHERE p.id = 101 OR (p.owner_user_id = u.id AND p.name = 'ピーコ'));

INSERT INTO pets (id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg)
SELECT 102, u.id, 'カレン', 'DOG', 'ポメプー', 'FEMALE', '2021-06-10', 4.10
FROM users u WHERE u.email = 'owner2@petlife.local'
  AND NOT EXISTS (SELECT 1 FROM pets p WHERE p.id = 102 OR (p.owner_user_id = u.id AND p.name = 'カレン'));

INSERT INTO pets (id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg)
SELECT 103, u.id, 'ボス', 'DOG', 'パグ', 'MALE', '2020-04-20', 5.60
FROM users u WHERE u.email = 'owner3@petlife.local'
  AND NOT EXISTS (SELECT 1 FROM pets p WHERE p.id = 103 OR (p.owner_user_id = u.id AND p.name = 'ボス'));

-- ─── Health records ──────────────────────────────────────────────────────────

INSERT INTO health_records (id, pet_id, recorded_by_user_id, record_date, weight_kg, meal_memo, exercise_minutes, note)
SELECT 1, 1, u.id, '2026-05-10', 8.60, '食欲良好', 30, '特記事項なし'
FROM users u WHERE u.email = 'owner1@petlife.local'
  AND NOT EXISTS (SELECT 1 FROM health_records WHERE id = 1);

INSERT INTO health_records (id, pet_id, recorded_by_user_id, record_date, weight_kg, meal_memo, exercise_minutes, note)
SELECT 2, 2, u.id, '2026-05-10', 3.75, '少し食欲低下', 15, '様子見'
FROM users u WHERE u.email = 'owner1@petlife.local'
  AND NOT EXISTS (SELECT 1 FROM health_records WHERE id = 2);

INSERT INTO health_records (id, pet_id, recorded_by_user_id, record_date, weight_kg, meal_memo, exercise_minutes, note)
SELECT 3, 3, u.id, '2026-05-11', 5.25, '通常', 25, '元気'
FROM users u WHERE u.email = 'owner2@petlife.local'
  AND NOT EXISTS (SELECT 1 FROM health_records WHERE id = 3);

-- ─── Pet care records ────────────────────────────────────────────────────────

INSERT INTO pet_care_records (id, pet_id, recorded_by_user_id, care_type, administered_on, next_due_on, memo)
SELECT 1, 1, u.id, 'RABIES', '2025-08-01', '2026-08-01', '狂犬病予防接種'
FROM users u WHERE u.email = 'owner1@petlife.local'
  AND NOT EXISTS (SELECT 1 FROM pet_care_records WHERE id = 1);

INSERT INTO pet_care_records (id, pet_id, recorded_by_user_id, care_type, administered_on, next_due_on, memo)
SELECT 2, 1, u.id, 'HEARTWORM', '2025-07-15', '2026-07-15', 'フィラリア予防薬'
FROM users u WHERE u.email = 'owner1@petlife.local'
  AND NOT EXISTS (SELECT 1 FROM pet_care_records WHERE id = 2);

INSERT INTO pet_care_records (id, pet_id, recorded_by_user_id, care_type, administered_on, next_due_on, memo)
SELECT 3, 1, u.id, 'COMBO_VACCINE', '2025-09-01', '2026-09-01', '混合ワクチン接種'
FROM users u WHERE u.email = 'owner1@petlife.local'
  AND NOT EXISTS (SELECT 1 FROM pet_care_records WHERE id = 3);

-- ─── Subscriptions ───────────────────────────────────────────────────────────
-- owner2 → STANDARD（診療予約・AI症状チェック使用可）

INSERT INTO subscriptions (user_id, pet_id, plan_id, start_date, status, auto_renew)
SELECT u.id, p.id, 2, CURRENT_DATE - INTERVAL '30 days', 'ACTIVE', true
FROM users u
JOIN pets p ON p.owner_user_id = u.id AND p.name = 'レオン' AND p.deleted_at IS NULL
WHERE u.email = 'owner2@petlife.local'
  AND NOT EXISTS (
      SELECT 1 FROM subscriptions s WHERE s.user_id = u.id AND s.pet_id = p.id AND s.plan_id = 2 AND s.status = 'ACTIVE' AND s.deleted_at IS NULL
  );

-- owner1 (Light プランテスト) → LIGHT

INSERT INTO subscriptions (user_id, pet_id, plan_id, start_date, status, auto_renew)
SELECT u.id, p.id, 1, CURRENT_DATE - INTERVAL '30 days', 'ACTIVE', true
FROM users u
JOIN pets p ON p.owner_user_id = u.id AND p.name = 'ピーコ' AND p.deleted_at IS NULL
WHERE u.email = 'owner1@petlife.local'
  AND NOT EXISTS (
      SELECT 1 FROM subscriptions s WHERE s.user_id = u.id AND s.pet_id = p.id AND s.plan_id = 1 AND s.status = 'ACTIVE' AND s.deleted_at IS NULL
  );

-- owner2 (Standard プランテスト) → STANDARD

INSERT INTO subscriptions (user_id, pet_id, plan_id, start_date, status, auto_renew)
SELECT u.id, p.id, 2, CURRENT_DATE - INTERVAL '30 days', 'ACTIVE', true
FROM users u
JOIN pets p ON p.owner_user_id = u.id AND p.name = 'カレン' AND p.deleted_at IS NULL
WHERE u.email = 'owner2@petlife.local'
  AND NOT EXISTS (
      SELECT 1 FROM subscriptions s WHERE s.user_id = u.id AND s.pet_id = p.id AND s.plan_id = 2 AND s.status = 'ACTIVE' AND s.deleted_at IS NULL
  );

-- owner3 (Premium プランテスト) → PREMIUM

INSERT INTO subscriptions (user_id, pet_id, plan_id, start_date, status, auto_renew)
SELECT u.id, p.id, 3, CURRENT_DATE - INTERVAL '30 days', 'ACTIVE', true
FROM users u
JOIN pets p ON p.owner_user_id = u.id AND p.name = 'ボス' AND p.deleted_at IS NULL
WHERE u.email = 'owner3@petlife.local'
  AND NOT EXISTS (
      SELECT 1 FROM subscriptions s WHERE s.user_id = u.id AND s.pet_id = p.id AND s.plan_id = 3 AND s.status = 'ACTIVE' AND s.deleted_at IS NULL
  );

-- ─── Sequence fixes ──────────────────────────────────────────────────────────
-- 明示 ID を使った INSERT 後はシーケンスをリセット

SELECT setval(pg_get_serial_sequence('roles',            'id'), COALESCE((SELECT MAX(id) FROM roles),            1), true);
SELECT setval(pg_get_serial_sequence('pets',             'id'), COALESCE((SELECT MAX(id) FROM pets),             1), true);
SELECT setval(pg_get_serial_sequence('health_records',   'id'), COALESCE((SELECT MAX(id) FROM health_records),   1), true);
SELECT setval(pg_get_serial_sequence('pet_care_records', 'id'), COALESCE((SELECT MAX(id) FROM pet_care_records), 1), true);
SELECT setval(pg_get_serial_sequence('plans',            'id'), COALESCE((SELECT MAX(id) FROM plans),            1), true);
SELECT setval(pg_get_serial_sequence('subscriptions',    'id'), COALESCE((SELECT MAX(id) FROM subscriptions),    1), true);
