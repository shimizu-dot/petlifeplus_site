# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PetLifePlus is a pet health management platform with two distinct parts:
- **`frontend/`** — Static HTML/CSS/JS marketing site (no build step)
- **`backend/`** — Spring Boot web application with server-side Thymeleaf rendering

These are not integrated; the frontend is a standalone public site, and the backend is a full-stack web app with its own templates.

## Backend Commands

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

Schema and seed data are **not** applied automatically (`spring.sql.init.mode=never`). Apply manually via `backup.sql` restore or run `schema.sql` / `data.sql` directly against PostgreSQL. To enable auto-init, set `spring.sql.init.mode=always` temporarily.

## Default Credentials (seeded by DataInitializer)

| Email | Password | Role |
|---|---|---|
| admin@petlifeplus.local | admin123 | ADMIN |
| owner1@petlifeplus.local | user123 | USER |
| vet1@petlifeplus.local | vet123 | VET |
| staff1@petlifeplus.local | staff123 | STAFF |
| owner2@petlifeplus.local | user123 | USER |
| owner.light@petlifeplus.local | light123 | USER |
| owner.standard@petlifeplus.local | standard123 | USER |
| owner.premium@petlifeplus.local | premium123 | USER |


## Backend Architecture

**Layered MVC (no REST API — Thymeleaf renders all views):**
```
Controller → Service → Mapper (MyBatis) → PostgreSQL
```

**URL structure:**
- `/` — redirects to frontend index
- `/app/login` — form login (public)
- `/app/dashboard` — authenticated entry point
- `/app/pets`, `/app/pets/{petId}/health-records` — ペット・健康記録 CRUD
- `/app/appointments` — 予約 CRUD（AppointmentPageController）
- `/app/calendar` — ペットカレンダー（CalendarController）
- `/app/notifications` — 通知一覧・既読処理（全認証ユーザー）
- `/app/subscriptions` — サブスクリプション確認（ADMIN は全ユーザー分）
- `/app/password-resets` — パスワード変更（全認証ユーザー）
- `/app/consultations/**` — 診療記録 CRUD（VET / STAFF / ADMIN のみ）
- `/app/consult/chatbot` — チャットボット相談（ConsultChatController）
- `/app/clinic-guide` — 動物病院案内（ClinicGuideController）
- `/app/premium/online-care` — Zoomオンライン診療（PremiumSupportController）
- `/app/reports` — サービス統計（ADMIN のみ）
- `/app/admin/**` — ユーザー管理・お知らせ管理・予約枠管理（ADMIN のみ）
- `/api/slack/events` — Slack Events API 受信（SlackEventController）
- `/api/line/events` — LINE Messaging API 受信（LineEventController）
- `/api/appointments` — 予約 REST API（AppointmentController）
- `/assets/**`, `/css/**`, `/js/**` — static resources (public)

**Authentication:** Spring Security with session-based form login. `UserDetailsServiceImpl` loads users by email. Roles: `ADMIN`, `USER`, `VET`, `STAFF`. Role filtering happens in the service layer (`currentUser.isAdmin()` checks).

**Data access:** MyBatis with annotated SQL in mapper interfaces. All tables with user-generated data use soft delete via `deleted_at` timestamp — queries must filter `WHERE deleted_at IS NULL`.

