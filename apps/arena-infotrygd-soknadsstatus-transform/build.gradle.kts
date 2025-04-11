import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val jaxb: Configuration by configurations.creating

val schemaDir = "src/main/resources/schema"
val xjcOutputDir = "$buildDir/generated/source/xjc/main"

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":common:ktor"))
    implementation(project(":common:kafka-stream-transformer"))
    implementation(project(":common:dataformat"))
    implementation(project(":common:filter"))
    implementation(project(":common:kafka"))
    implementation(project(":tjenestespesifikasjoner:pdl-api"))

    implementation(libs.bundles.ktorServer)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kafka.streams)
    implementation(libs.bundles.logging)
    testImplementation(libs.junit.jupiter)
    implementation(libs.jakarta.bindApi)
    implementation(libs.jaxb.runtime)
    jaxb(libs.jaxb.xjc)
    jaxb(libs.jaxb.runtime)
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
                    "Implementation-Title" to "Arena og Infotrygd Søknadstatus Transformer",
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
