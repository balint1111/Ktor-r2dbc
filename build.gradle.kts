plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    application
}

group = "com.example"
version = "1.0.0"

application {
    mainClass.set("com.example.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:3.4.0")
    implementation("io.ktor:ktor-server-netty:3.4.0")
    implementation("io.ktor:ktor-server-content-negotiation:3.4.0")
    implementation("io.ktor:ktor-server-openapi:3.4.0")
    implementation("io.ktor:ktor-server-swagger:3.4.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.0")
    
    // R2DBC
    implementation("io.r2dbc:r2dbc-h2:1.1.0.RELEASE")
    implementation("io.r2dbc:r2dbc-pool:1.0.2.RELEASE")
    implementation("io.r2dbc:r2dbc-spi:1.0.0.RELEASE")
    implementation("org.jetbrains.exposed:exposed-core:1.0.0")
    implementation("org.jetbrains.exposed:exposed-r2dbc:1.0.0")
    implementation("org.liquibase:liquibase-core:5.0.1")
    implementation("com.h2database:h2:2.4.240")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.2")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.27")
    
    // Testing
    testImplementation("io.ktor:ktor-server-test-host:3.4.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.3.10")
}

kotlin {
    jvmToolchain(24)
}
