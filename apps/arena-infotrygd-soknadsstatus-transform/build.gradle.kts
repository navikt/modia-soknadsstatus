import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlinx_datetime_version: String by project
val modia_common_version: String by project
val logback_version: String by project
val junit_version: String by project
val meldingsdefinisjon_version: String by project
val glassfish_jaxb_runtime_version: String by project
val jakarta_xml_bind_version: String by project

plugins {
    application
    id("setup.repository")
    id("org.jlleitschuh.gradle.ktlint") version "11.3.1"
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinx_datetime_version")
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("org.apache.kafka:kafka-streams:3.3.1")
    implementation("no.nav.personoversikt:ktor-utils:$modia_common_version")
    implementation("no.nav.personoversikt:logging:$modia_common_version")
    implementation(project(":common:ktor"))
    implementation(project(":common:kafka-stream-transformer"))
    implementation(project(":common:dataformat"))
    implementation(project(":common:filter"))
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:$jakarta_xml_bind_version")
    implementation("org.glassfish.jaxb:jaxb-runtime:$glassfish_jaxb_runtime_version")

    implementation("com.github.navikt.tjenestespesifikasjoner:nav-virksomhet-hendelsehandterer-behandlingstatus-v1-meldingsdefinisjon:$meldingsdefinisjon_version")

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

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName.set("app")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest {
        attributes["Implementation-Title"] = "Arena og Infotrygd Søknadstatus Transformer"
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