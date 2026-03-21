# Signus Backend

Signus backend built with Kotlin and Ktor. It manages JWT authentication, linking between users, shared semaphore state, realtime delivery over WebSocket, and push fallback through FCM when the partner does not have an active websocket session.

## Overview

The backend is the source of truth for:

- users and authentication
- linking and unlinking between two users
- current semaphore state (`AVAILABLE`, `BUSY`, `OFFLINE`)
- websocket sessions for realtime events
- FCM token registration per device
- push fallback through FCM when realtime does not deliver

The Android app consumes this backend through HTTP + JWT + WebSocket. Firebase is no longer used for auth or database; currently it is only kept as push notification transport through FCM.

## Migration status from Firebase

Already migrated to the custom backend:

- authentication
- user persistence and partner relationships
- linking through sessions and codes
- reading `/me` and `/partner`
- state change with `PATCH /status`
- realtime events over WebSocket
- unified websocket event contract:
  - `PARTNER_STATUS_CHANGED`
  - `PARTNER_UNLINKED`
- FCM token registration and removal per device
- backend fallback `realtime -> push` for status changes

Firebase currently keeps only this role:

- Firebase Cloud Messaging (FCM) to deliver push notifications

It is not part of the current system:

- Firebase Auth
- Firestore
- Firebase Realtime Database

## Current architecture

The project follows a feature-based structure:

```text
src/main/kotlin/
  core/          # config, DI, plugins, security, database
  features/
    auth/
    devicetoken/
    linking/
    notification/
    semaphore/
    user/
```

Responsibilities:

- routes: HTTP/WebSocket endpoints, basic validation, auth extraction
- services: use cases and orchestration
- repositories: persistence and DB mapping
- core: cross-cutting infrastructure

General dependency flow:

```text
routes -> services -> repositories -> database
```

The backend uses Koin for DI, Flyway for migrations, and Exposed for data access.

## Technical stack

- Kotlin
- Ktor
- PostgreSQL
- Exposed
- Flyway
- Koin
- JWT
- WebSockets
- Firebase Cloud Messaging
- Docker / Docker Compose
- Testcontainers

## Configuration

1. Copy the example file:

```bash
cp .env.example .env
```

2. Configure the required variables:

| Variable | Description |
| --- | --- |
| `DB_HOST` | PostgreSQL host |
| `DB_PORT` | PostgreSQL port |
| `DB_NAME` | Database name |
| `DB_USER` | Database user |
| `DB_PASSWORD` | Database password |
| `PORT` | Backend HTTP port |
| `JWT_SECRET` | Secret used to sign JWT |
| `JWT_ISSUER` | JWT issuer |
| `JWT_AUDIENCE` | JWT audience |
| `JWT_REALM` | Realm configured in Ktor auth |
| `JWT_EXPIRATION_TIME` | Token duration in ms |
| `FCM_SERVER_KEY` | Credential used by the backend FCM provider |

## Local execution

Full Docker mode:

```bash
docker compose up --build
```

Hybrid mode:

```bash
docker compose up -d db
./gradlew run
```

Stop services:

```bash
docker compose down
```

Remove volumes:

```bash
docker compose down -v
```

## Security

- Protected endpoints require `Authorization: Bearer <jwt>`.
- The websocket requires `token` in the query string: `/ws?token=<jwt>`.
- The JWT must include the `userId` claim.
- Secrets must be provided through environment variables.

## Real endpoints and contracts

Local base URL:

- HTTP: `http://localhost:8080`
- WebSocket: `ws://localhost:8080`

### Auth

#### `POST /auth/register`

Creates a user and returns JWT.

Request:

```json
{
  "email": "user@example.com",
  "password": "secret123",
  "displayName": "User"
}
```

Response `201 Created`:

```json
{
  "accessToken": "<jwt>"
}
```

#### `POST /auth/login`

Authenticates a user and returns JWT.

Request:

