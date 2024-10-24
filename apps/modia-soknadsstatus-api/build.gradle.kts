import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlinx_serialization_version: String by project
val modia_common_version: String by project
val logback_version: String by project
val junit_version: String by project
val postgres_version: String by project
val flyway_version: String by project
val graphql_version: String by project
val nav_common_version: String by project
val mockk_version: String by project
val mock_webserver_version: String by project
val guava_testlib_version: String by project
val logstash_version: String by project
val test_containers_version: String by project

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("com.expediagroup.graphql") version "8.2.0"
    id("com.gradleup.shadow") version "8.3.3"
}

dependencies {
    implementation(project(":common:kafka-stream-transformer"))
    implementation(project(":common:dataformat"))
    implementation(project(":common:ktor"))
    implementation(project(":common:kafka"))
    implementation(project(":common:utils"))
    implementation(project(":tjenestespesifikasjoner:norg-api"))
    implementation(project(":tjenestespesifikasjoner:kodeverk-api"))
    implementation(project(":tjenestespesifikasjoner:skjermede-personer-pip-api"))
    implementation(project(":tjenestespesifikasjoner:pdl-pip-api"))
    implementation(project(":tjenestespesifikasjoner:pdl-api"))
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("io.ktor:ktor-client-okhttp:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")
    implementation("org.apache.kafka:kafka-streams:3.8.0")
    implementation("com.github.navikt.modia-common-utils:ktor-utils:$modia_common_version")
    implementation("com.github.navikt.modia-common-utils:logging:$modia_common_version")
    implementation("com.github.navikt.modia-common-utils:kabac:$modia_common_version")
    implementation("com.zaxxer:HikariCP:6.0.0")
    implementation("org.flywaydb:flyway-core:$flyway_version")
    implementation("org.flywaydb:flyway-database-postgresql:$flyway_version")
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstash_version")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("com.expediagroup:graphql-kotlin-client:$graphql_version")
    implementation("com.expediagroup:graphql-kotlin-ktor-client:$graphql_version")
    implementation("no.nav.common:sts:$nav_common_version")
    implementation("no.nav.common:token-client:$nav_common_version")
    implementation("no.nav.common:client:$nav_common_version")
    testImplementation("org.junit.jupiter:junit-jupiter:$junit_version")
    testImplementation("org.testcontainers:junit-jupiter:$test_containers_version")
    testImplementation("org.testcontainers:postgresql:$test_containers_version")
    testImplementation("io.mockk:mockk-jvm:$mockk_version")
    testImplementation("com.squareup.okhttp3:mockwebserver:$mock_webserver_version")
    testImplementation("com.google.guava:guava-testlib:$guava_testlib_version")
    testImplementation("com.github.navikt.modia-common-utils:kabac:$modia_common_version") {
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
