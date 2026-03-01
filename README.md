# Signus Backend

Backend service for Signus, built with Kotlin and Ktor, providing secure authentication, user presence management, and real-time partner notifications.

## Overview

Signus Backend is responsible for:
- User authentication and JWT token issuance.
- User status updates (`AVAILABLE`, `BUSY`, `OFFLINE`).
- Real-time partner notifications over WebSocket.
- Push notification fallback through FCM when real-time delivery is not available.
- Data persistence and schema migrations.

## Tech Stack

- Kotlin
- Ktor
- PostgreSQL
- Exposed
- Flyway
- Koin (DI)
- JWT (Auth)
- WebSockets
- Docker + Docker Compose
- GitHub Actions
- Testcontainers

## Architecture

Business logic is isolated from transport concerns, making the codebase testable and modular.
`routes -> services -> repositories -> database`.

Cross-cutting concerns are organized under `core/` (configuration, security, DI, plugins, database bootstrap).  
Real-time notifications are sent over WebSocket, with FCM push used as fallback when the partner is offline.

## Prerequisites

- Docker
- Docker Compose
- Java 21 (only if running backend outside Docker)
- Gradle (optional, `./gradlew` wrapper is included)

## Environment Configuration

1. Copy the example file:

```bash
cp .env.example .env
```

2. Set values for the active environment variables:

| Variable | Description |
| --- | --- |
| `DB_HOST` | PostgreSQL host |
| `DB_PORT` | PostgreSQL port |
| `DB_NAME` | PostgreSQL database name |
| `DB_USER` | PostgreSQL user |
| `DB_PASSWORD` | PostgreSQL password |
| `PORT` | API port (default `8080`) |
| `JWT_SECRET` | Secret key used to sign JWTs |
| `JWT_ISSUER` | JWT issuer claim |
| `JWT_AUDIENCE` | JWT audience claim |
| `JWT_REALM` | JWT realm used by Ktor auth |
| `JWT_EXPIRATION_TIME` | JWT expiration time in milliseconds |
| `FCM_SERVER_KEY` | Firebase Cloud Messaging server key |

## Running Locally

### Full Docker Mode

Run API + PostgreSQL:

```bash
docker compose up --build
```

### Hybrid Mode (DB in Docker, API Local)

Start PostgreSQL in Docker:

```bash
docker compose up -d db
```

Run backend locally:

```bash
./gradlew run
```

### Stop Services

```bash
docker compose down
```

### Remove Volumes

```bash
docker compose down -v
```

## Production Deployment

Deploy Signus Backend as containers, keeping API and database configuration fully driven by environment variables.

Typical production setup:
- Build and run with Docker Compose on the server.
- Place a reverse proxy (Nginx or Traefik) in front of the API for TLS termination and routing.
- Manage secrets and environment variables through secure server or platform mechanisms.
- PostgreSQL data should be persisted using Docker volumes and backed up regularly.

## Security

- Protected HTTP endpoints use JWT Bearer authentication.
- Tokens are signed with `JWT_SECRET` and validated against issuer/audience/realm settings.
- WebSocket connections require a JWT token in the connection query string (`/ws?token=...`).
- Secrets must never be committed to the repository. Use environment-based configuration only.

## CI Pipeline

GitHub Actions workflow (`.github/workflows/ci.yml`) runs on:
- Push to `main` and `dev`
- Pull requests targeting `main` and `dev`

Pipeline responsibilities:
- Set up JDK 21
- Set up Gradle caching
- Validate Docker availability (required by Testcontainers)
- Run full test suite (`./gradlew clean test --no-daemon --info --stacktrace`)
- Submit dependency graph on push to `main`

## Testing Strategy

The project includes:
- Unit tests for services and feature behavior.
- Integration tests for routes, DI wiring, and repositories.
- Tests follow a Given–When–Then structure for clarity and maintainability.

Run tests:

```bash
./gradlew test
```

Integration repository tests use Testcontainers with PostgreSQL, so Docker must be available.

## API Documentation

Base URL (local):
- `http://localhost:8080` (HTTP)
- `ws://localhost:8080` (WebSocket)

Authentication conventions:
- HTTP protected endpoints require `Authorization: Bearer <jwt>`.
- WebSocket endpoint requires `token` query parameter.

### Auth

#### `POST /auth/register`

Creates a user and returns an access token.

Request:

```json
{
  "email": "user@example.com",
  "password": "secret123",
  "displayName": "User"
}
```

Success response (`201 Created`):

```json
{
  "accessToken": "<jwt>"
}
```

#### `POST /auth/login`

Authenticates a user and returns an access token.

Request:

```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

Success response (`200 OK`):

```json
{
  "accessToken": "<jwt>"
}
```

### Status

#### `PATCH /status`

Updates the authenticated user's status.  
Requires JWT Bearer token.

Request:

```json
{
  "status": "BUSY"
}
```

Success response (`200 OK`):

```json
{
  "status": "BUSY",
  "userId": "user-id",
  "expiration": null,
  "duration": null
}
```

### WebSocket Notifications

#### `WS /ws?token=<jwt>`

Registers the current user for real-time notifications.

Event payload example:

```json
{
  "type": "PARTNER_STATUS_CHANGED",
  "senderId": "user-1",
  "partnerId": "user-2",
  "status": "AVAILABLE"
}
```

### OpenAPI / Swagger

OpenAPI/Swagger dependencies are present in the project, but no public docs route is currently exposed by default.

## Project Structure

```text
src/
  main/
    kotlin/
      core/           # config, DI, plugins, security, database setup
      features/       # auth, semaphore (status), notification, user
    resources/
      db/migration/   # Flyway SQL migrations
  test/
    kotlin/
      features/       # feature-level unit/integration tests
      integration/    # repository and DI integration tests
      support/        # test utilities and Testcontainers setup
Dockerfile
docker-compose.yml
.env.example
.github/workflows/ci.yml
```

## Useful Commands

```bash
# Run locally
./gradlew run

# Run tests
./gradlew test

# Build project
./gradlew clean build

# Start full local stack
docker compose up --build

# Stop stack
docker compose down

# Stop and remove DB volume
docker compose down -v
```

## License

This project is licensed under the MIT License.

Copyright (c) 2026 Edelsol

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.