```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

Response `200 OK`:

```json
{
  "accessToken": "<jwt>"
}
```

### Linking sessions

#### `POST /linking/sessions`

Creates a linking session for the authenticated user.

Response `201 Created`:

```json
{
  "sessionId": "a6a21519-5d42-43d4-b6ea-e7f0c8187f32",
  "linkCode": "ABC123",
  "expiresAt": "2026-03-20T12:34:56Z"
}
```

#### `POST /linking/sessions/confirm`

Confirms an existing session using `linkCode`.

Request:

```json
{
  "linkCode": "ABC123"
}
```

Response `200 OK`:

```json
{
  "sessionId": "a6a21519-5d42-43d4-b6ea-e7f0c8187f32",
  "status": "CONFIRMED"
}
```

Relevant errors:

- `400 Bad Request` if the code is invalid or if the user tries to link with themselves
- `404 Not Found` if the session does not exist
- `410 Gone` if the session expired
- `409 Conflict` if the session was already confirmed

#### `GET /linking/sessions/{id}`

Checks the current state of a session.

Response `200 OK`:

```json
{
  "sessionId": "a6a21519-5d42-43d4-b6ea-e7f0c8187f32",
  "status": "PENDING"
}
```

Actual states: `PENDING`, `CONFIRMED`, `EXPIRED`.

### Current user and partner

#### `GET /me`

Returns the current state of the authenticated user.

Response `200 OK`:

```json
{
  "id": "user-1",
  "status": "BUSY",
  "statusExpiration": null,
  "statusDuration": null,
  "partnerId": "user-2"
}
```

#### `GET /partner`

Returns the current state of the linked partner.

Response `200 OK`:

```json
{
  "id": "user-2",
  "status": "AVAILABLE",
  "statusExpiration": null,
  "statusDuration": null,
  "partnerId": "user-1"
}
```

If the user does not have a linked partner, the route returns `404 Not Found`.

#### `DELETE /partner`

Unlinks the authenticated user from their current partner.

Response `204 No Content`.

If there is an active websocket on the partner, the backend emits the `PARTNER_UNLINKED` event. This flow currently does not use push fallback.

### Semaphore state

#### `PATCH /status`

Updates the state of the authenticated user.

Request:

```json
{
  "status": "BUSY"
}
```

Supported states: `AVAILABLE`, `BUSY`, `OFFLINE`.

Response `200 OK`:

```json
{
  "status": "BUSY",
  "userId": "user-1",
  "expiration": null,
  "duration": null
}
```

### FCM tokens per device

#### `PUT /devices/fcm-token`

Registers or updates the FCM token of the authenticated device.

Request:

```json
{
  "deviceId": "android-device-1",
  "fcmToken": "fcm-token-value",
  "platform": "android",
  "appVersion": "1.0.0"
}
```

Response `201 Created` when it creates a new record, or `200 OK` when it updates an existing one:

```json
{
  "created": true,
  "token": {
    "id": "token-row-id",
    "deviceId": "android-device-1",
    "platform": "android",
    "appVersion": "1.0.0",
    "active": true,
    "createdAt": 1710930000000,
    "updatedAt": 1710930000000,
    "lastRegisteredAt": 1710930000000,
    "deactivatedAt": null
  }
}
```

Real notes:

- `platform` only accepts `android`
- the backend stores tokens by `userId + deviceId`
- if the same `fcmToken` was active on another user or device, that previous record is deactivated

#### `DELETE /devices/fcm-token/{deviceId}`

Deactivates the active token associated with the authenticated user's `deviceId`.

Response `204 No Content`.

#### `GET /devices/fcm-token`

Returns the tokens of the authenticated user. By default only active ones.

Optional query param:

- `includeInactive=true|false`

### Realtime WebSocket

#### `WS /ws?token=<jwt>`

Registers the authenticated user's websocket session to receive server-push events.

The client does not need to send business messages; the backend uses the connection to push events.

Real events:

#### `PARTNER_STATUS_CHANGED`

```json
{
  "type": "PARTNER_STATUS_CHANGED",
  "partnerId": "user-1",
  "status": "AVAILABLE",
  "statusExpiration": null,
  "timestamp": 1710930000000
}
```

#### `PARTNER_UNLINKED`

```json
{
  "type": "PARTNER_UNLINKED",
  "partnerId": "user-1",
  "timestamp": 1710930000000
}
```

## Real flows

### Linking

1. User A creates `POST /linking/sessions`.
2. Backend generates `sessionId`, `linkCode`, and `expiresAt`.
3. User B calls `POST /linking/sessions/confirm` with `linkCode`.
4. Backend validates the session, expiration, and that it is not the same user.
5. Backend links both users and marks the session as `CONFIRMED`.

### Unlinking

1. A user calls `DELETE /partner`.
2. Backend removes the partner relationship in DB.
3. If the partner has an active websocket, backend sends `PARTNER_UNLINKED`.
4. This flow does not perform push fallback in the current state.

### State change

1. The app calls `PATCH /status`.
2. Backend persists the new state.
3. `StatusServiceImpl` delegates to `NotificationOrchestrator`.
4. The orchestration looks up the partner.
5. If there is a partner, it tries to send `PARTNER_STATUS_CHANGED` over websocket.
6. If there is no active session or realtime delivery fails, it uses push fallback through FCM.
7. If there is no partner, there are no active tokens, or a specific token fails, the state update is not reverted.

### FCM token synchronization

1. The app gets or refreshes its FCM token.
2. The app calls `PUT /devices/fcm-token` with `deviceId`, `fcmToken`, `platform`, and `appVersion`.
3. Backend creates or updates the record in `user_device_tokens`.
4. When the device is no longer valid or the user logs out, the app can call `DELETE /devices/fcm-token/{deviceId}` to deactivate the token.

## Development and testing

Run tests:

```bash
./gradlew test
```

Compile backend:

```bash
./gradlew --no-daemon clean compileKotlin
```

The tests cover:

- feature services and contracts
- HTTP and WebSocket routes
- repositories with PostgreSQL through Testcontainers

## Notes

- OpenAPI/Swagger is present as a dependency, but there is no public route exposed by default.
- Realtime/push migration documentation is kept in `REALTIME_NOTIFICATIONS_PLAN.md`.
