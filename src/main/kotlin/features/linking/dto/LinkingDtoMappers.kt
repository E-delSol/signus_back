package com.pecadoartesano.features.linking.dto

import com.pecadoartesano.features.linking.LinkSessionStatus
import java.time.Instant
import java.util.UUID

fun toCreateResponse(sessionId: UUID, linkCode: String, expiresAt: Instant): CreateLinkSessionResponse =
    CreateLinkSessionResponse(
        sessionId = sessionId.toString(),
        linkCode = linkCode,
        expiresAt = expiresAt.toString()
    )

fun toStatusResponse(sessionId: UUID, status: LinkSessionStatus): LinkSessionStatusResponse =
    LinkSessionStatusResponse(
        sessionId = sessionId.toString(),
        status = status
    )