**HTTP method override:** Thymeleaf forms use `_method` hidden parameter for `PATCH`/`DELETE` (Spring's `HiddenHttpMethodFilter` is enabled).

## External Service Configuration

The following integrations are configured via environment variables (with defaults in `application.properties`):

| Service | Env Vars | Used by |
|---|---|---|
| **OpenAI** | `OPENAI_API_KEY`, `OPENAI_MODEL` (default: `gpt-4.1-mini`), `OPENAI_BASE_URL` | `SymptomCheckService` — AI症状チェック。キー未設定時はキーワードベースのフォールバックで動作 |
| **Slack** | `SLACK_BOT_TOKEN`, `SLACK_SIGNING_SECRET`, `ADMIN_SLACK_USER_IDS` | `SlackEventController` (`/api/slack/events`) + `ConsultChatService` — Slackbot相談 |
| **LINE** | `LINE_CHANNEL_TOKEN`, `LINE_CHANNEL_SECRET`, `ADMIN_LINE_USER_IDS` | `LineEventController` (`/api/line/events`) — LINE Messaging API連携 |
| **Zoom** | `ZOOM_ACCOUNT_ID`, `ZOOM_CLIENT_ID`, `ZOOM_CLIENT_SECRET`, `ZOOM_MEETING_BASE_URL`, `ZOOM_API_BASE_URL`, `ZOOM_OAUTH_BASE_URL` | `ZoomLinkService` + `PremiumSupportController` (`/app/premium/online-care`) — プレミアムオンライン診療 |

未設定のまま起動しても OpenAI はフォールバックで動作し、Slack/Zoom は該当機能を使わない限りエラーにならない。

## Authentication Notes

Login is handled by Spring Security form login via `UserDetailsServiceImpl` + `BCryptPasswordEncoder`. This path works correctly.

`AuthService.login()` is a secondary login method (not called by any controller) and was previously doing plain-text password comparison. It has been fixed to use `passwordEncoder.matches()`. If a controller endpoint is added that calls `AuthService.login()`, it will work correctly.

## Frontend

Static files in `frontend/public/` — open directly in a browser or serve with any static file server. No build process. Pages: `index.html`, `f_service.html`, `f_flow.html`, `f_contact.html`, `f_info.html`, `webapp.html`.

UI conventions (from `frontend/docs/ui-design.md`):
- Colors: `#1D4ED8` (primary blue), `#0EA5A4` (teal), `#F59E0B` (amber accent)
- Fonts: Fredoka (headings), M PLUS Rounded 1c (body)
- Spacing: 8px increments; border-radius 10px (inputs), 14px (buttons/cards)
- Breakpoints: ≥1024px desktop, 768–1023px tablet, ≤767px mobile

## Database Schema

22 tables across these domains:
- **Auth:** `users`, `roles`
- **Pets & Health:** `pets`, `health_records`, `pet_care_records`, `symptom_checks`, `medical_histories`, `medical_attachments`
- **Operations:** `appointments`, `appointment_slots`, `plans`, `subscriptions`, `invoices`, `payments`
- **Messaging:** `notifications`, `notification_recipients`, `email_templates`, `email_messages`, `consult_chat_messages`
- **UX:** `dismissed_reminders` — ユーザーが確認済みのスケジュールリマインダーを永続保存（user_id + reminder_key のユニーク制約）; `pet_calendar_marks` — カレンダーマーク; `announcements` — お知らせ（管理者が作成、全ユーザーに表示）

Detailed schema: `backend/src/main/resources/schema.sql`. Business requirements: `petlife_plus.md` and `backend/docs/requirements.md`.

## Implementation Status

- **Implemented (Must):** User/pet/health-record/appointment CRUD, role-based access, dashboards
- **Implemented (Premium):** AI symptom check (F-009) — `SymptomCheckService` calls OpenAI API with heuristic fallback; gated by plan via `PlanAccessService.canUseAiSymptom()`
- **Implemented:** Notifications (`/app/notifications`) — view & mark-as-read; backed by `notification_recipients` table. Schedule reminders (vaccine/appointment/subscription renewal within 30 days) shown separately with per-item dismiss; dismissed state persisted in `dismissed_reminders` table via `POST /app/notifications/reminders/dismiss`
- **Implemented:** Subscriptions (`/app/subscriptions`) — plan contract list per user; admin sees all
- **Implemented:** Password change (`/app/password-resets`) — requires current password; BCrypt re-hash on save
- **Implemented:** Medical history / Consultations (`/app/consultations/**`) — full CRUD; VET/STAFF/ADMIN only; backed by `medical_histories` table
- **Implemented:** Reports (`/app/reports`) — aggregate stats for admin (users, pets, appointments, subscriptions)
- **Schema only (not implemented):** invoices, payments, email (templates & messages), PDF export
- **All 22 DB tables have corresponding MyBatis mappers.** Mappers for schema-only features (invoices, payments, email, medical_attachments) provide basic CRUD but have no controller/service wired up yet.
- **No automated tests exist** — `backend/docs/test_report.md` is a test plan, not test code
