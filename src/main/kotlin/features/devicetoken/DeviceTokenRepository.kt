package com.pecadoartesano.features.devicetoken

import com.pecadoartesano.features.devicetoken.ports.DeviceTokenRepositoryPort
import com.pecadoartesano.features.devicetoken.ports.DeviceTokenUpsertResult
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

class DeviceTokenRepository : DeviceTokenRepositoryPort {

    override fun upsertForUserDevice(
        userId: String,
        deviceId: String,
        fcmToken: String,
        platform: String,
        appVersion: String?,
        nowMillis: Long
    ): DeviceTokenUpsertResult = transaction {
        DeviceTokenTable.update({
            (DeviceTokenTable.fcmToken eq fcmToken) and
                (DeviceTokenTable.active eq true) and
                ((DeviceTokenTable.userId neq userId) or (DeviceTokenTable.deviceId neq deviceId))
        }) {
            it[active] = false
            it[updatedAt] = nowMillis
            it[deactivatedAt] = nowMillis
        }

        val existing = DeviceTokenTable
            .selectAll()
            .where {
                (DeviceTokenTable.userId eq userId) and
                    (DeviceTokenTable.deviceId eq deviceId)
            }
            .map(::toDeviceToken)
            .singleOrNull()

        if (existing == null) {
            val created = DeviceToken(
                id = UUID.randomUUID().toString(),
                userId = userId,
                deviceId = deviceId,
                fcmToken = fcmToken,
                platform = platform,
                appVersion = appVersion,
                active = true,
                createdAt = nowMillis,
                updatedAt = nowMillis,
                lastRegisteredAt = nowMillis,
                deactivatedAt = null
            )

            DeviceTokenTable.insert {
                it[id] = created.id
                it[DeviceTokenTable.userId] = created.userId
                it[DeviceTokenTable.deviceId] = created.deviceId
                it[DeviceTokenTable.fcmToken] = created.fcmToken
                it[DeviceTokenTable.platform] = created.platform
                it[DeviceTokenTable.appVersion] = created.appVersion
                it[DeviceTokenTable.active] = created.active
                it[DeviceTokenTable.createdAt] = created.createdAt
                it[DeviceTokenTable.updatedAt] = created.updatedAt
                it[DeviceTokenTable.lastRegisteredAt] = created.lastRegisteredAt
                it[DeviceTokenTable.deactivatedAt] = created.deactivatedAt
            }

            DeviceTokenUpsertResult(created, created = true)
        } else {
            val updated = existing.copy(
                fcmToken = fcmToken,
                platform = platform,
                appVersion = appVersion,
                active = true,
                updatedAt = nowMillis,
                lastRegisteredAt = nowMillis,
                deactivatedAt = null
            )

            DeviceTokenTable.update({ DeviceTokenTable.id eq existing.id }) {
                it[DeviceTokenTable.fcmToken] = updated.fcmToken
                it[DeviceTokenTable.platform] = updated.platform
                it[DeviceTokenTable.appVersion] = updated.appVersion
                it[DeviceTokenTable.active] = updated.active
                it[DeviceTokenTable.updatedAt] = updated.updatedAt
                it[DeviceTokenTable.lastRegisteredAt] = updated.lastRegisteredAt
                it[DeviceTokenTable.deactivatedAt] = updated.deactivatedAt
            }

            DeviceTokenUpsertResult(updated, created = false)
        }
    }

    override fun deactivateForUserDevice(userId: String, deviceId: String, nowMillis: Long): Boolean = transaction {
        val updatedRows = DeviceTokenTable.update({
            (DeviceTokenTable.userId eq userId) and
                (DeviceTokenTable.deviceId eq deviceId) and
                (DeviceTokenTable.active eq true)
        }) {
            it[active] = false
            it[updatedAt] = nowMillis
            it[deactivatedAt] = nowMillis
        }

        updatedRows > 0
    }

    override fun listByUserId(userId: String, includeInactive: Boolean): List<DeviceToken> = transaction {
        val query = DeviceTokenTable
            .selectAll()
            .where {
                if (includeInactive) {
                    DeviceTokenTable.userId eq userId
                } else {
                    (DeviceTokenTable.userId eq userId) and (DeviceTokenTable.active eq true)
                }
            }

        query
            .map(::toDeviceToken)
            .sortedByDescending { it.lastRegisteredAt }
    }

    private fun toDeviceToken(row: ResultRow): DeviceToken =
        DeviceToken(
            id = row[DeviceTokenTable.id],
            userId = row[DeviceTokenTable.userId],
            deviceId = row[DeviceTokenTable.deviceId],
            fcmToken = row[DeviceTokenTable.fcmToken],
            platform = row[DeviceTokenTable.platform],
            appVersion = row[DeviceTokenTable.appVersion],
            active = row[DeviceTokenTable.active],
            createdAt = row[DeviceTokenTable.createdAt],
            updatedAt = row[DeviceTokenTable.updatedAt],
            lastRegisteredAt = row[DeviceTokenTable.lastRegisteredAt],
            deactivatedAt = row[DeviceTokenTable.deactivatedAt]
        )
}
