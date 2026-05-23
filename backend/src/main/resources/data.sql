-- PetLifePlus seed data  (idempotent — ON CONFLICT / NOT EXISTS でリラン安全)
-- ユーザーは DataInitializer (BCrypt) で作成されるためここでは INSERT しません。
-- 実行: psql -U postgres -d petlifeplus -f data.sql

-- ─── Roles ───────────────────────────────────────────────────────────────────

INSERT INTO roles (id, role_code, role_name) VALUES
(1, 'ADMIN', '管理者'),
(2, 'USER',  '一般ユーザー'),
(3, 'VET',   '獣医師'),
(4, 'STAFF', 'スタッフ')
ON CONFLICT DO NOTHING;

-- ─── Plans ───────────────────────────────────────────────────────────────────

INSERT INTO plans (id, name, monthly_fee, features_json, is_active) VALUES
(1, 'LIGHT',    980.00,  '{"healthRecord":true,"basicNotification":true}'::jsonb, true),
(2, 'STANDARD', 1980.00, '{"healthRecord":true,"basicNotification":true,"aiSymptomCheck":true,"consultationLead":true}'::jsonb, true),
(3, 'PREMIUM',  2980.00, '{"healthRecord":true,"basicNotification":true,"aiSymptomCheck":true,"consultationLead":true,"prioritySupport":true}'::jsonb, true)
ON CONFLICT (id) DO UPDATE
    SET name          = EXCLUDED.name,
        monthly_fee   = EXCLUDED.monthly_fee,
        features_json = EXCLUDED.features_json,
        is_active     = EXCLUDED.is_active,
        updated_at    = CURRENT_TIMESTAMP;

-- ─── Pets ────────────────────────────────────────────────────────────────────
-- owner1@petlifeplus.local

INSERT INTO pets (id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg)
SELECT 1, u.id, 'ポチ', 'DOG', '柴犬', 'MALE', '2021-03-01', 8.50
FROM users u WHERE u.email = 'owner1@petlifeplus.local'
  AND NOT EXISTS (SELECT 1 FROM pets WHERE id = 1);

INSERT INTO pets (id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg)
SELECT 2, u.id, 'ミケ', 'CAT', '雑種', 'FEMALE', '2022-07-12', 3.80
FROM users u WHERE u.email = 'owner1@petlifeplus.local'
  AND NOT EXISTS (SELECT 1 FROM pets WHERE id = 2);

-- owner2@petlifeplus.local

INSERT INTO pets (id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg)
SELECT 3, u.id, 'レオ', 'DOG', 'トイプードル', 'MALE', '2020-11-23', 5.20
FROM users u WHERE u.email = 'owner2@petlifeplus.local'
  AND NOT EXISTS (SELECT 1 FROM pets WHERE id = 3);

-- プラン別テストアカウント用

INSERT INTO pets (id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg)
SELECT 101, u.id, 'ライト犬', 'DOG', '柴犬', 'MALE', '2022-01-01', 7.40
FROM users u WHERE u.email = 'owner.light@petlifeplus.local'
  AND NOT EXISTS (SELECT 1 FROM pets p WHERE p.id = 101 OR (p.owner_user_id = u.id AND p.name = 'ライト犬'));

INSERT INTO pets (id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg)
SELECT 102, u.id, '標準猫', 'CAT', '雑種', 'FEMALE', '2021-06-10', 4.10
FROM users u WHERE u.email = 'owner.standard@petlifeplus.local'
  AND NOT EXISTS (SELECT 1 FROM pets p WHERE p.id = 102 OR (p.owner_user_id = u.id AND p.name = '標準猫'));

INSERT INTO pets (id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg)
SELECT 103, u.id, '上位プー', 'DOG', 'トイプードル', 'MALE', '2020-04-20', 5.60
FROM users u WHERE u.email = 'owner.premium@petlifeplus.local'
  AND NOT EXISTS (SELECT 1 FROM pets p WHERE p.id = 103 OR (p.owner_user_id = u.id AND p.name = '上位プー'));

-- ─── Health records ──────────────────────────────────────────────────────────

INSERT INTO health_records (id, pet_id, recorded_by_user_id, record_date, weight_kg, meal_memo, exercise_minutes, note)
SELECT 1, 1, u.id, '2026-05-10', 8.60, '食欲良好', 30, '特記事項なし'
FROM users u WHERE u.email = 'owner1@petlifeplus.local'
  AND NOT EXISTS (SELECT 1 FROM health_records WHERE id = 1);

INSERT INTO health_records (id, pet_id, recorded_by_user_id, record_date, weight_kg, meal_memo, exercise_minutes, note)
SELECT 2, 2, u.id, '2026-05-10', 3.75, '少し食欲低下', 15, '様子見'
FROM users u WHERE u.email = 'owner1@petlifeplus.local'
  AND NOT EXISTS (SELECT 1 FROM health_records WHERE id = 2);

