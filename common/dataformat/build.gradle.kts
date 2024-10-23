import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinx_serialization_version: String by project
val kotlinx_datetime_version: String by project
val test_containers_version: String by project
val postgres_version: String by project
val junit_version: String by project
val kafka_version: String by project
val modia_common_version: String by project

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
    implementation(project(":tjenestespesifikasjoner:pdl-api"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinx_serialization_version")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinx_datetime_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")
    implementation("org.apache.kafka:kafka-streams:$kafka_version")
    implementation("org.flywaydb:flyway-core:10.20.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.20.0")
    implementation("com.zaxxer:HikariCP:6.0.0")
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("com.github.navikt.modia-common-utils:ktor-utils:$modia_common_version")
    testImplementation("org.testcontainers:junit-jupiter:$test_containers_version")
    testImplementation("org.testcontainers:postgresql:$test_containers_version")
    testImplementation("org.junit.jupiter:junit-jupiter:$junit_version")
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
tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
