import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinx_serialization_version: String by project
val kotlinx_coroutines_version: String by project
val modia_common_version: String by project
val caffeine_version: String by project
val junit_version: String by project
val guava_testlib_version: String by project

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.7.21"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinx_serialization_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines_version")
    implementation("com.github.navikt.modia-common-utils:logging:$modia_common_version")
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeine_version")
    testImplementation("org.junit.jupiter:junit-jupiter:$junit_version")
    testImplementation("com.google.guava:guava-testlib:$guava_testlib_version")
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
