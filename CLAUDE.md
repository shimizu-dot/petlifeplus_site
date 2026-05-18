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

Schema and seed data (`schema.sql`, `data.sql`) are applied automatically on every startup (`spring.sql.init.mode=always`).

## Default Credentials (seeded by DataInitializer)

| Email | Password | Role |
|---|---|---|
| admin@petlifeplus.local | admin123 | ADMIN |
| owner1@petlifeplus.local | user123 | USER |
| vet1@petlifeplus.local | vet123 | VET |
| staff1@petlifeplus.local | staff123 | STAFF |

## Backend Architecture

**Layered MVC (no REST API — Thymeleaf renders all views):**
```
Controller → Service → Mapper (MyBatis) → PostgreSQL
```

**URL structure:**
- `/` — redirects to frontend index
- `/app/login` — form login (public)
- `/app/dashboard` — authenticated entry point
- `/app/pets`, `/app/appointments`, `/app/health-records` — user-facing CRUD
- `/app/admin/**` — requires ADMIN role
- `/assets/**`, `/css/**`, `/js/**` — static resources (public)

**Authentication:** Spring Security with session-based form login. `UserDetailsServiceImpl` loads users by email. Roles: `ADMIN`, `USER`, `VET`, `STAFF`. Role filtering happens in the service layer (`currentUser.isAdmin()` checks).

**Data access:** MyBatis with annotated SQL in mapper interfaces. All tables with user-generated data use soft delete via `deleted_at` timestamp — queries must filter `WHERE deleted_at IS NULL`.

**HTTP method override:** Thymeleaf forms use `_method` hidden parameter for `PATCH`/`DELETE` (Spring's `HiddenHttpMethodFilter` is enabled).

## Key Known Issue

`AuthService` currently compares passwords in plain text. It should use:
```java
passwordEncoder.matches(request.password(), user.passwordHash())
```
instead of direct string equality.

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
- **Pets & Health:** `pets`, `health_records`, `symptom_checks`, `medical_histories`, `medical_attachments`
- **Operations:** `appointments`, `plans`, `subscriptions`, `invoices`, `payments`
- **Messaging:** `notifications`, `notification_recipients`, `email_templates`, `email_messages`

Detailed schema: `backend/src/main/resources/schema.sql`. Business requirements: `petlife_plus.md` and `backend/docs/requirements.md`.

## Implementation Status

- **Implemented (Must):** User/pet/health-record/appointment CRUD, role-based access, dashboards
- **Schema only (not implemented):** AI symptom check (F-009), invoices, payments, notifications, email, PDF export
- **No automated tests exist** — `backend/docs/test_report.md` is a test plan, not test code
