import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "2.0.21"
    alias(libs.plugins.openapi)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.common.rest)
    implementation(libs.bundles.jackson)
    implementation(libs.kotlin.stdlib.jdk8)
}

group = "no.nav.modia.soknadsstatus"
description = "tilgangsmaskinen"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
    dependsOn("openApiGenerate")
}

openApiGenerate {
    inputSpec.set("$projectDir/src/main/resources/tilgangsmaskinen/openapi.json")
    generatorName.set("kotlin")
    library.set("jvm-okhttp4")
    packageName.set("no.nav.modia.soknadsstatus.consumer.tilgangsmaskinen.generated")
    templateDir.set("$projectDir/../openapi-templates")
    outputDir.set("$buildDir/generated-sources")
    configOptions.set(
        mapOf(
            "enumPropertyNaming" to "original",
            "serializationLibrary" to "jackson",
        ),
    )
    additionalProperties.set(
        mapOf(
            "generateWithHttpInfo" to true,
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
