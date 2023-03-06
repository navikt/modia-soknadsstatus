import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.expediagroup.graphql.plugin.gradle.config.GraphQLScalar
import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLDownloadSDLTask
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask

val ktor_version: String by project
val kotlinx_serialization_version: String by project
val modia_common_version: String by project
val logback_version: String by project
val junit_version: String by project
val postgres_version: String by project
val graphql_version: String by project

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"
    id("com.expediagroup.graphql") version "6.4.0"
}

dependencies {
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")
    implementation("org.apache.kafka:kafka-streams:3.3.1")
    implementation("no.nav.personoversikt:ktor-utils:$modia_common_version")
    implementation("no.nav.personoversikt:logging:$modia_common_version")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.flywaydb:flyway-core:9.8.3")
    implementation(project(":common:kafka-stream-transformer"))
    implementation(project(":common:dataformat"))
    implementation(project(":common:ktor"))
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("com.expediagroup:graphql-kotlin-client:$graphql_version")
    implementation("no.nav.common:sts:2.2023.01.10_13.49-81ddc732df3a")
    testImplementation("org.junit.jupiter:junit-jupiter:$junit_version")
}

group = "no.nav.modia.soknadsstatus"
version = ""

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
    sourceSets {
//        main.kotlin.srcDirs += "build/generated/source/graphql"
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName.set("app")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest {
        attributes["Implementation-Title"] = "Arena Søknadstatus Transformer"
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

val downloadPDLSchema by tasks.creating(GraphQLDownloadSDLTask::class) {
    endpoint.set("https://navikt.github.io/pdl/pdl-api-sdl.graphqls")
    outputFile.set(file("${project.projectDir}/src/main/resources/pdl/schema.graphqls"))
}

val generatePDLClient by tasks.creating(GraphQLGenerateClientTask::class) {
    packageName.set("no.nav.api.generated.pdl")
    schemaFile.set(downloadPDLSchema.outputFile)
    queryFiles.from(file("${project.projectDir}/src/main/resources/pdl/schema.graphqls"))
    serializer.set(GraphQLSerializer.KOTLINX)
    customScalars.add(
        GraphQLScalar(
            "Long",
            "no.nav.api.pdl.converters.PdlLong",
            "no.nav.api.pdl.converters.LongScalarConverter"
        )
    )
    customScalars.add(
        GraphQLScalar(
            "Date",
            "kotlinx.datetime.LocalDate",
            "no.nav.api.pdl.converters.DateScalarConverter"
        )
    )
    customScalars.add(
        GraphQLScalar(
            "DateTime",
            "kotlinx.datetime.LocalDateTime",
            "no.nav.api.pdl.converters.DateTimeScalarConverter"
        )
    )
    dependsOn("downloadPDLSchema")
}