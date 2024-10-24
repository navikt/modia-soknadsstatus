import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kafka_version: String by project
val modia_common_version: String by project
val logback_version: String by project
val junit_version: String by project
val mockk_version: String by project
val test_containers_version: String by project
val postgres_version: String by project
val flyway_version: String by project
val slack_client_version: String by project

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":common:utils"))
    implementation(project(":common:ktor"))
    implementation(project(":common:dataformat"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("com.github.navikt.modia-common-utils:logging:$modia_common_version")
    implementation("com.github.navikt.modia-common-utils:ktor-utils:$modia_common_version")
    implementation("org.apache.kafka:kafka-clients:$kafka_version")
    implementation("org.apache.kafka:kafka-streams:$kafka_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("org.postgresql:postgresql:$postgres_version")
    testImplementation("com.zaxxer:HikariCP:6.0.0")
    testImplementation("org.flywaydb:flyway-core:$flyway_version")
    testImplementation("org.flywaydb:flyway-database-postgresql:$flyway_version")
    testImplementation("org.junit.jupiter:junit-jupiter:$junit_version")
    testImplementation("io.mockk:mockk:$mockk_version")
    testImplementation("org.testcontainers:junit-jupiter:$test_containers_version")
    testImplementation("org.testcontainers:postgresql:$test_containers_version")

    implementation("com.slack.api:slack-api-client:$slack_client_version")
    implementation("com.slack.api:slack-api-model-kotlin-extension:$slack_client_version")
    implementation("com.slack.api:slack-api-client-kotlin-extension:$slack_client_version")
}

group = "no.nav.modia.soknadsstatus"
version = ""

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
