package com.pecadoartesano.features.user

import com.pecadoartesano.features.auth.ports.AuthUserRepositoryPort
import com.pecadoartesano.features.linking.ports.LinkingUserRepositoryPort
import com.pecadoartesano.features.notification.ports.PartnerLookupPort
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

class UserRepository : AuthUserRepositoryPort, PartnerLookupPort, LinkingUserRepositoryPort {

    fun findById(userId: String): User? = transaction {
        UserTable
            .selectAll()
            .where { UserTable.id eq userId }
            .map(::toUser)
            .singleOrNull()
    }

    override fun findByEmail(email: String): User? = transaction {
        UserTable
            .selectAll()
            .where { UserTable.email eq email }
            .map(::toUser)
            .singleOrNull()
    }

    override fun findPartnerByUserId(userId: String): User? = transaction {
        val partnerId = UserTable
            .selectAll()
            .where { UserTable.id eq userId }
            .map { it[UserTable.partnerId] }
            .singleOrNull()
            ?: return@transaction null

        UserTable
            .selectAll()
            .where { UserTable.id eq partnerId }
            .map(::toUser)
            .singleOrNull()
    }

    override fun create(user: User): User = transaction {
        UserTable.insert {
            it[id] = user.id
            it[email] = user.email
            it[passwordHash] = user.passwordHash
            it[displayName] = user.displayName
            it[partnerId] = user.partnerId
            it[fcmToken] = user.fcmToken
            it[createdAt] = System.currentTimeMillis()
        }
        user
    }

    override fun linkUsers(userAId: UUID, userBId: UUID): Boolean = transaction {
        val userAIdString = userAId.toString()
        val userBIdString = userBId.toString()

        val updatedUserA = UserTable.update({ UserTable.id eq userAIdString }) {
            it[partnerId] = userBIdString
        }

        val updatedUserB = UserTable.update({ UserTable.id eq userBIdString }) {
            it[partnerId] = userAIdString
        }

        if (updatedUserA == 0 || updatedUserB == 0) {
            throw IllegalArgumentException("Cannot link users because one or both users were not found")
        }

        true
    }

    private fun toUser(row: ResultRow): User =
        User(
            id = row[UserTable.id],
            email = row[UserTable.email],
            passwordHash = row[UserTable.passwordHash],
            displayName = row[UserTable.displayName],
            partnerId = row[UserTable.partnerId],
            fcmToken = row[UserTable.fcmToken],
            createdAt = row[UserTable.createdAt]
        )
}
