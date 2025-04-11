import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.1.20"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":common:utils"))
    implementation(project(":common:ktor"))
    implementation(project(":common:dataformat"))

    implementation(libs.kotlinx.coroutines)

    implementation(libs.modia.logging)
    implementation(libs.modia.ktorUtils)

    implementation(libs.kafka.streams)
    implementation(libs.kafka.clients)
    implementation(libs.bundles.logging)
    implementation(libs.bundles.postgres)

    implementation(libs.bundles.slack)

    testImplementation(libs.testContainers)
    testImplementation(libs.testContainers.postgres)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk.jvm)
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
