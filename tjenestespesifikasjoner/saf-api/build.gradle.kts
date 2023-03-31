import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLDownloadSDLTask
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask

val nav_common_version: String by project
val graphql_version: String by project
val kotlinx_datetime_version: String by project

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "1.7.21"
    id("com.expediagroup.graphql") version "6.4.0"
}

dependencies {
    implementation("com.expediagroup:graphql-kotlin-client:$graphql_version")
    implementation("com.expediagroup:graphql-kotlin-ktor-client:$graphql_version")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinx_datetime_version")
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

val downloadSAFSchema by tasks.creating(GraphQLDownloadSDLTask::class) {
    endpoint.set("https://navikt.github.io/saf/saf-api-sdl.graphqls")
    outputFile.set(file("${project.projectDir}/src/main/resources/saf/schema.graphqls"))
}
val generateSAFClient by tasks.creating(GraphQLGenerateClientTask::class) {
    dependsOn(downloadSAFSchema)
    packageName.set("no.nav.api.generated.saf")
    schemaFile.set(downloadSAFSchema.outputFile)
    queryFiles.from(fileTree("${project.projectDir}/src/main/resources/saf/queries/").files)
    serializer.set(GraphQLSerializer.KOTLINX)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
    dependsOn(generateSAFClient)
}

tasks.named("processResources").configure { dependsOn(downloadSAFSchema) }
