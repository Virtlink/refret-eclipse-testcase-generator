package net.pelsmaeker.generator.stage1

import net.pelsmaeker.generator.utils.overwritingBufferedWriter
import net.pelsmaeker.generator.utils.writeln
import java.io.Writer
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.bufferedWriter

/**
 * Generates a test case.
 */
object TestSuiteGenerator {

    /**
     * Writes the Java project as a Test suite in the specified directory.
     *
     * @return the path to which the file was written; or `null` if it was skipped because it already exists
     */
    fun writeToFile(javaProject: JavaProject, outputDirectory: Path, force: Boolean): Path? {
        val testDir = outputDirectory.resolve(javaProject.directory)
        Files.createDirectories(testDir)
        val destinationPath = testDir.resolve(javaProject.name + (javaProject.qualifier?.let { "_$it" } ?: "") + ".java")
        try {
            destinationPath.overwritingBufferedWriter(force).use { writer ->
                generate(javaProject.packages, writer)
            }
        } catch (ex: java.nio.file.FileAlreadyExistsException) {
            return null
        }
        return destinationPath
    }

    /**
     * Writes a test suite file for the specific test.
     *
     * @param packages the Java packages to include
     * @param writer the writer to write to
     */
    fun generate(packages: Iterable<JavaPackage>, writer: Writer): Unit = writer.run {
        writeln("test;")
        packages.forEach { pkg ->

            if (pkg.name.isNotBlank()) writeln("[${pkg.name}] {")
            pkg.units.forEach { unit ->
                writeln("[${unit.name}]")
                write("  ")
                writeln(unit.text.prependIndent("  "))
            }
            if (pkg.name.isNotBlank()) writeln("}")
        }
    }

}

