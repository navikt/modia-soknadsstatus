plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "2.1.10"
    alias(libs.plugins.openapi)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.common.rest)
    implementation(libs.bundles.jackson)
    implementation(libs.kotlin.stdlib.jdk8)
}

group = "no.nav.modia.soknadsstatus"
// description = "norg-api"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val openapiJson = "src/main/resources/kodeverk/openapi.json"
val generatedSourcesDir = "$buildDir/generated-sources/openapi"

openApiGenerate {
    inputSpec.set("$projectDir/src/main/resources/kodeverk/openapi.json")
    generatorName.set("kotlin")
    library.set("jvm-okhttp4")
    packageName.set("no.nav.modiasoknadsstatus.consumer.kodeverk.generated")
    modelNameSuffix.set("DTO")
    templateDir.set("$projectDir/../openapi-templates")
    configOptions.set(
        mapOf(
            "useTags" to "true",
            "enumPropertyNaming" to "original",
            "serializationLibrary" to "jackson",
        ),
    )
}

tasks.register("generateApi") {
    group = "build"
    description = "Generate API"
    dependsOn("openApiGenerate")
}

tasks.getByName("build").dependsOn("generateApi")
