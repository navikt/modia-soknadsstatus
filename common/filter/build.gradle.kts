import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinx_serialization_version: String by project
val kotlinx_datetime_version: String by project
val modia_common_version: String by project
val test_containers_version: String by project
val postgres_version: String by project
val junit_version: String by project

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.7.21"
}
dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":common:dataformat"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinx_serialization_version")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinx_datetime_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")
    testImplementation("org.testcontainers:junit-jupiter:$test_containers_version")
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
tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
