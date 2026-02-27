val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val commons_codec_version: String by project

plugins {
    kotlin("jvm") version "2.3.0"
    id("io.ktor.plugin") version "3.4.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0"
}

group = "com.pecadoartesano"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

////dependencies {
////    implementation("io.ktor:ktor-server-core")
////    implementation("io.ktor:ktor-server-call-logging")
////    implementation("io.ktor:ktor-server-content-negotiation")
////    implementation("io.ktor:ktor-serialization-kotlinx-json")
////    implementation("io.ktor:ktor-server-websockets")
////    implementation("io.ktor:ktor-server-netty")
////    implementation("ch.qos.logback:logback-classic:$logback_version")
////    testImplementation("io.ktor:ktor-server-test-host")
////    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
////}
//dependencies {
//    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
//    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
//    implementation("ch.qos.logback:logback-classic:$logback_version")
//    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
//
//    implementation("commons-codec:commons-codec:$commons_codec_version")
//    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
//
//    implementation("com.zaxxer:HikariCP:7.0.2")
//    implementation("org.flywaydb:flyway-core:9.22.3")
//
//}

dependencies {
    // Ktor core
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
//    implementation(libs.ktor.serialization.gson)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)

    // Logging
    implementation(libs.logback.classic)

    // Database & migration
    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.kotlin.datetime)

    // Utils
    implementation(libs.bcrypt)
    implementation(libs.commons.email)
    implementation(libs.valiktor.core)
    implementation(libs.commons.io)
    implementation(libs.dotenv.kotlin)

    // Swagger / OpenAPI
    implementation(libs.ktor.swagger.ui)
    implementation(libs.ktor.open.api)

    // Dependency injection (Koin)
    implementation(libs.koin.ktor)
    implementation(libs.koin.core)
    implementation(libs.koin.logger)

    // Testing
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.websockets)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.testcontainers.postgresql)
}
