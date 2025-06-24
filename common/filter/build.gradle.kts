import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.2.0"
}
dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":common:dataformat"))

    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.testContainers)
    testImplementation(libs.junit.jupiter)
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
