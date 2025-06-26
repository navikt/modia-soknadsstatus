import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
}

dependencies {
    implementation(project(":common:ktor"))
    implementation(project(":common:dataformat"))
    implementation(project(":common:kafka"))
    implementation(project(":common:utils"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.ktor.server.cio)
    implementation(libs.kafka.streams)
    implementation(libs.modia.kotlinUtils)

    implementation(libs.bundles.logging)
    implementation(libs.micrometer.registry)

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

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
