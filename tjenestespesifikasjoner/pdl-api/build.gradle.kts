import com.expediagroup.graphql.plugin.gradle.config.GraphQLScalar
import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLDownloadSDLTask
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "2.1.20"
    alias(libs.plugins.graphql)
    kotlin("plugin.serialization") version "2.1.20"
}

dependencies {
    implementation(libs.bundles.graphql)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

sourceSets {
    main {
        kotlin {
            srcDir("${layout.buildDirectory}/generated/source/graphql/main/")
        }
    }
}

val downloadPDLSchema by tasks.creating(GraphQLDownloadSDLTask::class) {
    endpoint.set("https://navikt.github.io/pdl/pdl-api-sdl.graphqls")
    outputFile.set(file("${project.projectDir}/src/main/resources/pdl/schema.graphqls"))
}

val generatePDLClient by tasks.creating(GraphQLGenerateClientTask::class) {
    dependsOn(downloadPDLSchema)
    packageName.set("no.nav.api.generated.pdl")
    schemaFile.set(downloadPDLSchema.outputFile)
    queryFiles.from(fileTree("${project.projectDir}/src/main/resources/pdl/queries/").files)
    serializer.set(GraphQLSerializer.KOTLINX)
    customScalars.add(
        GraphQLScalar(
            "Long",
            "no.nav.api.pdl.converters.PdlLong",
            "no.nav.api.pdl.converters.LongScalarConverter",
        ),
    )
    customScalars.add(
        GraphQLScalar(
            "Date",
            "kotlinx.datetime.LocalDate",
            "no.nav.api.pdl.converters.DateScalarConverter",
        ),
    )
    customScalars.add(
        GraphQLScalar(
            "DateTime",
            "kotlinx.datetime.LocalDateTime",
            "no.nav.api.pdl.converters.DateTimeScalarConverter",
        ),
    )
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
    dependsOn("generatePDLClient")
}

tasks.named("processResources").configure { dependsOn("downloadPDLSchema") }
