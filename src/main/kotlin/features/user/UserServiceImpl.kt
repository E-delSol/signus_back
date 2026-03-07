package com.pecadoartesano.features.user

import at.favre.lib.crypto.bcrypt.BCrypt
import com.pecadoartesano.features.semaphore.UserState
import com.pecadoartesano.features.semaphore.ports.SemaphoreRepositoryPort
import com.pecadoartesano.features.user.ports.UserService
import java.util.UUID

class UserServiceImpl(
    private val userRepository: UserRepository,
    private val semaphoreRepository: SemaphoreRepositoryPort
) : UserService {

    override fun register(
        email: String,
        rawPassword: String,
        displayName: String?
    ): User {

        userRepository.findByEmail(email)?.let {
            error("User with email $email already exists.")
        }

        val passwordHash = BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray())

        val user = User(
            id = generateUserId(),
            email = email,
            passwordHash = passwordHash,
            displayName = displayName,
            createdAt = System.currentTimeMillis()
        )

        return userRepository.create(user)
    }

    override fun getCurrentUser(userId: String): UserState {
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User with id $userId was not found")
        val semaphore = semaphoreRepository.findByUserId(userId)

        return UserState(
            id = user.id,
            email = user.email,
            displayName = user.displayName,
            partnerId = user.partnerId,
            status = semaphore?.status,
            statusExpiration = semaphore?.expiration,
            statusDuration = semaphore?.duration
        )
    }

    override fun getCurrentPartner(userId: String): UserState {
        val currentUser = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User with id $userId was not found")
        val partnerId = currentUser.partnerId
            ?: throw IllegalStateException("User has no linked partner")
        val partnerUser = userRepository.findById(partnerId)
            ?: throw IllegalArgumentException("Partner with id $partnerId was not found")
        val partnerSemaphore = semaphoreRepository.findByUserId(partnerUser.id)

        return UserState(
            id = partnerUser.id,
            email = partnerUser.email,
            displayName = partnerUser.displayName,
            partnerId = partnerUser.partnerId,
            status = partnerSemaphore?.status,
            statusExpiration = partnerSemaphore?.expiration,
            statusDuration = partnerSemaphore?.duration
        )
    }

    private fun generateUserId(): String =
        UUID.randomUUID().toString()

}
