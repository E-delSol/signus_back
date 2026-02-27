package com.pecadoartesano.features.notification.ports

import com.pecadoartesano.features.user.User

interface PartnerLookupPort {
    fun findPartnerByUserId(userId: String): User?
}
