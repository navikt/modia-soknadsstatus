import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlinx_serialization_version: String by project
val modia_common_version: String by project
val logback_version: String by project
val ibm_mq_version: String by project
val active_mq_version: String by project
val kafka_version: String by project
val junit_version: String by project
val kotlinx_datetime_version: String by project
val prometheus_version: String by project

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinx_datetime_version")
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("org.apache.kafka:kafka-streams:$kafka_version")
    implementation("com.github.navikt.modia-common-utils:kotlin-utils:$modia_common_version")
    implementation(project(":common:ktor"))
    implementation(project(":common:dataformat"))
    implementation(project(":common:kafka"))
    implementation(project(":common:utils"))
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.micrometer:micrometer-registry-prometheus:$prometheus_version")

    testImplementation("org.junit.jupiter:junit-jupiter:$junit_version")
}

group = "no.nav.modia.soknadsstatus"
version = ""

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
