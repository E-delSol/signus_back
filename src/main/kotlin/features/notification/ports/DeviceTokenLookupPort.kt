package com.pecadoartesano.features.notification.ports

interface DeviceTokenLookupPort {
    fun findActiveFcmTokensByUserId(userId: String): List<String>
}
