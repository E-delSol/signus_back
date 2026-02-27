package com.pecadoartesano.features.notification

interface PushProvider {
    suspend fun sendPush(targetUserId: String, token: String, title: String, body: String): Boolean
}
