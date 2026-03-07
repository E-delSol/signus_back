package com.pecadoartesano.features.linking.ports

import com.pecadoartesano.features.linking.LinkSession
import java.util.UUID

interface LinkingService {
    fun createSession(ownerUserId: UUID): LinkSession
    fun confirmSessionByLinkCode(linkCode: String, confirmedByUserId: UUID): LinkSession
    fun getSession(sessionId: UUID): LinkSession
}
