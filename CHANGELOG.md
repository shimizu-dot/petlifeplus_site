# CHANGELOG

## [2026-05-26]

### セキュリティ・バグ修正

#### #7 — SymptomCheckMapper: ソフトデリートフィルター欠落
- **ファイル:** `backend/src/main/java/com/example/petlife/mapper/SymptomCheckMapper.java`
- **変更:** `findRecentByPetId` クエリに `AND deleted_at IS NULL` を追加
- **理由:** 全テーブルがソフトデリートを採用しているが、`symptom_checks` のクエリのみフィルターが抜けており、削除済みレコードが取得されていた

#### #8 — AppointmentPageController: 権限チェック不統一
- **ファイル:** `backend/src/main/java/com/example/petlife/controller/AppointmentPageController.java`
- **変更:**
  - `approve` / `reject`: `canManageClinical()` → `hasStaffAccess()`（ADMIN を含むよう修正）
  - `cancel`: `canManageClinical()` → `hasStaffAccess()`（ADMIN のすり抜けを防止）
  - エラーメッセージを実態に合わせて修正
- **理由:** `canManageClinical()` は VET + STAFF のみで ADMIN を除外するため、承認・却下操作が ADMIN にできなかった。キャンセルエンドポイントでは逆に ADMIN がオーナーとして処理されるバグがあった

#### #9 — DashboardController: N+1 クエリ
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/mapper/HealthRecordMapper.java`
  - `backend/src/main/java/com/example/petlife/controller/DashboardController.java`
- **変更:**
  - `HealthRecordMapper` に `countByOwnerUserId` (JOIN クエリ) を追加
  - `DashboardController` のペット件数ループを `countByOwnerUserId` の 1 クエリに置換
  - 不要な `List<PetEntity>` / `java.util.List` のインポートを削除
- **理由:** ペット数分だけ `countByPetId` を繰り返し呼ぶ N+1 が発生していた

#### #10 — index.html: 外部 QR コード API 依存
- **ファイル:** `frontend/public/index.html`
- **変更:** `<img src="https://api.qrserver.com/...">` を削除し、`qrcode.js` によるブラウザ内 QR コード生成に置換
- **理由:** LINE ID がクエリパラメータとして外部サービスに送信されていた。外部障害で機能停止するリスクもあった
- **備考:** `qrcode.js` は現在 jsDelivr CDN から読み込み。完全自己ホスティングには `assets/js/qrcode.min.js` を配置して参照先を変更すること

#### #11 — admin/users/list.html: ロール ID 数値ハードコード
- **ファイル:** `backend/src/main/resources/templates/admin/users/list.html`
- **変更:** `user.roleId == 1` 等の数値比較（3 箇所）を `user.roleDisplay` 文字列比較に統一
- **理由:** ロール ID が DB の物理値に依存しており、変更に弱かった

---

#### H-1 — ADMIN がアクセス不可ページで 403 のみ返す問題
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/config/SecurityConfig.java`
  - `backend/src/main/java/com/example/petlife/controller/AccessDeniedPageController.java` *(新規)*
  - `backend/src/main/resources/templates/error/access-denied.html` *(新規)*
- **変更:** `.exceptionHandling(e -> e.accessDeniedPage("/app/access-denied"))` を追加。アクセス拒否専用ページ（コントローラー + テンプレート）を新規作成
- **背景仕様:** 管理者アカウントはシステム管理専用。診療予約・カレンダー・診療記録へのアクセス不可は設計通り。ただし生の 403 ではなく説明メッセージを表示する必要があった

#### H-2 — AppointmentService: VET/STAFF が自分の予約しか見えない
- **ファイル:** `backend/src/main/java/com/example/petlife/service/AppointmentService.java`
- **変更:** `list()` / `listForApp()` / `deleteSelected()` の `isAdmin()` → `canManageClinical()` に変更
- **背景仕様:**
  - SUPER / VET / STAFF → 全予約を表示・操作
  - USER → 自分の予約のみ
  - ADMIN → 予約機能自体にアクセス不可（H-1 で制御）
- **理由:** `isAdmin()` では VET / STAFF / SUPER が自分の予約しか参照できず、承認・却下業務が実質不可能だった

---

