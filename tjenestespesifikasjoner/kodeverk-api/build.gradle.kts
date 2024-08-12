val nav_common_version: String by project

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "1.9.23"
    id("org.openapi.generator") version "7.7.0"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("no.nav.common:rest:$nav_common_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0")
}

group = "no.nav.modia.soknadsstatus"
// description = "norg-api"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val openapiJson = "src/main/resources/kodeverk/openapi.json"
val generatedSourcesDir = "$buildDir/generated-sources/openapi"

openApiGenerate {
    inputSpec.set("$projectDir/src/main/resources/kodeverk/openapi.json")
    generatorName.set("kotlin")
    library.set("jvm-okhttp3")
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
