# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PetLifePlus is a pet health management platform with two distinct parts:
- **`frontend/`** — Static HTML/CSS/JS marketing site (no build step)
- **`backend/`** — Spring Boot web application with server-side Thymeleaf rendering

These are not integrated; the frontend is a standalone public site, and the backend is a full-stack web app with its own templates.

## 起動方法

### Docker（推奨）

```bash
# 初回 / ソース変更後: イメージを再ビルドして起動
docker compose up --build

# 通常起動（バックグラウンド）
docker compose up -d

# ログ確認
docker compose logs -f app

# 停止
docker compose down

# DB データを含めて完全削除（再構築したいとき）
docker compose down -v && docker compose up --build
```

**環境変数のカスタマイズ:**
```bash
cp .env.example .env   # .env を編集して API キー等を設定
docker compose up -d
```

`.env` は `.gitignore` 対象のため Git にはコミットされない。未設定の外部サービス（OpenAI / Groq / SendGrid / Slack / LINE / Zoom）はフォールバックモードで動作する。

**本番環境で DB 自動初期化を無効にする場合:**
```bash
SQL_INIT_MODE=never docker compose up -d
# または .env に SQL_INIT_MODE=never を記載
```

---

### Maven（ローカル直接起動）

PostgreSQL を別途起動している場合のみ使用。

```bash
# Run development server (from backend/)
mvn spring-boot:run

# Build JAR
mvn clean package

# Run built JAR
java -jar target/petlife-0.0.1-SNAPSHOT.jar
```

**Prerequisites:** PostgreSQL running locally with:
- Host: `localhost:5432`
- Database: `petlifeplus`
- Username: `postgres`
- Password: `hs0512`

```bash
# Maven での初回 DB セットアップ
createdb -U postgres petlifeplus
mvn spring-boot:run   # 起動時に schema.sql → data.sql が自動実行される

# DB 完全再構築
dropdb -U postgres petlifeplus && createdb -U postgres petlifeplus && mvn spring-boot:run
```

`spring.sql.init.mode=${SQL_INIT_MODE:always}` により起動のたびに schema.sql → data.sql が実行されるが、両ファイルは冪等（`CREATE TABLE IF NOT EXISTS` / `ON CONFLICT DO NOTHING`）なので安全。

## Default Credentials (seeded by data.sql)

全ユーザー・プラン・ペット・サブスクリプションは `data.sql` で定義され、起動時に自動適用される（`spring.sql.init.mode=always`）。`DataInitializer` はセーフティネット（`data.sql` が無効な環境向け）として残す。

| Email | Password | Role | Plan |
|---|---|---|---|
| super@petlife.local | super123 | SUPER | — |
| admin@petlife.local | admin123 | ADMIN | — |
| vet1@petlife.local | vet123 | VET | — |
| staff1@petlife.local | staff123 | STAFF | — |
| owner1@petlife.local | user123 | USER | Light |
| owner2@petlife.local | user123 | USER | Standard |
| owner3@petlife.local | user123 | USER | Premium |

> **注意:** Light プラン（owner1）は `/app/appointments`（予約ページ）にアクセスできない。`AppointmentPageController.ensureAccessible()` が Standard 以上を要求するため。


## Backend Architecture

**Layered MVC (Thymeleaf renders all app views):**
```
Controller → Service → Mapper (MyBatis) → PostgreSQL
```

> **Note:** `/api/appointments` は例外として `@RestController` による JSON REST API（`AppointmentController`）も存在する。その他の `/app/**` はすべて Thymeleaf MVC。

