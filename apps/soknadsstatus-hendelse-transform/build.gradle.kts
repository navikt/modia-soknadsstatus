import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.1.10"
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":common:ktor"))
    implementation(project(":common:kafka-stream-transformer"))
    implementation(project(":common:dataformat"))
    implementation(project(":common:filter"))
    implementation(project(":common:kafka"))
    implementation(project(":common:utils"))
    implementation(project(":tjenestespesifikasjoner:pdl-api"))

    implementation(libs.bundles.ktorServer)
    implementation(libs.bundles.logging)
    implementation(libs.kafka.streams)
    testImplementation(libs.junit.jupiter)
}

group = "no.nav.modia.soknadsstatus"
version = ""

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass.set("no.nav.modia.soknadsstatus.MainKt")
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

tasks {
    shadowJar {
        mergeServiceFiles {
            setPath("META-INF/services/org.flywaydb.core.extensibility.Plugin")
        }
        archiveBaseName.set("app")
        archiveClassifier.set("")
        isZip64 = true
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to "Foreldrepenger Pleiepenger Søknadstatus Transformer",
                    "Implementation-Version" to archiveVersion,
                    "Main-Class" to "no.nav.modia.soknadsstatus.MainKt",
                ),
            )
        }
    }
    "build" {
        dependsOn("shadowJar")
    }
}
