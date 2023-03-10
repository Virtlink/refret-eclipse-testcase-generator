// Workaround for IntelliJ issue where `libs` is errored: https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    java
    `maven-publish`
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

dependencies {
    // CLI
    implementation      (libs.clikt)
    implementation      (libs.mordant)

    // Logging
    implementation      (libs.logback)
    implementation      (libs.kotlinLogging)

    // Testing
    testImplementation  (libs.kotest)
}

tasks.shadowJar {
    archiveFileName.set("labback-docker.${archiveExtension.get()}")
}

application {
    mainClass.set("net.pelsmaeker.generator.cli.MainKt")
}