**URL structure:**
- `/` — redirects to frontend index
- `/app/login` — form login (public)
- `/app/forgot-password`, `/app/reset-password` — メールトークンベースのパスワードリセット（public、`ForgotPasswordController`）
- `/app/dashboard` — authenticated entry point
- `/app/pets`, `/app/pets/{petId}/health-records` — ペット・健康記録 CRUD
- `GET /app/pets/{petId}/health-records/print` — 診療情報印刷画面（ペット基本情報・ワクチン記録・診療履歴・AI症状チェック履歴・健康記録をブラウザ印刷）
- `POST /app/pets/{id}/decease` — ペット永眠マーク（`PetController`、ペットの `deceased_at` を設定し以後の操作を制限）
- `/app/appointments` — 予約一覧・新規登録（**STANDARD以上のみ**、Light プランは `AppointmentPageController.ensureAccessible()` で 400 エラー）
- `POST /app/appointments/{id}/approve` — 予約承認 → CONFIRMED（VET/STAFF/ADMIN/SUPER のみ）
- `POST /app/appointments/{id}/reject` — 予約却下 → CANCELED（VET/STAFF/ADMIN/SUPER のみ）
- `POST /app/appointments/{id}/cancel` — ユーザー自身によるキャンセル（オーナーのみ、スタッフは不可）
- `POST /app/appointments/delete-selected` — 選択予約の一括削除
- `/app/calendar` — ペットカレンダー（CalendarController）
- `/app/notifications` — 通知一覧・既読処理（全認証ユーザー）
- `/app/subscriptions` — サブスクリプション確認（ADMIN は全ユーザー分）
- `GET /app/invoices/{id}` — 請求書・領収書ビュー（`UserInvoiceController`、ブラウザ印刷対応）
- `/app/password-resets` — パスワード変更（全認証ユーザー・現在のパスワード必須）
- `/app/consultations/**` — 診療記録 CRUD（SUPER / VET / STAFF のみ）
- `/app/consult/chatbot` — チャットボット相談（ConsultChatController）
- `/app/clinic-guide` — 動物病院案内（ClinicGuideController）
- `/app/premium/online-care` — Zoomオンライン診療（PremiumSupportController）
- `/app/reports` — サービス統計（ADMIN / SUPER のみ）
- `/app/admin/users/**` — ユーザー管理（ADMIN / SUPER / VET / STAFF）
- `/app/admin/announcements/**` — お知らせ管理（ADMIN / SUPER / STAFF）
- `/app/admin/appointment-slots/**` — 予約枠管理（ADMIN / SUPER / STAFF）。過去日付は拒否（エラー表示・スロット一覧非表示）。追加枠・ブロック枠を日別で管理
- `/app/admin/billing/**` — 請求・決済管理（ADMIN / SUPER のみ、`BillingController`）
- `POST /app/admin/billing/{id}/resend-notification` — お支払い案内を顧客へ再送（アプリ内通知 + メール + LINE）
- `/app/admin/line/push` — LINE一斉配信（ADMIN / SUPER のみ、`LinePushController`）
- `/app/admin/**` — その他管理機能（ADMIN / SUPER のみ）
- `/api/slack/events` — Slack Events API 受信（SlackEventController）
- `/api/line/events` — LINE Messaging API 受信（LineEventController）
- `/api/appointments` — 予約 REST API（AppointmentController）
- `/assets/**`, `/css/**`, `/js/**` — static resources (public)

**Authentication:** Spring Security with session-based form login. `UserDetailsServiceImpl` loads users by email. Roles: `SUPER`, `ADMIN`, `USER`, `VET`, `STAFF`. `SUPER` is treated like `ADMIN` with the same access level in `SecurityConfig`. Role filtering also happens in the service layer (`currentUser.isAdmin()` checks).

**Data access:** MyBatis with annotated SQL in mapper interfaces. All tables with user-generated data use soft delete via `deleted_at` timestamp — queries must filter `WHERE deleted_at IS NULL`.

