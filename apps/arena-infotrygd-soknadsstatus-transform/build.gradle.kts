import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlinx_datetime_version: String by project
val modia_common_version: String by project
val logback_version: String by project
val junit_version: String by project
val logstash_version: String by project
val jaxb_version: String by project

val jaxb: Configuration by configurations.creating

val schemaDir = "src/main/resources/schema"
val xjcOutputDir = "$buildDir/generated/source/xjc/main"

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinx_datetime_version")
    implementation("org.apache.kafka:kafka-streams:3.8.0")
    implementation("com.github.navikt.modia-common-utils:ktor-utils:$modia_common_version")
    implementation("com.github.navikt.modia-common-utils:logging:$modia_common_version")
    implementation(project(":common:ktor"))
    implementation(project(":common:kafka-stream-transformer"))
    implementation(project(":common:dataformat"))
    implementation(project(":common:filter"))
    implementation(project(":common:kafka"))
    implementation(project(":tjenestespesifikasjoner:pdl-api"))
    implementation("net.logstash.logback:logstash-logback-encoder:$logstash_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-cio-jvm:2.3.12")
    testImplementation("org.junit.jupiter:junit-jupiter:$junit_version")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:$jaxb_version")
    implementation("org.glassfish.jaxb:jaxb-runtime:$jaxb_version")
    jaxb("org.glassfish.jaxb:jaxb-xjc:$jaxb_version")
    jaxb("org.glassfish.jaxb:jaxb-runtime:$jaxb_version")
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

val createXjcOutputDir by tasks.register("createXjcOutputDir") {
    doLast {
        mkdir(xjcOutputDir)
    }
}

val xjc by tasks.registering(JavaExec::class) {
    dependsOn(createXjcOutputDir)
    classpath = jaxb
    mainClass.set("com.sun.tools.xjc.XJCFacade")
    args =
        listOf(
            "-d",
            xjcOutputDir,
            "-p",
            project.group.toString(),
            "-no-header",
            "-quiet",
            schemaDir,
        )
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(xjc)
}

sourceSets {
    main {
        java {
            srcDirs(
                files(xjcOutputDir) {
                    builtBy(xjc)
                },
            )
        }
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
