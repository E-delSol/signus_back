package com.pecadoartesano.features.notification.providers

import com.pecadoartesano.features.notification.NotificationEvent
import com.pecadoartesano.features.notification.ports.NotificationMapper
import com.pecadoartesano.features.notification.ports.NotificationPayload

class FcmNotificationMapper : NotificationMapper {

    override fun toPayload(event: NotificationEvent): NotificationPayload {
        return when (event) {
            is NotificationEvent.PartnerStatusChanged -> toPartnerStatusChangedPayload(event)
            is NotificationEvent.SelfStatusChanged -> toSelfStatusChangedPayload(event)
            is NotificationEvent.PartnerUnlinked -> toPartnerUnlinkedPayload(event)
        }
    }

    private fun toPartnerStatusChangedPayload(event: NotificationEvent.PartnerStatusChanged): NotificationPayload {
        return NotificationPayload(
            title = "Estado actualizado",
            body = "Tu pareja ahora está ${event.status.name}",
            data = mapOf(
                "type" to "PARTNER_STATUS_CHANGED",
                "actorId" to event.actorUserId,
                "status" to event.status.name
            )
        )
    }

    private fun toSelfStatusChangedPayload(event: NotificationEvent.SelfStatusChanged): NotificationPayload {
        return NotificationPayload(
            title = "Estado actualizado",
            body = "Tu estado ahora es ${event.status.name}",
            data = mapOf(
                "type" to "SELF_STATUS_CHANGED",
                "status" to event.status.name
            )
        )
    }

    private fun toPartnerUnlinkedPayload(event: NotificationEvent.PartnerUnlinked): NotificationPayload {
        return NotificationPayload(
            title = "Pareja desvinculada",
            body = "Tu pareja ha sido desvinculada",
            data = mapOf(
                "type" to "PARTNER_UNLINKED",
                "actorId" to event.actorUserId
            )
        )
    }
}
