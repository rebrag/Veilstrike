# Veilstrike — Project Guide for Claude Code

Browser-based, mobile-first PvP strategy game (Polytopia × poker/chess hybrid).
Hex-grid maps, fog of war, class-based combat, skill + luck mechanics.
Pixel-art, minimalist, gameplay-first.

## Tech stack
- **Backend:** Java 21, Spring Boot 3.x, Maven. Base package `com.veilstrike`.
- **Frontend:** React + Phaser.js, PWA, mobile-first.
- **Database:** PostgreSQL (AWS RDS in prod; local Docker in dev).
- **Migrations:** Flyway, versioned SQL in `src/main/resources/db/migration`.
- **Auth:** Custom JWT (access + refresh). Local email/password AND Google OAuth 2.0,
  both issuing the **same internal JWT**. Spring Security + `spring-boot-starter-oauth2-client`.
- **Infra:** AWS EC2 / Elastic Beanstalk. Secrets via AWS Secrets Manager.

## Build priorities (in order)
1. Custom JWT auth (register, login, refresh, sessions table)
2. Spring Boot structure + API scaffolding
3. Phaser hex grid + fog of war
4. Real-time PvP loop via WebSockets
5. Class system + turn-based combat

## Standards
- Prefer complete, working files over snippets.
- Briefly explain architectural decisions so the developer learns while building.
- Clean REST API design; mobile-first; test on low-end Android.
- **Never hardcode secrets** — read from env vars locally, AWS Secrets Manager in prod.
- All SQL changes go through Flyway migrations (never edit a shipped migration).

## Auth specifics (must follow)
- `users` table: `id` (UUID PK), `email` (unique, not null), `password_hash` (**nullable** —
  null for OAuth-only users), `google_id` (**nullable**, unique), `auth_provider`
  (`LOCAL` | `GOOGLE`), `display_name`, `created_at`, `updated_at`.
  - DB constraint: `LOCAL` users must have `password_hash`; `GOOGLE` users must have `google_id`.
- `sessions` table: `id` (UUID PK), `user_id` (FK → users, on delete cascade),
  `refresh_token_hash` (**store the hash, never the raw token**), `expires_at`,
  `revoked` (bool default false), `created_at`, `user_agent`, `ip_address`.
  - Index on `user_id` and `refresh_token_hash`.
- Password hashing: BCrypt (or Argon2). Access tokens short-lived (~15 min),
  refresh tokens long-lived and rotated on use.
- Google OAuth and local login converge on one internal JWT issuance path.

## Brand / design tokens
Design system lives in `brand/`. Use these tokens for any UI work so the app stays
consistent (direction: "Twilight Tactician"):
- `brand/veilstrike-tokens.css` — CSS custom properties (React side).
- `brand/veilstrike-theme.js` — numeric hex for Phaser + render config.
- `brand/veilstrike-brand-book.html` — full styleguide (palette, classes, type, voice).
Core: Abyss `#0E1726`, Slate `#1C2C42`, Teal `#2BB6A3`, Mist `#E8EDF2`.
Classes: Warrior `#C0894E`, Rogue `#E6D24A`, Mage `#8AA6EC`, Priest `#CDD8E4`.

## Local dev
- Postgres: `docker run --name veilstrike-pg -e POSTGRES_PASSWORD=dev -e POSTGRES_DB=veilstrike -p 5432:5432 -d postgres:16`
- Config via env vars: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`.
