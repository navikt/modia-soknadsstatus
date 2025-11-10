import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.2.0"
    alias(libs.plugins.graphql)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":common:kafka-stream-transformer"))
    implementation(project(":common:dataformat"))
    implementation(project(":common:ktor"))
    implementation(project(":common:kafka"))
    implementation(project(":common:utils"))
    implementation(project(":tjenestespesifikasjoner:kodeverk-api"))
    implementation(project(":tjenestespesifikasjoner:pdl-pip-api"))
    implementation(project(":tjenestespesifikasjoner:pdl-api"))
    implementation(project(":tjenestespesifikasjoner:tilgangsmaskinen"))

    implementation(libs.bundles.ktorServer)
    implementation(libs.ktor.client.okhttp)

    implementation(libs.bundles.logging)
    implementation(libs.kafka.streams)
    implementation(libs.bundles.postgres)
    implementation(libs.bundles.graphql)

    implementation(libs.common.sts)
    implementation(libs.common.tokenClient)
    implementation(libs.common.client)

    implementation(libs.modia.kabac)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testContainers)
    testImplementation(libs.testContainers.postgres)
    testImplementation(libs.mockk.jvm)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.guava.testlib)
    testImplementation(libs.modia.kabac) {
        artifact {
            classifier = "tests"
        }
    }
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
                    "Implementation-Title" to "Modia Soknadsstatus API",
                    "Implementation-Version" to archiveVersion,
                    "Main-Class" to "no.nav.modia.soknadsstatus.AppKt",
                ),
            )
        }
    }
    "build" {
        dependsOn("shadowJar")
    }
}
