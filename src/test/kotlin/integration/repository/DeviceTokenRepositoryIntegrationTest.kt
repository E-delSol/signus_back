package integration.repository

import com.pecadoartesano.features.devicetoken.DeviceToken
import com.pecadoartesano.features.devicetoken.DeviceTokenRepository
import com.pecadoartesano.features.user.User
import com.pecadoartesano.features.user.UserRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import support.PostgresIntegrationSupport

class DeviceTokenRepositoryIntegrationTest {

    private val userRepository = UserRepository()
    private val tokenRepository = DeviceTokenRepository()

    @BeforeTest
    fun setUp() {
        PostgresIntegrationSupport.ensureDockerAndDatabase()
        PostgresIntegrationSupport.cleanDatabase()
        userRepository.create(
            User(
                id = "user-1",
                email = "user1@test.com",
                passwordHash = "hash-1",
                displayName = "User One",
                createdAt = 1L
            )
        )
    }

    @Test
    fun `given active token when deactivateByFcmToken then sets inactive with reason and timestamp`() {
        // Given
        val now = 1000L
        val result = tokenRepository.upsertForUserDevice(
            userId = "user-1",
            deviceId = "device-1",
            fcmToken = "fcm-token-1",
            platform = "android",
            appVersion = null,
            nowMillis = now
        )
        assertTrue(result.created)

        // When
        val deactivated = tokenRepository.deactivateByFcmToken("fcm-token-1", "test-reason")

        // Then
        assertNotNull(deactivated)
        assertEquals("fcm-token-1", deactivated.fcmToken)
        assertEquals(false, deactivated.active)
        assertEquals("test-reason", deactivated.deactivationReason)
        assertNotNull(deactivated.deactivatedAt)
    }

    @Test
    fun `given inactive token when deactivateByFcmToken then returns null`() {
        // Given — create and then deactivate
        val now = 1000L
        tokenRepository.upsertForUserDevice(
            userId = "user-1",
            deviceId = "device-1",
            fcmToken = "fcm-token-2",
            platform = "android",
            appVersion = null,
            nowMillis = now
        )
        tokenRepository.deactivateForUserDevice("user-1", "device-1", 2000L)

        // When — try to deactivate already inactive token
        val result = tokenRepository.deactivateByFcmToken("fcm-token-2", "test-reason")

        // Then
        assertNull(result)
    }

    @Test
    fun `given unknown fcm token when deactivateByFcmToken then returns null`() {
        // When
        val result = tokenRepository.deactivateByFcmToken("nonexistent-token", "test-reason")

        // Then
        assertNull(result)
    }

    @Test
    fun `given tokens with different ages when findActiveTokensOlderThan then returns only old tokens`() {
        // Given — create old token
        tokenRepository.upsertForUserDevice(
            userId = "user-1",
            deviceId = "device-old",
            fcmToken = "fcm-token-old",
            platform = "android",
            appVersion = null,
            nowMillis = 100L
        )
        // Given — create recent token
        tokenRepository.upsertForUserDevice(
            userId = "user-1",
            deviceId = "device-recent",
            fcmToken = "fcm-token-recent",
            platform = "android",
            appVersion = null,
            nowMillis = 5000L
        )
        // Given — create inactive token (should not appear in active query)
        tokenRepository.upsertForUserDevice(
            userId = "user-1",
            deviceId = "device-inactive",
            fcmToken = "fcm-token-inactive",
            platform = "android",
            appVersion = null,
            nowMillis = 50L
        )
        tokenRepository.deactivateForUserDevice("user-1", "device-inactive", 3000L)

        // When
        val threshold = 2000L
        val oldTokens = tokenRepository.findActiveTokensOlderThan(threshold)

        // Then
        assertEquals(1, oldTokens.size)
        assertEquals("fcm-token-old", oldTokens[0].fcmToken)
    }

    @Test
    fun `given no old tokens when findActiveTokensOlderThan then returns empty list`() {
        // Given — create only recent token
        tokenRepository.upsertForUserDevice(
            userId = "user-1",
            deviceId = "device-recent",
            fcmToken = "fcm-token-recent",
            platform = "android",
            appVersion = null,
            nowMillis = 5000L
        )

        // When
        val threshold = 1000L
        val oldTokens = tokenRepository.findActiveTokensOlderThan(threshold)

        // Then
        assertTrue(oldTokens.isEmpty())
    }
}
