package net.pelsmaeker.generator.stage1

import java.io.Writer
import java.nio.file.Path
import kotlin.io.path.bufferedWriter

/**
 * Generates a test case.
 */
object TestSuiteGenerator {

    /**
     * Writes the Java project as a Test suite in the specified directory.
     */
    fun writeToFile(javaProject: JavaProject, outputDirectory: Path) {
        outputDirectory.resolve(javaProject.name).bufferedWriter().use { writer ->
            generate(javaProject.packages, writer)
        }
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
