# Signus – Realtime & Push Notification Architecture

Este documento describe la arquitectura final de sincronización de estado entre dispositivos
y las tareas necesarias para completar la migración desde Firebase hacia un backend propio.

El objetivo es conseguir:

- Sincronización instantánea cuando la app está abierta
- Notificaciones cuando la app está cerrada o en background
- Backend Ktor como fuente de verdad
- Firebase reducido únicamente a transporte de push notifications

---

# Arquitectura final

La comunicación entre dispositivos se realiza mediante dos canales.

## 1. WebSocket (app abierta)

Se utiliza cuando la app está en ejecución.

Flujo:

1. Usuario A cambia su estado.
2. La app envía `PATCH /status` al backend.
3. El backend actualiza el estado en base de datos.
4. El backend envía un evento WebSocket al partner.
5. La app del partner actualiza el disco del semáforo inmediatamente.

Ventajas:

- Latencia mínima
- Sin polling
- Actualización instantánea

---

## 2. FCM Push Notification (app cerrada o en background)

Se utiliza cuando la app del partner no tiene conexión WebSocket activa.

Flujo:

1. Usuario A cambia su estado.
2. Backend guarda el estado.
3. Backend detecta que el partner no tiene sesión WebSocket.
4. Backend envía notificación push mediante FCM.
5. Android muestra la notificación aunque la app esté cerrada.

Ventajas:

- Funciona con la app cerrada
- Entrega fiable del sistema
- Bajo consumo de batería

---

# Responsabilidades del sistema

## Backend (Ktor)

Responsable de:

- Auth
- Linking de dispositivos
- Estado del semáforo
- Persistencia de usuarios
- Gestión de WebSocket
- Envío de notificaciones push
- Sincronización de estados

El backend es la **fuente de verdad del estado**.

---

## App Android

Responsable de:

- UI
- conexión WebSocket
- actualización del estado local
- recepción de notificaciones push
- sincronización del token FCM con backend

---

## Firebase

Firebase queda reducido únicamente a:

- Firebase Cloud Messaging (FCM)

NO se utiliza:

- Firebase Auth
- Firestore
- Realtime Database

---

# Estado actual del proyecto

## Backend

Completado:

- Auth backend
- Linking entre dispositivos
- Endpoint `PATCH /status`
- WebSocket realtime
- Eventos `SemaphoreStatusChangedEvent`
- Notificación al partner

Pendiente:

- Integración de FCM en backend
- Envío de push si el partner no está conectado por WebSocket

---

## App

Completado:

- Eliminación de polling
- WebSocket realtime
- sincronización del semáforo
- actualización de estado tras `PATCH /status`

Pendiente:

- Registrar token FCM en backend
- recibir notificaciones push
- mostrar notificación del sistema

---

# Tareas pendientes

## Fase 1 — Sincronizar token FCM

### App

- obtener token con `FirebaseMessaging`
- enviar token al backend
- refrescar token cuando cambie

### Backend

Crear endpoint:

POST /users/fcm-token

Guardar en base de datos:

- userId
- fcmToken
- deviceId (opcional)
- updatedAt

---

## Fase 2 — Servicio de push en backend

Crear servicio:

PushNotificationService

Responsabilidades:

- enviar notificación FCM
- construir payload
- gestionar errores
- registrar logs de entrega

---

## Fase 3 — Integración con Semaphore

Modificar flujo de cambio de estado:

StatusService.updateStatus()

Nuevo flujo:

updateStatus  
→ guardar estado  
→ notificar partner por websocket  
→ si no hay websocket activo  
→ enviar push FCM

---

## Fase 4 — Recepción de notificaciones en Android

Crear:

FirebaseMessagingService

Responsabilidades:

- recibir push
- mostrar notificación
- reproducir sonido
- abrir app si el usuario toca la notificación

---

# Mejoras futuras

Opcionales:

- reconexión automática de WebSocket
- cola de eventos
- múltiples dispositivos por usuario
- preferencias de notificación
- métricas de entrega
- deduplicación de eventos

---

# Principio clave de arquitectura

El backend es siempre la fuente de verdad.

WebSocket y FCM son únicamente **canales de entrega de eventos**.

Backend  
↓  
Evento  
↓  
WebSocket (foreground)  
FCM (background)

Esto garantiza:

- consistencia
- escalabilidad
- control completo del sistema