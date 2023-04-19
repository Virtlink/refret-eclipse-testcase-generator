package net.pelsmaeker.generator.stage2

import net.pelsmaeker.generator.TestKind
import net.pelsmaeker.generator.TestSuite
import net.pelsmaeker.generator.utils.overwritingBufferedWriter
import net.pelsmaeker.generator.utils.writeTestContent
import net.pelsmaeker.generator.utils.writeln
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path

/**
 * Generates a test case.
 */
object SptTestGenerator {

    /**
     * Writes the test suite as a SPT test in the specified directory.
     *
     * @param modulePrefix the module prefix, such as `"refret/intellij"`; should match the end of the output directory
     * @param submoduleName the submodule name, such as `"analysis"`; or `null`. Used to make a subdirectory in the output directory.
     * @param suite the test suite
     * @param outputDirectory the directory to write the test file to
     * @param force whether to force overwriting existing files
     * @param kinds the kinds of tests to write; or empty to write all tests
     * @return the path to which the file was written
     */
    fun writeToFile(modulePrefix: String, submoduleName: String?, suite: TestSuite, outputDirectory: Path, force: Boolean, vararg kinds: TestKind): Path {
        val testDir = (submoduleName?.let { outputDirectory.resolve(it) } ?: outputDirectory).resolve(suite.directory)
        Files.createDirectories(testDir)
        val destinationPath = testDir.resolve(suite.name + ".spt")
        destinationPath.overwritingBufferedWriter(force).use { writer ->
            generate(modulePrefix, submoduleName, suite, writer, kinds.asList())
        }
        return destinationPath
    }

    /**
     * Writes an SPT test file for the specific test.
     *
     * @param modulePrefix the module prefix, such as `"refret/intellij"`
     * @param submoduleName the submodule name, such as `"analysis"`; or `null`
     * @param suite the test suite
     * @param writer the writer to write to
     * @param kinds the kinds of tests to write; or empty to write all tests
     */
    private fun generate(modulePrefix: String, submoduleName: String?, suite: TestSuite, writer: Writer, kinds: List<TestKind>): Unit = writer.run {
        writeModule(modulePrefix, submoduleName, suite) {
            for (case in suite.cases.filter { it.isAcceptable(kinds) }) {
                case.write(this)
            }
        }
    }

    /**
     * Writes a module to the specified writer.
     *
     * @param modulePrefix the module prefix, such as `"refret/intellij"`
     * @param submoduleName the submodule name, such as `"analysis"`; or `null`
     * @param suite the test suite
     * @param block the code that writes the module contents
     */
    private fun Writer.writeModule(modulePrefix: String, submoduleName: String?, suite: TestSuite, block: Writer.() -> Unit) {
        val moduleName = listOfNotNull(modulePrefix, submoduleName, suite.directory.takeIf { it.isNotBlank() }, suite.name).joinToString("/").replace("//", "/")
        writeln("module $moduleName")
        block()
        writeln()
    }
}