#### M-3 — パスワードリセット: 古いトークンが無効化されない
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/mapper/PasswordResetTokenMapper.java`
  - `backend/src/main/java/com/example/petlife/service/PasswordResetService.java`
- **変更:**
  - `PasswordResetTokenMapper` に `invalidateByUserId` を追加（同ユーザーの未使用トークンを `used_at = NOW()` でマーク）
  - `initiateReset()` で新トークン INSERT 前に `invalidateByUserId` を呼び出し
- **理由:** 連続リクエストで `password_reset_tokens` テーブルが無制限に増加し、古いリンクも有効なままになっていた

---

### リファクタリング・設計改善

#### L-1 — ReportController: サービス層なし・ロール ID ハードコード
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/mapper/UserMapper.java`
  - `backend/src/main/java/com/example/petlife/dto/report/ReportStats.java` *(新規)*
  - `backend/src/main/java/com/example/petlife/service/ReportService.java` *(新規)*
  - `backend/src/main/java/com/example/petlife/controller/ReportController.java`
  - `backend/src/main/resources/templates/reports/index.html`
- **変更:**
  - `UserMapper` に `countByRoleCode(String)` を追加（roles テーブル JOIN でロールコード文字列で集計）
  - `ReportStats` record（全統計値を保持する DTO）を新規作成
  - `ReportService` を新規作成し、5 マッパーの注入と集計ロジックを集約
  - `ReportController` を `ReportService` 1本のみ注入する薄いコントローラーに変更
  - テンプレートの参照を `${userCount}` → `${stats.userCount}` 等に統一
- **理由:** `countByRoleId(1L)` 等の数値がハードコードされており、ビジネスロジックがコントローラーに混在していた

#### L-2 — LoginUser: @Deprecated メソッド残存
- **ファイル:** `backend/src/main/java/com/example/petlife/config/LoginUser.java`
- **変更:** 使用箇所ゼロの `canManagePets()` を削除
- **理由:** 廃止メソッドが残存し、どのメソッドを使うべきか混乱を招いていた

#### L-5 — AppointmentService.deleteSelected(): N+1 削除
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/mapper/AppointmentMapper.java`
  - `backend/src/main/java/com/example/petlife/service/AppointmentService.java`
- **変更:**
  - `AppointmentMapper` に `softDeleteByIds(List<Long>, LocalDateTime)` を追加（MyBatis `<foreach>` で `WHERE id IN (...)` の一括 UPDATE）
  - `deleteSelected()` の個別 `softDelete` ループを `softDeleteByIds` の 1 呼び出しに置換
- **理由:** 削除件数分だけ UPDATE を個別発行する N+1 が発生していた

#### L-4 — payments テーブル: deleted_at / updated_at 欠落
- **ファイル:**
  - `backend/src/main/resources/schema.sql`
  - `backend/src/main/java/com/example/petlife/entity/PaymentEntity.java`
  - `backend/src/main/java/com/example/petlife/mapper/PaymentMapper.java`
- **変更:**
  - `schema.sql` の `payments` テーブルに `deleted_at TIMESTAMP` と `updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` を追加
  - `PaymentEntity` record に `deletedAt` / `updatedAt` フィールドを追加
  - `PaymentMapper` の全 SELECT に `deleted_at IS NULL` フィルターと新カラムを追加
  - `updateStatus` の `WHERE` 句に `AND deleted_at IS NULL` を追加（旧実装は `updated_at` カラム不存在のため実行時エラー）
  - `softDelete` メソッドを新規追加
- **理由:** 他の全テーブルがソフトデリートを採用しているが `payments` のみ `deleted_at` が欠落。さらに `updated_at` も存在しないにもかかわらず `updateStatus` が参照しており、実行時エラーになる潜在バグがあった
- **備考:** `CREATE TABLE IF NOT EXISTS` では既存 DB にカラムは追加されない。既存 DB には以下を手動実行すること:
  ```sql
  ALTER TABLE payments ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
  ALTER TABLE payments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
  ```

#### L-3 — AppointmentSlotController: サービス層なし
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/service/AppointmentSlotService.java` *(新規)*
  - `backend/src/main/java/com/example/petlife/controller/AppointmentSlotController.java`
- **変更:**
  - `AppointmentSlotService` を新規作成（`list` / `create` / `delete` + `ensureAccess` を集約）
  - `delete` に存在確認（`findById`）を追加（旧実装で欠落）
  - コントローラーは `AppointmentSlotService` 1本のみ注入し、`BadRequestException` をキャッチして `FlashAttribute` に変換するだけ
- **理由:** マッパーを直接注入し、バリデーション・認可チェックがコントローラーに混在していた
