package net.pelsmaeker.generator.stage1

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
        writeln()
        packages.forEach { pkg ->
            writeln("[${pkg.name}] {")
            pkg.units.forEach { unit ->
                writeln("[${unit.name}]")
                writeln(unit.text)
            }
            writeln("}")
        }
    }

    /**
     * Writes a line.
     *
     * @param text the line of text to write
     */
    private fun Writer.writeln(text: String = "") {
        write(text)
        write("\n")
    }

}
