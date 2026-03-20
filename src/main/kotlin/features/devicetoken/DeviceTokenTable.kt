package com.pecadoartesano.features.devicetoken

import org.jetbrains.exposed.v1.core.Table

object DeviceTokenTable : Table("user_device_tokens") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val deviceId = varchar("device_id", 191)
    val fcmToken = varchar("fcm_token", 512)
    val platform = varchar("platform", 20)
    val appVersion = varchar("app_version", 64).nullable()
    val active = bool("active")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    val lastRegisteredAt = long("last_registered_at")
    val deactivatedAt = long("deactivated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
