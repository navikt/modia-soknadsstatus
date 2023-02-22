import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlinx_serialization_version: String by project
val modia_common_version: String by project
val logback_version: String by project
val junit_version: String by project
val postgres_version: String by project

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"
}

dependencies {
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")
    implementation("org.apache.kafka:kafka-streams:3.3.1")
    implementation("no.nav.personoversikt:ktor-utils:$modia_common_version")
    implementation("no.nav.personoversikt:logging:$modia_common_version")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.flywaydb:flyway-core:9.8.3")
    implementation(project(":common:kafka-stream-transformer"))
    implementation(project(":common:dataformat"))
    implementation(project(":common:ktor"))
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")
    testImplementation("org.junit.jupiter:junit-jupiter:$junit_version")
}

group = "no.nav.modia.soknadstatus"
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

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName.set("app")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest {
        attributes["Implementation-Title"] = "Arena Søknadstatus Transformer"
        attributes["Implementation-Version"] = archiveVersion
        attributes["Main-Class"] = "no.nav.modia.soknadstatus.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
