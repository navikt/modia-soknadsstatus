import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("setup.repository")
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.9.10"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.ibm.mq:com.ibm.mq.allclient:9.3.1.0")
    implementation("org.apache.activemq:activemq-jms-pool:5.17.3")
    implementation("org.apache.activemq:activemq-client:5.17.3")
    implementation("org.apache.activemq:activemq-broker:5.17.3")
    implementation(project(":common:ktor"))
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

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
