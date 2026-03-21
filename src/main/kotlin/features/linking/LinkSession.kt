package com.pecadoartesano.features.linking

import java.time.Instant
import java.util.UUID

data class LinkSession(
    val id: UUID,
    val ownerUserId: UUID,
    val linkCode: String,
    val status: LinkSessionStatus,
    val expiresAt: Instant,
    val createdAt: Instant,
    val confirmedAt: Instant? = null,
    val confirmedByUserId: UUID? = null
)
