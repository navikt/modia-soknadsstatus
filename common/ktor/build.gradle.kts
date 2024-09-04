import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val modia_common_version: String by project

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "1.9.10"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("com.github.navikt.modia-common-utils:ktor-utils:$modia_common_version")
    implementation("com.github.navikt.modia-common-utils:kotlin-utils:$modia_common_version")
    implementation("com.github.navikt.modia-common-utils:logging:$modia_common_version")
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
