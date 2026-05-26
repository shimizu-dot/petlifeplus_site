# CHANGELOG

## [2026-05-27]

### 機能追加

#### F-1 — LINE プッシュメッセージ送信機能
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/service/line/LineBotService.java`
  - `backend/src/main/java/com/example/petlife/controller/line/LinePushController.java` *(新規)*
  - `backend/src/main/java/com/example/petlife/mapper/UserMapper.java`
  - `backend/src/main/resources/templates/admin/line-push.html` *(新規)*
  - `backend/src/main/resources/templates/fragments/nav.html`
- **変更:**
  - `LineBotService` に `pushMessage()`（個別送信）・`multicastMessage()`（最大500人ずつ一斉送信）を追加
  - `UserMapper` に `findAllWithLineId()`（LINE ID 登録済みアクティブユーザー一覧）・`saveLineUserId()` を追加
  - `LinePushController` を新規作成（`GET/POST /app/admin/line/push`）
  - 管理画面テンプレート `admin/line-push.html` を新規作成（登録人数表示・送信フォーム・使い方ガイド）
  - サイドバーの運営管理メニューに「💬 LINE 一斉送信」リンクを追加
- **動作:** ADMIN が `/app/admin/line/push` から任意のメッセージを入力すると、`line_user_id` が設定されている全アクティブユーザーに LINE メッセージが送信される。500名超は自動分割送信

#### F-2 — LINE follow イベント: 友達追加時ウェルカムメッセージ
- **ファイル:** `backend/src/main/java/com/example/petlife/controller/line/LineEventController.java`
- **変更:** `follow` イベントタイプを処理するブランチを追加。友達追加時にウェルカムメッセージを自動返信
- **補足:** `message` イベントのみ処理していた既存ロジックを `eventType` 分岐に整理

#### F-3 — 相談チャットボット: OpenAI マルチターン対話対応
- **ファイル:** `backend/src/main/java/com/example/petlife/service/ConsultChatService.java`
- **変更:**
  - OpenAI API 連携を追加（`@Value` で `openai.*` 設定を注入、`RestTemplate` で呼び出し）
  - `generateReply()` を会話履歴付き OpenAI 呼び出し → フォールバックの構成に刷新
  - OpenAI システムプロンプト: 情報収集フェーズでは毎回トリアージを出さず、情報が揃った段階で初めて判定するよう指示
  - フォールバック（APIキー未設定時）: ターン数を計測し、症状→時期→頻度→全身状態の順に自然な一問一答で収集後、初めてトリアージ判定を提示するロジックに変更
  - キーワードセットをクラス定数（`SYMPTOM_WORDS` 等）に整理
- **原因:** 毎ターン「受け取った内容を一次トリアージしました」という同一テンプレートを返しており、会話が進展しなかった

#### F-4 — 相談チャットボット: 食欲不振 2 日以上で即時受診ルール
- **ファイル:** `backend/src/main/java/com/example/petlife/service/ConsultChatService.java`
- **変更:**
  - `APPETITE_LOSS_WORDS`（食べない・食欲がない・食欲不振など）と `TWO_OR_MORE_DAYS_WORDS`（2日・ふつか・3日間・48時間など）の定数セットを追加
  - フォールバック: 両セットが会話履歴内で揃った場合、緊急ワードと同等の即時受診メッセージを返す
  - OpenAI システムプロンプト: 食欲不振 2 日以上 → 「今すぐ受診」強く推奨のルールを追記

#### F-5 — ユーザー管理: LINE / Slack / Zoom 連携ステータスアイコン表示
- **ファイル:**
  - `backend/src/main/java/com/example/petlife/service/PlanAccessService.java`
  - `backend/src/main/java/com/example/petlife/dto/user/UserResponse.java`
  - `backend/src/main/java/com/example/petlife/service/UserService.java`
  - `backend/src/main/java/com/example/petlife/controller/UserController.java`
  - `backend/src/main/resources/static/css/app.css`
  - `backend/src/main/resources/templates/admin/users/list.html`
  - `backend/src/main/resources/templates/admin/users/form.html`
- **変更:**
  - `PlanAccessService` に `resolveIntegrationStatusForUser(userId, roleId, slackUserId, lineUserId)` を追加。ロール 3（一般ユーザー）はプラン購読から機能可否を判定し、スタッフ系ロール（ADMIN/VET/STAFF）は全機能有効扱い
  - `UserResponse` に `integrationStatus` フィールドを追加
  - `UserService.toResponse()` で `planAccessService.resolveIntegrationStatusForUser()` を呼び出し、レスポンスに含める
  - `UserController.editForm` に `integrationStatus` モデル属性を追加
  - CSS に `.int-chip` / `.int-chip-line` / `.int-chip-slack` / `.int-chip-zoom` / `.int-chip-off` スタイルを追加
  - ユーザー一覧（table/list/grid 全ビュー）に連携チップを表示。table ビューは既存の Slack/LINE ID 生テキスト列を「連携」1列に集約
  - ユーザー編集フォームに連携ステータス欄を追加（Slack/LINE ID 入力欄の直下）
- **表示ロジック:**
  - LINE・Slack: プランに機能が含まれ、かつ該当 ID が登録済み → 色付き / それ以外（プラン外 or ID 未登録）→ グレー
  - Zoom: プランに機能が含まれる → 色付き / 未対応プラン → グレー
  - ホバーで「ID未登録」「プラン対象外」などのツールチップを表示

---

### バグ修正

#### B-1 — pets/list.html: canManagePets() 呼び出しによるレンダリングエラー
- **ファイル:** `backend/src/main/resources/templates/pets/list.html`
- **変更:** `currentUser.canManagePets()` → `currentUser.hasStaffAccess()` に4箇所置換（リストビュー・グリッドビュー・テーブルヘッダー・テーブルセル）
- **原因:** CHANGELOG L-2（2026-05-26）で `LoginUser.canManagePets()` を「使用箇所ゼロ」として削除したが、テンプレートの4箇所に残存。Thymeleaf がメソッド未定義で 500 エラーを返し、ユーザー系アカウントがペット一覧を表示・操作できなくなっていた
- **補足:** `canManagePets()` の意味（ADMIN/VET/STAFF のみサービスレベルバッジを表示）は `hasStaffAccess()` で完全に代替可能

#### B-2 — ダッシュボードテーマ: 管理系とユーザー系の色が区別しにくい
- **ファイル:** `backend/src/main/resources/static/css/app.css`
- **変更:**
  - `body.role-admin .sidebar`: 青グラデーション（`#1D4ED8 → #1E3A8A`）を明示
  - `body.role-user`: teal（`#0B8585`）→ **緑系**（`#16A34A` エメラルドグリーン）に変更
  - `body.role-user .sidebar`: 緑グラデーション（`#16A34A → #15803D`）
  - `body.role-user .btn-primary`: 緑グラデーション（`#22C55E → #15803D`）
- **原因:** ユーザー系テーマが teal（青緑）のため、管理系の青と視覚的に区別しにくかった。管理系=青、ユーザー系=緑 の設計意図を CSS で明確に表現

---

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
