import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlinx_datetime_version: String by project
val modia_common_version: String by project
val logback_version: String by project
val junit_version: String by project
val logstash_version: String by project

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("com.gradleup.shadow") version "8.3.3"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinx_datetime_version")
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("org.apache.kafka:kafka-streams:3.8.0")
    implementation("com.github.navikt.modia-common-utils:ktor-utils:$modia_common_version")
    implementation("com.github.navikt.modia-common-utils:logging:$modia_common_version")
    implementation(project(":common:ktor"))
    implementation(project(":common:kafka-stream-transformer"))
    implementation(project(":common:dataformat"))
    implementation(project(":common:filter"))
    implementation(project(":common:kafka"))
    implementation(project(":common:utils"))
    implementation(project(":tjenestespesifikasjoner:pdl-api"))
    implementation("net.logstash.logback:logstash-logback-encoder:$logstash_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("org.junit.jupiter:junit-jupiter:$junit_version")
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
