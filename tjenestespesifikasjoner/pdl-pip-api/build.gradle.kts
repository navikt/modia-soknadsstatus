val nav_common_version: String by project

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "1.9.23"
    id("org.openapi.generator") version "7.4.0"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("no.nav.common:rest:$nav_common_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.10")
}

group = "no.nav.modia.soknadsstatus"
description = "pdl-pip-api"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
    dependsOn("openApiGenerate")
}

openApiGenerate {
    generatorName.set("kotlin")
    library.set("jvm-okhttp4")
    inputSpec.set("$projectDir/src/main/resources/pdl-pip-api/openapi.json")
    packageName.set("no.nav.modia.soknadsstatus.consumer.pdlPipApi.generated")
    templateDir.set("$projectDir/../openapi-templates")
    outputDir.set("$buildDir/generated-sources")
    configOptions.set(
        mapOf(
            "enumPropertyNaming" to "original",
            "serializationLibrary" to "jackson",
        ),
    )
}

sourceSets {
    main {
        kotlin {
            srcDir("$buildDir/generated-sources/src/main/kotlin")
        }
    }
}
