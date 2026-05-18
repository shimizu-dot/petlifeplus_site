-- 冪等INSERT: ON CONFLICT DO NOTHING でリラン安全
INSERT INTO roles (id, role_code, role_name) VALUES
(1, 'ADMIN', '管理者'),
(2, 'USER',  '一般ユーザー'),
(3, 'VET',   '獣医師'),
(4, 'STAFF', 'スタッフ')
ON CONFLICT DO NOTHING;

-- ユーザーの password_hash は DataInitializer で BCrypt 生成される
-- 既にユーザーが存在する場合はスキップ

INSERT INTO pets (id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg)
SELECT 1, u.id, 'ポチ', 'DOG', '柴犬', 'MALE', '2021-03-01', 8.50
FROM users u WHERE u.email = 'owner1@petlifeplus.local'
  AND NOT EXISTS (SELECT 1 FROM pets WHERE id = 1);

INSERT INTO pets (id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg)
SELECT 2, u.id, 'ミケ', 'CAT', '雑種', 'FEMALE', '2022-07-12', 3.80
FROM users u WHERE u.email = 'owner1@petlifeplus.local'
  AND NOT EXISTS (SELECT 1 FROM pets WHERE id = 2);

INSERT INTO pets (id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg)
SELECT 3, u.id, 'レオ', 'DOG', 'トイプードル', 'MALE', '2020-11-23', 5.20
FROM users u WHERE u.email = 'owner2@petlifeplus.local'
  AND NOT EXISTS (SELECT 1 FROM pets WHERE id = 3);

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

INSERT INTO plans (id, name, monthly_fee, features_json, is_active)
VALUES
(1, 'Basic',   980.00,  '{"aiCheck": true, "notifications": true}'::jsonb, true),
(2, 'Premium', 1980.00, '{"aiCheck": true, "notifications": true, "prioritySupport": true}'::jsonb, true)
ON CONFLICT DO NOTHING;
