package com.pecadoartesano.core.di

import com.pecadoartesano.core.config.AppConfig
import com.pecadoartesano.core.config.FcmConfig
import com.pecadoartesano.core.config.JwtConfig
import com.pecadoartesano.core.security.JwtService
import com.pecadoartesano.core.security.PasswordService
import com.pecadoartesano.features.auth.AuthServiceImpl
import com.pecadoartesano.features.auth.ports.AuthService
import com.pecadoartesano.features.auth.ports.AuthUserRepositoryPort
import com.pecadoartesano.features.devicetoken.DeviceTokenRepository
import com.pecadoartesano.features.devicetoken.DeviceTokenServiceImpl
import com.pecadoartesano.features.devicetoken.ports.DeviceTokenRepositoryPort
import com.pecadoartesano.features.devicetoken.ports.DeviceTokenService
import com.pecadoartesano.features.linking.LinkSessionRepository
import com.pecadoartesano.features.linking.LinkingServiceImpl
import com.pecadoartesano.features.linking.ports.LinkSessionRepositoryPort
import com.pecadoartesano.features.linking.ports.LinkingService
import com.pecadoartesano.features.linking.ports.LinkingUserRepositoryPort
import com.pecadoartesano.features.notification.NotificationOrchestrator
import com.pecadoartesano.features.notification.PartnerPushNotificationService
import com.pecadoartesano.features.notification.PushProvider
import com.pecadoartesano.features.notification.RealtimeNotificationServiceImpl
import com.pecadoartesano.features.notification.ports.DeviceTokenLookupPort
import com.pecadoartesano.features.notification.ports.PartnerLookupPort
import com.pecadoartesano.features.notification.ports.RealtimeNotificationService
import com.pecadoartesano.features.notification.providers.FcmPushProvider
import com.pecadoartesano.features.semaphore.SemaphoreRepository
import com.pecadoartesano.features.semaphore.StatusServiceImpl
import com.pecadoartesano.features.semaphore.ports.SemaphoreRepositoryPort
import com.pecadoartesano.features.semaphore.ports.StatusService
import com.pecadoartesano.features.user.UserRepository
import com.pecadoartesano.features.user.UserServiceImpl
import com.pecadoartesano.features.user.ports.UserService
import org.koin.core.module.Module
import org.koin.dsl.module

fun appModules(appConfig: AppConfig): List<Module> = listOf(
    module {
        single<AppConfig> { appConfig }
        single<JwtConfig> { appConfig.jwt }
        single<FcmConfig> { appConfig.fcm }

        single { UserRepository() }
        single<AuthUserRepositoryPort> { get<UserRepository>() }
        single<PartnerLookupPort> { get<UserRepository>() }
        single { SemaphoreRepository() }
        single<SemaphoreRepositoryPort> { get<SemaphoreRepository>() }
        single { LinkSessionRepository() }
        single<LinkSessionRepositoryPort> { get<LinkSessionRepository>() }
        single<LinkingUserRepositoryPort> { get<UserRepository>() }
        single { DeviceTokenRepository() }
        single<DeviceTokenRepositoryPort> { get<DeviceTokenRepository>() }
        single<DeviceTokenLookupPort> { get<DeviceTokenRepository>() }

        single { PasswordService() }
        single { JwtService(get()) }
        single<AuthService> { AuthServiceImpl(get(), get(), get()) }
        single<LinkingService> { LinkingServiceImpl(get(), get()) }
        single<UserService> { UserServiceImpl(get(), get(), get()) }
        single<DeviceTokenService> { DeviceTokenServiceImpl(get()) }

        single<RealtimeNotificationService> { RealtimeNotificationServiceImpl() }
        single<PushProvider> { FcmPushProvider(serverKey = get<FcmConfig>().serverKey) }
        single { PartnerPushNotificationService(get(), get()) }
        single { NotificationOrchestrator(get(), get(), get()) }
        single<StatusService> { StatusServiceImpl(get(), get()) }
    }
)
