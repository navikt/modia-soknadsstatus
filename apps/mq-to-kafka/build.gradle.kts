import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val modia_common_version: String by project
val logback_version: String by project
val kafka_version: String by project
val jms_api_version: String by project
val junit_version: String by project

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"
}

dependencies {
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("org.apache.kafka:kafka-clients:$kafka_version")
    implementation("no.nav.personoversikt:ktor-utils:$modia_common_version")
    implementation("no.nav.personoversikt:kotlin-utils:$modia_common_version")
    implementation("javax.jms:javax.jms-api:$jms_api_version")
    implementation(project(":common:kafka"))
    implementation(project(":common:jms"))
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

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName.set("app")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest {
        attributes["Implementation-Title"] = "MQ to Kafka"
        attributes["Implementation-Version"] = archiveVersion
        attributes["Main-Class"] = "no.nav.modia.soknadsstatus.MainKt"
    }
    exclude(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA"
    )
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
