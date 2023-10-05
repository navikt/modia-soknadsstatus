import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlinx_datetime_version: String by project
val modia_common_version: String by project
val logback_version: String by project
val junit_version: String by project

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.9.10"
}

dependencies {
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("org.apache.kafka:kafka-streams:3.3.1")
    implementation("com.github.navikt.modia-common-utils:ktor-utils:$modia_common_version")
    implementation(project(":common:kafka"))
    implementation(project(":common:dataformat"))
    implementation(project(":common:jms"))
    implementation(project(":common:kafka-stream-transformer"))
    implementation(project(":common:ktor"))

    implementation("ch.qos.logback:logback-classic:$logback_version")

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

val fatJar =
    task("fatJar", type = Jar::class) {
        archiveBaseName.set("app")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        manifest {
            attributes["Implementation-Title"] = "Data Generator App"
            attributes["Implementation-Version"] = archiveVersion
            attributes["Main-Class"] = "no.nav.modia.soknadsstatus.MainKt"
        }
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        with(tasks.jar.get() as CopySpec)
    }

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
