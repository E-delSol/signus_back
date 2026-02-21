package com.pecadoartesano.features.user

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class UserRepository {

    fun findById(userId: String): User? = transaction {
        UserTable
            .selectAll()
            .where { UserTable.id eq userId }
            .map(::toUser)
            .singleOrNull()
    }

    fun findByEmail(email: String): User? = transaction {
        UserTable
            .selectAll()
            .where { UserTable.email eq email }
            .map(::toUser)
            .singleOrNull()
    }

    fun findPartnerByUserId(userId: String): User? = transaction {
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

    fun create(user: User): User = transaction {
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