INSERT INTO health_records (id, pet_id, recorded_by_user_id, record_date, weight_kg, meal_memo, exercise_minutes, note)
SELECT 3, 3, u.id, '2026-05-11', 5.25, '通常', 25, '元気'
FROM users u WHERE u.email = 'owner2@petlifeplus.local'
  AND NOT EXISTS (SELECT 1 FROM health_records WHERE id = 3);

-- ─── Pet care records ────────────────────────────────────────────────────────

INSERT INTO pet_care_records (id, pet_id, recorded_by_user_id, care_type, administered_on, next_due_on, memo)
SELECT 1, 1, u.id, 'RABIES', '2025-08-01', '2026-08-01', '狂犬病予防接種'
FROM users u WHERE u.email = 'owner1@petlifeplus.local'
  AND NOT EXISTS (SELECT 1 FROM pet_care_records WHERE id = 1);

INSERT INTO pet_care_records (id, pet_id, recorded_by_user_id, care_type, administered_on, next_due_on, memo)
SELECT 2, 1, u.id, 'HEARTWORM', '2025-07-15', '2026-07-15', 'フィラリア予防薬'
FROM users u WHERE u.email = 'owner1@petlifeplus.local'
  AND NOT EXISTS (SELECT 1 FROM pet_care_records WHERE id = 2);

INSERT INTO pet_care_records (id, pet_id, recorded_by_user_id, care_type, administered_on, next_due_on, memo)
SELECT 3, 1, u.id, 'COMBO_VACCINE', '2025-09-01', '2026-09-01', '混合ワクチン接種'
FROM users u WHERE u.email = 'owner1@petlifeplus.local'
  AND NOT EXISTS (SELECT 1 FROM pet_care_records WHERE id = 3);

-- ─── Subscriptions ───────────────────────────────────────────────────────────
-- owner1 / owner2 → STANDARD（診療予約・AI症状チェック使用可）

INSERT INTO subscriptions (user_id, pet_id, plan_id, start_date, status, auto_renew)
SELECT u.id, p.id, 2, CURRENT_DATE - INTERVAL '30 days', 'ACTIVE', true
FROM users u
JOIN pets p ON p.owner_user_id = u.id AND p.name = 'ポチ' AND p.deleted_at IS NULL
WHERE u.email = 'owner1@petlifeplus.local'
  AND NOT EXISTS (
      SELECT 1 FROM subscriptions s WHERE s.user_id = u.id AND s.status = 'ACTIVE' AND s.deleted_at IS NULL
  );

INSERT INTO subscriptions (user_id, pet_id, plan_id, start_date, status, auto_renew)
SELECT u.id, p.id, 2, CURRENT_DATE - INTERVAL '30 days', 'ACTIVE', true
FROM users u
JOIN pets p ON p.owner_user_id = u.id AND p.name = 'レオ' AND p.deleted_at IS NULL
WHERE u.email = 'owner2@petlifeplus.local'
  AND NOT EXISTS (
      SELECT 1 FROM subscriptions s WHERE s.user_id = u.id AND s.status = 'ACTIVE' AND s.deleted_at IS NULL
  );

-- owner.light → LIGHT

INSERT INTO subscriptions (user_id, pet_id, plan_id, start_date, status, auto_renew)
SELECT u.id, p.id, 1, CURRENT_DATE - INTERVAL '30 days', 'ACTIVE', true
FROM users u
JOIN pets p ON p.owner_user_id = u.id AND p.name = 'ライト犬' AND p.deleted_at IS NULL
WHERE u.email = 'owner.light@petlifeplus.local'
  AND NOT EXISTS (
      SELECT 1 FROM subscriptions s WHERE s.user_id = u.id AND s.pet_id = p.id AND s.plan_id = 1 AND s.status = 'ACTIVE' AND s.deleted_at IS NULL
  );

-- owner.standard → STANDARD

INSERT INTO subscriptions (user_id, pet_id, plan_id, start_date, status, auto_renew)
SELECT u.id, p.id, 2, CURRENT_DATE - INTERVAL '30 days', 'ACTIVE', true
FROM users u
JOIN pets p ON p.owner_user_id = u.id AND p.name = '標準猫' AND p.deleted_at IS NULL
WHERE u.email = 'owner.standard@petlifeplus.local'
  AND NOT EXISTS (
      SELECT 1 FROM subscriptions s WHERE s.user_id = u.id AND s.pet_id = p.id AND s.plan_id = 2 AND s.status = 'ACTIVE' AND s.deleted_at IS NULL
  );

-- owner.premium → PREMIUM

INSERT INTO subscriptions (user_id, pet_id, plan_id, start_date, status, auto_renew)
SELECT u.id, p.id, 3, CURRENT_DATE - INTERVAL '30 days', 'ACTIVE', true
FROM users u
JOIN pets p ON p.owner_user_id = u.id AND p.name = '上位プー' AND p.deleted_at IS NULL
WHERE u.email = 'owner.premium@petlifeplus.local'
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
