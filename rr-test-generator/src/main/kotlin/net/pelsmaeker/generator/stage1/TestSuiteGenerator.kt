package net.pelsmaeker.generator.stage1

import net.pelsmaeker.generator.utils.writeln
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.bufferedWriter

/**
 * Generates a test case.
 */
object TestSuiteGenerator {

    /**
     * Writes the Java project as a Test suite in the specified directory.
     *
     * @return the path to which the file was written
     */
    fun writeToFile(javaProject: JavaProject, outputDirectory: Path): Path {
        val testDir = outputDirectory.resolve(javaProject.directory)
        Files.createDirectories(testDir)
        val destinationPath = testDir.resolve(javaProject.name + "_" + javaProject.qualifier + ".java")
        destinationPath.bufferedWriter().use { writer ->
            generate(javaProject.packages, writer)
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
            writeln("[${pkg.name}] {")
            pkg.units.forEach { unit ->
                writeln("[${unit.name}]")
                write("  ")
                writeln(unit.text.prependIndent("  "))
            }
            writeln("}")
        }
    }

}
