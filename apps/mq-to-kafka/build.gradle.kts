import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val modia_common_version: String by project
val logback_version: String by project
val kafka_version: String by project
val jms_api_version: String by project
val junit_version: String by project
val logstash_version: String by project
val slack_client_version: String by project
val jaxb_version: String by project

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
}

dependencies {
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("org.apache.kafka:kafka-clients:$kafka_version")
    implementation("com.github.navikt.modia-common-utils:ktor-utils:$modia_common_version")
    implementation("com.github.navikt.modia-common-utils:kotlin-utils:$modia_common_version")
    implementation("com.github.navikt.modia-common-utils:logging:$modia_common_version")
    implementation("jakarta.jms:jakarta.jms-api:3.1.0")
    implementation(project(":common:kafka"))
    implementation(project(":common:jms"))
    implementation(project(":common:ktor"))
    implementation(project(":common:utils"))
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstash_version")
    testImplementation("org.junit.jupiter:junit-jupiter:$junit_version")

    implementation("com.slack.api:slack-api-client:$slack_client_version")
    implementation("com.slack.api:slack-api-model-kotlin-extension:$slack_client_version")
    implementation("com.slack.api:slack-api-client-kotlin-extension:$slack_client_version")
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
            attributes["Implementation-Title"] = "MQ to Kafka"
            attributes["Implementation-Version"] = archiveVersion
            attributes["Main-Class"] = "no.nav.modia.soknadsstatus.MainKt"
        }
        exclude(
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
        )
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        with(tasks.jar.get() as CopySpec)
    }

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
