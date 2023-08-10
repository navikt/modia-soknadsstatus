import com.expediagroup.graphql.plugin.gradle.config.GraphQLScalar
import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLDownloadSDLTask
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask

val nav_common_version: String by project
val graphql_version: String by project
val kotlinx_datetime_version: String by project
val kotlinx_serialization_version: String by project

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "1.7.21"
    id("com.expediagroup.graphql") version "6.4.0"
    kotlin("plugin.serialization") version "1.8.21"
}

dependencies {
    implementation("com.expediagroup:graphql-kotlin-client:$graphql_version")
    implementation("com.expediagroup:graphql-kotlin-ktor-client:$graphql_version")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinx_datetime_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinx_serialization_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

sourceSets {
    main {
        kotlin {
            srcDir("$buildDir/generated/source/graphql/main/")
        }
    }
}

val downloadPDLSchema by tasks.creating(GraphQLDownloadSDLTask::class) {
    endpoint.set("https://navikt.github.io/pdl/pdl-api-sdl.graphqls")
    outputFile.set(file("${project.projectDir}/src/main/resources/pdl/schema.graphql"))
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
    kotlinOptions.jvmTarget = "11"
    dependsOn("generatePDLClient")
}

tasks.named("processResources").configure { dependsOn("downloadPDLSchema") }
