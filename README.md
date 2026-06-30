# ⚙️ Signus Backend

Backend API for a real-time system built with Kotlin and Ktor.

This service manages authentication, user relationships, shared state, and **real-time communication with fallback delivery** using WebSockets and Firebase Cloud Messaging (FCM).

---

## 🚀 Project Summary

This backend is the **core of the Signus system**, responsible for:

* user authentication and identity
* linking between two users
* shared "semaphore" state (`AVAILABLE`, `BUSY`, `OFFLINE`)
* real-time event delivery via WebSocket
* fallback delivery via FCM when realtime is not available

---

## 🌐 Part of the Signus Ecosystem

Signus is structured as a multi-repository system:

* [signus_app](https://github.com/E-delSol/signus_app) — Android client
* signus_back — Backend API (this repository)
* [signus_infra](https://github.com/E-delSol/signus_infra) — Infrastructure and deployment

---

## 🧩 What this project demonstrates

* Designing a backend for **real-time user-to-user communication**
* Implementing **WebSocket-based event delivery**
* Building a **fallback system (WebSocket → Push notifications)**
* Managing **state synchronization between distributed clients**
* Structuring a backend using **feature-based architecture**
* Integrating **JWT authentication, persistence, and messaging systems**

---

## 🏗️ Architecture

The backend follows a feature-based modular structure:

```text id="k1c5cx"
routes → services → repositories → database
```

Project layout:

```text id="7ehg8g"
src/main/kotlin/
  core/
  features/
    auth/
    devicetoken/
    linking/
    notification/
    semaphore/
    user/
```

### Responsibilities

* **routes** → HTTP / WebSocket endpoints, validation, auth extraction
* **services** → business logic and orchestration
* **repositories** → persistence and DB mapping
* **core** → shared infrastructure (config, DI, security, DB)

---

## ⚡ Real-Time Delivery Model

This system is built around a **hybrid real-time communication strategy**.

### Primary channel: WebSocket

* Each authenticated user can open a WebSocket session:

  ```
  WS /ws?token=<jwt>
  ```

* The backend pushes events directly to connected clients

---

### Fallback: FCM (Firebase Cloud Messaging)

If real-time delivery is not possible:

* no active WebSocket session
* app is in background
* connection lost

👉 the backend **automatically falls back to push notifications via FCM**

---

### Key design insight

* WebSocket = **low-latency realtime**
* FCM = **delivery guarantee when realtime is unavailable**

👉 This ensures the system remains consistent even when clients are offline.

---

## 🔌 Event System

The backend emits domain events to clients:

### PARTNER_STATUS_CHANGED

```json id="w4y0cx"
{
  "type": "PARTNER_STATUS_CHANGED",
  "partnerId": "user-1",
  "status": "AVAILABLE",
  "statusExpiration": null,
  "timestamp": 1710930000000
}
```

### PARTNER_UNLINKED

```json id="2mcm7c"
{
  "type": "PARTNER_UNLINKED",
  "partnerId": "user-1",
  "timestamp": 1710930000000
}
```

---

## 🔄 Core Flows

### Linking

1. User A creates a linking session
2. Backend generates `linkCode`
3. User B confirms the session
4. Backend links both users

---

### State Change

1. Client calls `PATCH /status`
2. Backend persists the new state
3. Backend attempts WebSocket delivery
4. If it fails → fallback to FCM

---

### Unlinking

1. Client calls `DELETE /partner`
2. Backend removes relationship
3. Emits `PARTNER_UNLINKED` event if possible

---

## 🧰 Technical Stack

* **Kotlin**
* **Ktor**
* **PostgreSQL**
* **Exposed**
* **Flyway**
* **Koin**
* **JWT**
* **WebSockets**
* **Firebase Cloud Messaging (FCM)**
* **Docker / Docker Compose**
* **Testcontainers**

---

## 🔐 Security

* JWT-based authentication

* `POST /auth/register` and `POST /auth/login` return:

  * short-lived `accessToken`
  * long-lived `refreshToken`

* `POST /auth/refresh` exchanges a valid refresh token for a new access token

* `POST /auth/logout` revokes the refresh token for the current session

* Protected endpoints require:

  ```
  Authorization: Bearer <jwt>
  ```

* WebSocket authentication via query param

* Secrets managed via environment variables

---

## ⚙️ Configuration

Copy:

```bash id="w7y9mb"
cp .env.example .env
```

Configure:

* database credentials
* JWT configuration
* FCM server key
* port

---

## ▶️ Local Execution

### Full Docker

```bash id="xrf8u1"
docker compose up --build
```

### Hybrid

```bash id="s6p3mi"
docker compose up -d db
./gradlew run
```

---

## 📡 API Overview

Base URL:

* HTTP → `http://localhost:8080`
* WS → `ws://localhost:8080`

Main domains:

* authentication (`/auth`)
* linking (`/linking`)
* user (`/me`, `/partner`)
* status (`/status`)
* device tokens (`/devices/fcm-token`)
* realtime (`/ws`)

---

## 🧠 Engineering Highlights

* Real-time backend with **graceful degradation**
* Clear separation of concerns across layers
* Explicit handling of connection vs delivery guarantees
* WebSocket + FCM hybrid strategy
* Feature-based architecture for scalability
* Backend-driven state consistency

---

## 📄 License

This project is **source-available but not open source**.

You may view and study the code, but you are not allowed to:

* use it for commercial purposes
* deploy it as a service
* build competing products

See [LICENSE](LICENSE) for details.

---

## 👤 Author

E-delSol

---
