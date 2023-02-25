import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.adarshr.gradle.testlogger.theme.ThemeType
import java.util.Properties
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Workaround for IntelliJ issue where `libs` is errored: https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gitversion)
    alias(libs.plugins.benmanesVersions)
    alias(libs.plugins.testlogger)
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "java-library")
    apply(plugin = "com.palantir.git-version")      // Set gitVersion() from last Git repository tag
    apply(plugin = "com.github.ben-manes.versions") // Check for dependency updates
    apply(plugin = "com.adarshr.test-logger")       // Pretty-print test results live to console

    val gitVersion: groovy.lang.Closure<String> by extra

    group = "net.pelsmaeker"
    version = gitVersion()
    description = "refret-eclipse-testcase-generator"

    repositories {
        mavenCentral()
    }

    tasks.test {
        useJUnitPlatform()
        testlogger {
            theme = ThemeType.MOCHA
        }
    }

    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(11))
        withSourcesJar()
        withJavadocJar()
    }


    tasks {
        val createProperties by registering {
            dependsOn("processResources")
            doLast {
                val tag = "git describe --always".runCommand().trim()
                val shortRevision = "git log -n1 --format=%h".runCommand().trim()
                val revision = "git log -n1 --format=%H".runCommand().trim()
                val propertiesDir = "$buildDir/resources/main"
                mkdir(propertiesDir)
                file("$propertiesDir/version.properties").writer().use { w ->
                    val p = Properties()
                    p["version"] = project.version.toString()
                    p["tag"] = tag
                    p["short-revision"] = shortRevision
                    p["revision"] = revision
                    p["build-time"] =
                        ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(
                            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                        )
                    p.store(w, "Version information")
                }
            }
        }

        classes {
            dependsOn(createProperties)
        }
    }
}


gradleEnterprise {
    buildScan {
        // Git Commit ID
        "git rev-parse --verify HEAD".runCommand().takeIf { it.isNotEmpty() }?.let { commitId ->
            value("Git Commit ID", commitId)
        }

        // Git Branch Name
        "git rev-parse --abbrev-ref HEAD".runCommand().takeIf { it.isNotEmpty() }?.let { branchName ->
            value("Git Branch Name", branchName)
        }

        // Git Status
        "git status --porcelain".runCommand().takeIf { it.isNotEmpty() }?.let { status ->
            tag("dirty")
            value("Git Status", status)
        }
    }
}

fun String.runCommand(workingDir: File = file("./")): String {
    val parts = this.split("\\s".toRegex())
    val proc = ProcessBuilder(*parts.toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    proc.waitFor(1, TimeUnit.MINUTES)
    return proc.inputStream.bufferedReader().readText()
}
