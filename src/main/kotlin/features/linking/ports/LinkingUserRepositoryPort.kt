package com.pecadoartesano.features.linking.ports

import java.util.UUID

interface LinkingUserRepositoryPort {
    fun linkUsers(userAId: UUID, userBId: UUID): Boolean
}
