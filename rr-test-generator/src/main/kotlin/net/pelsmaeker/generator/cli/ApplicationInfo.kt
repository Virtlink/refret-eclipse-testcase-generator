package net.pelsmaeker.generator.cli

import java.time.Instant
import java.time.OffsetDateTime
import java.util.*

private const val VERSION_RESOURCE = "/version.properties"

/** Information about the application. */
object ApplicationInfo {

    /** The properties read from `VERSION_RESOURCE`. */
    private val properties: Properties

    init {
        try {
            ApplicationInfo::class.java.getResourceAsStream(VERSION_RESOURCE)!!
        } catch (ex: Throwable) {
            throw RuntimeException("Failed to find resource: $VERSION_RESOURCE.", ex)
        }.use { input ->
            try {
                properties = Properties()
                properties.load(input)
            } catch (ex: Throwable) {
                throw RuntimeException("Failed to load properties.", ex)
            }
        }
    }

    /** The name of the application that is running. */
    val name: String = "rr-test-generator"
    /** The command for running the application. */
    val command: String = "rr-test-generator"
    /** The version of the application that is running. */
    val version: String = properties.getProperty("version")!!
    /** The tag (+ optional commit hash) of the application version that is running. */
    val tag: String = properties.getProperty("tag")!!
    /** The full revision of the application that is running. */
    val revision: String = properties.getProperty("revision")!!
    /** The short revision of the application that is running. */
    val shortRevision: String = properties.getProperty("short-revision")!!
    /** When the application was built. */
    val buildAt: Instant = OffsetDateTime.parse(properties.getProperty("build-time")!!).toInstant()

}