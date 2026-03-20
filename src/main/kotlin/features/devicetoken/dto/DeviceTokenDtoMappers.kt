package com.pecadoartesano.features.devicetoken.dto

import com.pecadoartesano.features.devicetoken.DeviceToken
import com.pecadoartesano.features.devicetoken.ports.DeviceTokenUpsertResult

fun DeviceToken.toResponse(): DeviceTokenResponse =
    DeviceTokenResponse(
        id = id,
        deviceId = deviceId,
        platform = platform,
        appVersion = appVersion,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastRegisteredAt = lastRegisteredAt,
        deactivatedAt = deactivatedAt
    )

fun DeviceTokenUpsertResult.toResponse(): UpsertDeviceTokenResponse =
    UpsertDeviceTokenResponse(
        created = created,
        token = token.toResponse()
    )
