# Signus - Current Realtime and Push Status

This document reflects the current state of the migration from Firebase to a custom backend for synchronization between users.

The architecture principle remains the same:

- the Ktor backend is the source of truth
- WebSocket and FCM are delivery channels
- the persisted state has priority over any client cache

## Executive summary

Current status:

- phase 1 completed in backend
- phase 2 completed in backend
- phase 3 completed in backend
- phase 4 pending in Android

Current result:

- if the partner app has an active websocket, it receives realtime events
- if it does not have an active websocket, the backend falls back to FCM push for status changes
- unlinking still notifies only through realtime

## Currently implemented architecture

### Channel 1 - WebSocket

Current usage:

- client connection to `WS /ws?token=<jwt>`
- backend registers the session by `userId`
- backend sends server-push events to the partner when appropriate

Real events:

- `PARTNER_STATUS_CHANGED`
- `PARTNER_UNLINKED`

### Channel 2 - FCM Push

Current usage:

- backend keeps tokens per device in `user_device_tokens`
- backend looks up the partner's active tokens
- backend sends push through `FcmPushProvider` when realtime does not deliver a status change

Current role of Firebase:

- only FCM transport

Firebase no longer participates in:

- auth
- user persistence
- state persistence
- realtime synchronization between clients

## Real status by component

### Backend

Completed:

- auth with JWT
- linking through sessions
- current user and partner retrieval
- `PATCH /status`
- WebSocket `/ws`
- unified realtime contract
- FCM token registration per device
- backend FCM provider
- `realtime -> fallback push` orchestration for status changes

Pending in backend:

- push fallback for unlinking
- richer or typed push payload if needed in Android
- delivery metrics or traceability

### Android app

Confirmed by backend contract:

- it can authenticate through JWT
- it can open websocket with `token`
- it can update state through HTTP
- it can register and deactivate FCM token per device

Pending to complete the end-to-end migration:

- actual push reception in background
- showing system notification
- deciding how to refresh local state after tapping a push

## Migration phases

### Phase 1 - Synchronize FCM token

Status: completed in backend.

Implemented:

- `PUT /devices/fcm-token`
- `DELETE /devices/fcm-token/{deviceId}`
- `GET /devices/fcm-token`
- persistence in `user_device_tokens` table
- active token lookup through `DeviceTokenLookupPort`

Corrections compared to previous versions of this document:

- `POST /users/fcm-token` does not exist
- the real contract is per device, not flat per user

### Phase 2 - Push service in backend

Status: completed in backend.

Implemented:

- `PartnerPushNotificationService`
- `FcmPushProvider`
- sending to multiple active tokens
- tolerance to failure per individual token

Real behavior:

- if there are no active tokens, the flow returns without breaking the use case
- if a specific token fails, it continues with the rest

### Phase 3 - Integration with state change

Status: completed in backend.

Current real flow:

1. `PATCH /status`
2. persistence in DB
3. `StatusServiceImpl` delegates to `NotificationOrchestrator`
4. the orchestrator looks up the partner
5. it tries to send `PARTNER_STATUS_CHANGED` over websocket
6. if realtime does not deliver, it uses push fallback

Real pieces involved:

- `StatusServiceImpl`
- `NotificationOrchestrator`
- `RealtimeNotificationService`
- `PartnerPushNotificationService`
- `PartnerLookupPort`

Corrections compared to previous versions:

- `SemaphoreStatusChangedEvent` no longer exists as the current contract
- the current realtime contract is `PARTNER_STATUS_CHANGED`
- FCM integration in backend is no longer pending

### Phase 4 - Receiving notifications in Android

Status: pending outside this repository.

Pending:

- `FirebaseMessagingService` or equivalent in the app
- system notification
- deep link or app open strategy
- local state re-sync if applicable

## Real contracts

### Auth

#### `POST /auth/register`

Request:

```json
{
  "email": "user@example.com",
  "password": "secret123",
  "displayName": "User"
}
```

Response:

```json
{
  "accessToken": "<jwt>"
}
```

#### `POST /auth/login`

Request:

```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

Response:

```json
{
  "accessToken": "<jwt>"
}
```

### Linking sessions

#### `POST /linking/sessions`

Response:

```json
{
  "sessionId": "a6a21519-5d42-43d4-b6ea-e7f0c8187f32",
  "linkCode": "ABC123",
  "expiresAt": "2026-03-20T12:34:56Z"
}
```

#### `POST /linking/sessions/confirm`

Request:

```json
{
  "linkCode": "ABC123"
}
```

Response:

```json
{
  "sessionId": "a6a21519-5d42-43d4-b6ea-e7f0c8187f32",
  "status": "CONFIRMED"
}
```

#### `GET /linking/sessions/{id}`

Response:

```json
{
  "sessionId": "a6a21519-5d42-43d4-b6ea-e7f0c8187f32",
  "status": "PENDING"
}
```

### `/partner`

#### `GET /partner`

Response:

```json
{
  "id": "user-2",
  "status": "AVAILABLE",
  "statusExpiration": null,
  "statusDuration": null,
  "partnerId": "user-1"
}
```

#### `DELETE /partner`

Response: `204 No Content`

Current realtime effect:

- if the partner has an active websocket, it receives `PARTNER_UNLINKED`

### `PATCH /status`

Request:

```json
{
  "status": "BUSY"
}
```

Response:

```json
{
  "status": "BUSY",
  "userId": "user-1",
  "expiration": null,
  "duration": null
}
```

Supported states:

- `AVAILABLE`
- `BUSY`
- `OFFLINE`

### WebSocket events

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

### Device FCM tokens

#### `PUT /devices/fcm-token`

Request:

```json
{
  "deviceId": "android-device-1",
  "fcmToken": "fcm-token-value",
  "platform": "android",
  "appVersion": "1.0.0"
}
```

Response:

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

#### `DELETE /devices/fcm-token/{deviceId}`

Response: `204 No Content`

## Relevant behaviors already implemented

State change:

- if the partner does not exist, the state is persisted and nothing is notified
- if there is an active websocket, realtime is attempted
- if realtime does not deliver, push is used
- if there are no active tokens, the flow does not fail
- if a specific token fails, it continues with the others
- if notification fails, the already persisted state change is not reverted

Unlinking:

- if the user has no partner, it returns a domain error
- if the partner has an active websocket, it receives `PARTNER_UNLINKED`
- there is no push fallback implemented for unlinking

## Real pending items

- implement push reception in Android
- define the final UX when opening the app from a push
- decide whether unlinking will also need push fallback
- add delivery observability if the project requires it