**HTTP method override:** Thymeleaf forms use `_method` hidden parameter for `PATCH`/`DELETE` (Spring's `HiddenHttpMethodFilter` is enabled).

## External Service Configuration

The following integrations are configured via environment variables (with defaults in `application.properties`):

| Service | Env Vars | Used by |
|---|---|---|
| **OpenAI** | `OPENAI_API_KEY`, `OPENAI_MODEL` (default: `gpt-4.1-mini`), `OPENAI_BASE_URL` | `SymptomCheckService`（AI症状チェック F-009 のみ）。未設定時は `heuristic()` フォールバックで動作。詳細: `backend/docs/ai-features.md` |
| **Chatbot (Groq)** | `CHATBOT_API_KEY`, `CHATBOT_MODEL` (default: `llama-3.1-8b-instant`), `CHATBOT_BASE_URL` (default: Groq) | `ConsultChatService`（チャットボット相談 F-022）。OpenAI 互換 API を任意に差し替え可能。未設定時は `fallbackReply()` フォールバック（全プラン利用可） |
| **SendGrid** | `SENDGRID_API_KEY`, `SENDGRID_FROM_EMAIL` | `BillingNotificationService`（請求メール）+ `PasswordResetService`（パスワードリセットメール）。未設定時はメール送信をスキップしログに警告 |
| **Slack** | `SLACK_BOT_TOKEN`, `SLACK_SIGNING_SECRET`, `ADMIN_SLACK_USER_IDS` | `SlackEventController` (`/api/slack/events`) — LINE 相当の Slack Bot 連携 |
| **LINE** | `LINE_CHANNEL_TOKEN`, `LINE_CHANNEL_SECRET`, `ADMIN_LINE_USER_IDS` | `LineEventController` (`/api/line/events`) + `LinePushController` (`/app/admin/line/push`) — LINE Messaging API連携・一斉配信 |
| **Zoom** | `ZOOM_ACCOUNT_ID`, `ZOOM_CLIENT_ID`, `ZOOM_CLIENT_SECRET`, `ZOOM_MEETING_BASE_URL`, `ZOOM_API_BASE_URL`, `ZOOM_OAUTH_BASE_URL` | `ZoomLinkService` + `PremiumSupportController` (`/app/premium/online-care`) — プレミアムオンライン診療 |

未設定のまま起動しても OpenAI・Chatbot はフォールバックで動作し、SendGrid/Slack/Zoom は該当機能を使わない限りエラーにならない。

## Authentication Notes

Login is handled by Spring Security form login via `UserDetailsServiceImpl` + `BCryptPasswordEncoder`. This path works correctly.

`AuthService.login()` is a secondary login method (not called by any controller) and was previously doing plain-text password comparison. It has been fixed to use `passwordEncoder.matches()`. If a controller endpoint is added that calls `AuthService.login()`, it will work correctly.

## Frontend

Static files in `frontend/public/` — open directly in a browser or serve with any static file server. No build process. Pages: `index.html`, `f_service.html`, `f_flow.html`, `f_contact.html`, `f_info.html`, `webapp.html`, `line-qr.html`.

UI conventions (from `frontend/docs/ui-design.md`):
- Colors: `#FF8FB1` (brand pink), `#8C6EE6` (accent purple), `#4A2D3C` (text)
- Fonts: Fredoka (headings), M PLUS Rounded 1c (body)
- Spacing: `--space-section: 36px`, `--space-block: 22px`; border-radius 999px (buttons), 26px (cards), 10px (inputs)
- Breakpoints: ≥768px desktop, <768px mobile (main), ≥900px for wider grids

> **CSS sync note:** `frontend/public/assets/css/style.css` is the source of truth.
> `backend/src/main/resources/static/assets/css/style.css` is a copy served by Spring Boot.
> After editing the frontend CSS, manually copy the file to the backend static path to keep them in sync.

## Database Schema

24 tables across these domains:
- **Auth:** `users`, `roles`, `password_reset_tokens` — メールトークンベースのパスワードリセット（`ForgotPasswordController` / `PasswordResetService` で使用）
- **Pets & Health:** `pets`, `health_records`, `pet_care_records`, `symptom_checks`, `medical_histories`, `medical_attachments`
- **Operations:** `appointments`, `appointment_slots`, `plans`, `plan_features`, `subscriptions`, `invoices`, `payments`
- **Messaging:** `notifications`, `notification_recipients`, `email_templates`, `email_messages`, `consult_chat_messages`
- **UX:** `dismissed_reminders` — ユーザーが確認済みのスケジュールリマインダーを永続保存（user_id + reminder_key のユニーク制約）; `pet_calendar_marks` — カレンダーマーク; `announcements` — お知らせ（管理者が作成、全ユーザーに表示）

`plan_features` — プランごとに利用可能な機能コードを管理。コード: `AI_SYMPTOM` / `SLACK_BOT` / `LINE_BOT` / `ZOOM_CONSULT`。`PlanFeatureMapper` + `PlanAccessService` でアクセス。ADMIN/VET/STAFF は全機能利用可能として扱う。

Detailed schema: `backend/src/main/resources/schema.sql`. Business requirements: `petlife_plus.md` and `backend/docs/requirements.md`.

## Implementation Status

- **Implemented (Must):** User/pet/health-record/appointment CRUD, role-based access, dashboards
- **Implemented (Premium):** 以下の2機能は独立した実装。詳細は `backend/docs/ai-features.md` を参照
  - **AI症状チェック (F-009)** — `SymptomCheckService` / `POST /app/pets/{id}/symptom-check` / ペット紐付き・1ターン完結・結果を `symptom_checks` テーブルに保存 / OpenAI (`openai.*`) temperature=0.2 / フォールバック: `heuristic()`（キーワード3分類） / `PlanAccessService.canUseAiSymptom()` でゲート（STANDARD以上）
  - **チャットボット相談 (F-022)** — `ConsultChatService` / `POST /app/consult/chatbot` / ユーザー紐付き・マルチターン・履歴を `consult_chat_messages` テーブルに蓄積 / Groq/OpenAI互換 (`chatbot.*`) temperature=0.7 / フォールバック: `fallbackReply()`（5段階フロー） / プランゲートなし（全プラン利用可）
- **Implemented:** Notifications (`/app/notifications`) — view & mark-as-read; backed by `notification_recipients` table. Schedule reminders (vaccine/appointment/subscription renewal within 30 days) shown separately with per-item dismiss; dismissed state persisted in `dismissed_reminders` table via `POST /app/notifications/reminders/dismiss`
- **Implemented:** Subscriptions (`/app/subscriptions`) — plan contract list per user; admin sees all. Subscription renewal triggers invoice creation and notifies user via email + LINE
- **Implemented:** Password change (`/app/password-resets`) — requires current password; BCrypt re-hash on save
- **Implemented:** Forgot password (`/app/forgot-password` → `/app/reset-password`) — email token-based reset via SendGrid SMTP; token stored in `password_reset_tokens` table
- **Implemented:** Medical history / Consultations (`/app/consultations/**`) — full CRUD; VET/STAFF/SUPER only; backed by `medical_histories` table
- **Implemented:** Reports (`/app/reports`) — aggregate stats for admin (users, pets, appointments, subscriptions)
- **Implemented (F-011):** Billing (`/app/admin/billing`) — invoice creation, payment registration, PAID/PARTIAL tracking; `POST /app/admin/billing/{id}/resend-notification` で通知再送可。通知は **同期アプリ内通知（確実）** + `@Async` email/LINE（設定時）の2段階。`SubscriptionController.renewRequest()` で invoice 作成失敗時はエラーメッセージ表示（サイレント握りつぶし廃止）
- **Implemented (F-013 部分):** 診療情報印刷 (`GET /app/pets/{petId}/health-records/print`) — ブラウザ印刷。出力項目: ペット基本情報・ワクチン/予防記録・診療履歴・**AI症状チェック履歴**・健康記録。汎用 PDF ライブラリ（iText 等）は未使用
- **Schema only (not implemented):** email templates & messages (UI)、汎用 PDF ダウンロード（レポート画面 / 一覧画面）
- **All 24 DB tables have corresponding MyBatis mappers** (23 mapper interfaces; `AppointmentMapper` and `AppointmentSlotMapper` are separate). Mappers for schema-only features (email_templates, email_messages, medical_attachments) provide basic CRUD but have no controller/service wired up yet.
- **No automated tests exist** — `backend/docs/test_report.md` is a test plan, not test code
