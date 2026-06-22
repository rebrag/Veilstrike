# First auth prompts for Claude Code

Work through these in order. Each is a self-contained prompt you can paste into the
Claude Code panel in VS Code. Read `CLAUDE.md` (repo root) first — Claude Code picks it
up automatically and it carries all the stack + auth rules.

---

## Step 0 — Generate the skeleton (do this yourself, not via Claude)
Use https://start.spring.io with: Maven · Java 21 · Spring Boot 3.x (latest stable) ·
Group `com.veilstrike` · Artifact `veilstrike-api` · Package `com.veilstrike` · Jar.
Dependencies: Spring Web, Spring Security, Spring Data JPA, PostgreSQL Driver,
OAuth2 Client, Flyway Migration, Validation, Lombok.
Unzip into this repo folder.

---

## Prompt 1 — Dependencies + Flyway migration
> The Spring Boot skeleton is in place (Maven, Java 21, package `com.veilstrike`). Per
> CLAUDE.md, do two things:
> 1. Add the `jjwt` libraries (api/impl/jackson, v0.12.6) and
>    `spring-boot-starter-websocket` to `pom.xml`.
> 2. Create `src/main/resources/db/migration/V1__init_users_and_sessions.sql` with the
>    `users` and `sessions` tables exactly as specified in CLAUDE.md — UUID PKs, nullable
>    `password_hash` and `google_id`, the `auth_provider` column, the provider check
>    constraint, the hashed refresh-token column, cascade FK, and the indexes.
> Then add `application.yml` with the Postgres datasource and Flyway config reading all
> credentials from env vars. Explain the key schema decisions briefly. Don't write
> controllers yet.

## Prompt 2 — Entities + repositories
> Create the JPA entities and Spring Data repositories for `users` and `sessions`,
> matching the V1 migration. Use a `UUID` id strategy, an `AuthProvider` enum
> (`LOCAL`, `GOOGLE`), and proper timestamps with `@CreationTimestamp`/`@UpdateTimestamp`.
> Add a `UserRepository.findByEmail` and `findByGoogleId`, and a
> `SessionRepository.findByRefreshTokenHash`. Keep entities clean (no business logic).

## Prompt 3 — JWT token service
> Implement a `JwtService` that issues and validates tokens per CLAUDE.md: short-lived
> access tokens (~15 min) and long-lived refresh tokens. Sign with HS256 using a secret
> from the `JWT_SECRET` env var. Provide methods to generate an access token for a user,
> generate + persist a refresh token (storing only its hash in the sessions table),
> validate an access token, and rotate a refresh token on use. Explain the rotation flow.

## Prompt 4 — Local auth endpoints
> Build the local email/password auth: a `POST /api/auth/register`, `POST /api/auth/login`,
> and `POST /api/auth/refresh`. Hash passwords with BCrypt. Return access + refresh tokens.
> Add request/response DTOs with validation, and a global exception handler that returns
> clean JSON errors. Don't touch Google OAuth yet.

## Prompt 5 — Spring Security filter chain
> Configure Spring Security: stateless session, a JWT authentication filter that validates
> the access token and sets the security context, public access to `/api/auth/**`, and
> authenticated access to everything else. Add CORS config suitable for a mobile PWA
> frontend. Show how to test a protected endpoint.

## Prompt 6 — Google OAuth 2.0
> Add Google social login using `spring-boot-starter-oauth2-client`. On successful Google
> login, find-or-create a user by `google_id` (auth_provider GOOGLE), then issue the SAME
> internal JWT pair as local login — converging both flows on the existing JwtService path.
> Read `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` from env. Explain the find-or-create and
> account-linking edge cases (e.g. a Google email that matches an existing LOCAL user).

---

### Tips
- After each step, run `./mvnw spring-boot:run` against the local Docker Postgres and
  confirm Flyway applies cleanly before moving on.
- Commit after each prompt so you have clean checkpoints.
- If Claude Code drifts from the spec, point it back to CLAUDE.md.
