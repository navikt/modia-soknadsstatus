import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
}

dependencies {
    implementation(project(":common:kafka"))
    implementation(project(":common:dataformat"))
    implementation(project(":common:jms"))
    implementation(project(":common:kafka-stream-transformer"))
    implementation(project(":common:ktor"))

    implementation(libs.bundles.ktorServer)
    implementation(libs.ktor.server.websockets)
    implementation(libs.kafka.streams)

    implementation(libs.logback)

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
