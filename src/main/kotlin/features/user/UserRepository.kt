package com.pecadoartesano.features.user

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class UserRepository {

    fun findByEmail(email: String): User? = transaction {
        UserTable
            .selectAll()
            .where { UserTable.email eq email }
            .map(::toUser)
            .singleOrNull()
    }

    fun create(user: User): User = transaction {
        UserTable.insert {
            it[id] = user.id
            it[email] = user.email
            it[passwordHash] = user.passwordHash
            it[displayName] = user.displayName
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
            createdAt = row[UserTable.createdAt]
        )

}