# Spring Auth

Authentication backend built with **Spring Boot 4** and **Java 21**. Supports local registration and login (email/password), OAuth2 login (Google, GitHub), JWT tokens in HttpOnly cookies, and refresh token rotation stored in PostgreSQL.

The project is part of a larger ecosystem (e.g. frontend at `http://localhost:5173`, “Edumate” naming in Docker/Swagger config), but works as a standalone auth microservice.

---

## Table of contents

- [Features](#features)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [API endpoints](#api-endpoints)
- [Requirements](#requirements)
- [Local setup](#local-setup)
- [Docker Compose](#docker-compose)
- [Configuration](#configuration)
- [OAuth2 (Google / GitHub)](#oauth2-google--github)
- [Project structure](#project-structure)
- [TODO / Roadmap](#todo--roadmap)

---

## Features


| Area                 | Description                                                 |
| -------------------- | ----------------------------------------------------------- |
| **Register / login** | Email + password with BCrypt hashing                        |
| **JWT**              | Access token in `access_token` HttpOnly cookie (HS256)      |
| **Refresh token**    | Random token in DB, rotated on refresh, 14-day validity     |
| **OAuth2**           | Google (OIDC) and GitHub with automatic account creation    |
| **Session**          | Stateless — no server session; authorization via JWT filter |
| **User profile**     | `GET /user/me` for the authenticated user                   |
| **API docs**         | Swagger UI at `/swagger-ui.html`                            |
| **CORS**             | Configured for Vite frontend (`localhost:5173`)             |


---

## Tech stack

- **Java 21**
- **Spring Boot 4.1.0** — Web MVC, Security, Data JPA, OAuth2 Client
- **PostgreSQL 16**
- **JJWT 0.11.5** — JWT generation and validation
- **SpringDoc OpenAPI 2.6.0** — Swagger UI
- **Lombok**
- **Docker** + **Docker Compose** (PostgreSQL, pgAdmin, app)

---

## Architecture

```
┌─────────────┐     cookies (access_token, refresh_token)     ┌──────────────────┐
│   Frontend  │ ◄──────────────────────────────────────────────►│  Spring Auth     │
│  :5173      │     REST: /auth/*, /user/*                      │  :8080           │
└─────────────┘                                               └────────┬─────────┘
       │                                                               │
       │ OAuth redirect                                                │ JPA
       ▼                                                               ▼
┌─────────────┐                                               ┌──────────────────┐
│ Google /    │                                               │   PostgreSQL     │
│ GitHub      │                                               │   (refresh_token,│
└─────────────┘                                               │    app_user)     │
                                                              └──────────────────┘
```

### Authentication flow

1. **Register / Login** — `AuthService` validates credentials, generates JWT and refresh token, sets HttpOnly cookies in the response.
2. **Every request** — `JwtAuthenticationFilter` reads `access_token` from cookies and sets `SecurityContext`.
3. **Refresh** — client sends `refresh_token`; service validates it in DB, revokes the old token, and issues a new pair (rotation).
4. **OAuth2** — on success, `OAuth2SuccessHandler` creates/links the user, sets cookies, and redirects to the frontend.

---

## API endpoints

### Public (`/auth/`**)


| Method | Path             | Description                                        |
| ------ | ---------------- | -------------------------------------------------- |
| `POST` | `/auth/register` | Register — body: `{ "name", "email", "password" }` |
| `POST` | `/auth/login`    | Login — body: `{ "email", "password" }`            |
| `POST` | `/auth/refresh`  | Refresh tokens (requires `refresh_token` cookie)   |
| `POST` | `/auth/logout`   | Clear cookies (maxAge=0)                           |


### OAuth2 (Spring Security)


| Method | Path                           | Description        |
| ------ | ------------------------------ | ------------------ |
| `GET`  | `/oauth2/authorization/google` | Start Google login |
| `GET`  | `/oauth2/authorization/github` | Start GitHub login |


After successful OAuth, the user is redirected to `http://localhost:5173/dashboard`.

### Protected (valid `access_token` required)


| Method | Path       | Description                          |
| ------ | ---------- | ------------------------------------ |
| `GET`  | `/user/me` | Authenticated user data              |
| `GET`  | `/test`    | Simple test endpoint (requires auth) |


### Documentation


| Path               | Description  |
| ------------------ | ------------ |
| `/swagger-ui.html` | Swagger UI   |
| `/v3/api-docs/**`  | OpenAPI JSON |


---

## Requirements

- **JDK 21**
- **Maven 3.9+** (or `./mvnw`)
- **Docker & Docker Compose** (optional)
- **Google Cloud** and/or **GitHub** developer account (for OAuth2)

---

## Docker Compose

Full stack (PostgreSQL + pgAdmin + app):

```bash
cp .env.template .env
# fill in .env (JWT, OAuth credentials)
docker compose up --build
```


| Service    | Port   | Description                            |
| ---------- | ------ | -------------------------------------- |
| App        | `8080` | Spring Auth                            |
| PostgreSQL | `5432` | Database `edumate`                     |
| pgAdmin    | `5050` | DB panel (`admin@admin.com` / `admin`) |


---

## Configuration

### `application.properties`


| Property                        | Value         | Notes                                                      |
| ------------------------------- | ------------- | ---------------------------------------------------------- |
| `spring.jpa.hibernate.ddl-auto` | `create-drop` | Schema recreated on every restart — **not for production** |
| `spring.jpa.show-sql`           | `true`        | SQL logging — disable in prod                              |
| `server.port`                   | `8080`        |                                                            |


### Environment variables (`.env`)


| Variable                                       | Description                                 |
| ---------------------------------------------- | ------------------------------------------- |
| `JWT_SECRET_KEY`                               | HMAC key for signing JWT (`jwt.secret.key`) |
| `SPRING_DATASOURCE_URL`                        | PostgreSQL JDBC URL                         |
| `SPRING_DATASOURCE_USERNAME`                   | DB user                                     |
| `SPRING_DATASOURCE_PASSWORD`                   | DB password                                 |
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_`* | OAuth2 Google/GitHub credentials            |


Spring Boot maps `JWT_SECRET_KEY` → `jwt.secret.key` automatically.

---

## OAuth2 (Google / GitHub)

### Google

1. Create a project in [Google Cloud Console](https://console.cloud.google.com/).
2. Configure the OAuth consent screen.
3. Create an OAuth Client ID (type: Web application).
4. Redirect URI: `http://localhost:8080/login/oauth2/code/google`
5. Paste Client ID and Secret into `.env`.

### GitHub

1. **Settings → Developer settings → OAuth Apps → New OAuth App**
2. Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`
3. Paste Client ID and Secret into `.env`.

---

## Project structure

```
src/main/java/com/example/auth/
├── AuthApplication.java          # Entry point
├── TestController.java           # Test endpoint /test
├── auth/
│   ├── controller/AuthController.java
│   ├── dto/                      # LoginRequest, RegisterRequest, AuthTokens, UserDto
│   ├── entity/RefreshToken.java
│   ├── repository/RefreshTokenRepository.java
│   └── service/                  # AuthService, RefreshTokenService
├── config/
│   ├── AppConfig.java            # Security beans, BCrypt, RestTemplate
│   ├── CorsConfig.java
│   └── SwaggerConfig.java
├── security/
│   ├── config/SecurityConfiguration.java
│   ├── jwt/                      # JwtService, JwtAuthenticationFilter
│   └── oauth/                    # OAuth2 handlers and user services
└── user/
    ├── controller/UserController.java
    ├── entity/                   # AppUser, AuthProvider
    ├── repository/UserRepository.java
    └── service/UserService.java
```

---

## TODO / Roadmap

Based on the current codebase — gaps, inconsistencies, and suggested improvements.

### Critical / fixes

- [ ] **Database name mismatch** — `docker-compose.yml` creates `edumate`, while `.env.template` points to `auth`. Align everywhere.
- [ ] **Token lifetime inconsistency** — `access_token` cookie has `maxAge(60)` (60 seconds) in `AuthController`, while JWT in `JwtService` expires after ~24 minutes, and in `OAuth2SuccessHandler` the cookie is 15 minutes. Unify JWT and cookie TTL.
- [ ] **Logout does not revoke refresh tokens in DB** — `/auth/logout` only clears cookies; `revokeAllForUser` exists in the repository but is unused.
- [ ] `**AuthProvider.LOCAL` not set on registration** — `provider` stays `null` for local accounts.
- [ ] **No global exception handler** — `RuntimeException`, `IllegalStateException` return raw 500 instead of proper HTTP codes (409 for taken email, 401 for invalid refresh token, etc.).
- [ ] `**ddl-auto=create-drop`** — data is lost on restart; add migrations (Flyway/Liquibase) and use `validate`/`update` in prod.

### Security

- [ ] **Input validation** — add `@Valid`, `@Email`, `@NotBlank`, `@Size` to register/login DTOs; password strength policy.
- [ ] **Linking OAuth and local accounts** — `findByEmail` does not check provider; consider account linking strategy or duplicate blocking.
- [ ] **Swagger vs cookies** — docs describe Bearer JWT, but the app uses HttpOnly cookies; update OpenAPI (cookie security scheme) or add `Authorization` header support.
- [ ] **Hardcoded CORS and redirect URL** — `localhost:5173` is hardcoded; move to environment config.
- [ ] **Refresh token rotation without User-Agent/IP validation** — variables are logged but not used for reuse detection.

### Functionality

- [ ] **Email verification** — no email verification after registration.
- [ ] **Password reset** — no “forgot password” flow.
- [ ] **Expired token cleanup** — `deleteExpired` in the repository is never called; add a scheduled job.
- [ ] **Remove or document `TestController`** — `/test` requires auth but is commented out in `permitAll`.

### Code quality

- [ ] **Refactor duplication** — cookie logic duplicated in `AuthController` and `OAuth2SuccessHandler`; extract a helper/factory.

### DevOps / production

- [ ] **Spring profiles** — `application-dev.properties`, `application-prod.properties` with different JPA, logging, CORS settings.
- [ ] **Health checks** — add Spring Actuator (`/actuator/health`).
- [ ] **CI/CD** — pipeline (build, test, Docker push).

### Documentation

- [ ] **Request examples** — curl/HTTPie for register, login, refresh with cookie handling.
- [ ] **ER diagram** — `AppUser` ↔ `RefreshToken` relationship.
- [ ] **License** — define project license.

---

## License

TBD — see [TODO](#todo--roadmap).