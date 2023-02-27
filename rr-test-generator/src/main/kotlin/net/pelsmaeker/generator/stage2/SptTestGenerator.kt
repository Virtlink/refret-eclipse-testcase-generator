package net.pelsmaeker.generator.stage2

import net.pelsmaeker.generator.stage1.JavaProject
import net.pelsmaeker.generator.stage1.TestSuiteGenerator
import net.pelsmaeker.generator.utils.overwritingBufferedWriter
import net.pelsmaeker.generator.utils.replaceAll
import net.pelsmaeker.generator.utils.writeln
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.bufferedWriter

/**
 * Generates a test case.
 */
object SptTestGenerator {


    /**
     * Writes the test suite as a SPT test in the specified directory.
     *
     * @return the path to which the file was written
     */
    fun writeToFile(modulePrefix: String, suite: TestSuite, outputDirectory: Path, force: Boolean): Path {
        val testDir = outputDirectory.resolve(suite.directory)
        Files.createDirectories(testDir)
        val destinationPath = testDir.resolve(suite.name + ".spt")
        destinationPath.overwritingBufferedWriter(force).use { writer ->
            generate(modulePrefix, suite, writer)
        }
        return destinationPath
    }

    /**
     * Writes an SPT test file for the specific test.
     *
     * @param suite the test suite
     * @param writer the writer to write to
     */
    fun generate(modulePrefix: String, suite: TestSuite, writer: Writer): Unit = writer.run {
        writeln("module ${if (modulePrefix.isNotBlank()) "$modulePrefix/" else "" }${if (suite.directory.isNotBlank()) "${suite.directory}/" else "" }${suite.name}")
        writeParsingTest(suite)
        writeAnalysisTest(suite)
        writeReferenceRetentionTests(suite)
        writeln()
    }

    /**
     * Writes a parsing test.
     *
     * @param suite the test suite
     */
    private fun Writer.writeParsingTest(suite: TestSuite) {
        writeln()
        if (suite.isDisabled) {
            writeln("// [[@disabled]]")
            return
        }
        writeln("test parse: ${suite.name} [[")
        writeTestContent(suite)
        writeln("]] parse succeeds")
    }

    /**
     * Writes an analysis test.
     *
     * @param suite the test suite
     */
    private fun Writer.writeAnalysisTest(suite: TestSuite) {
        writeln()
        if (suite.isDisabled) {
            writeln("// [[@disabled]]")
            return
        }
        writeln("test analysis: ${suite.name} [[")
        writeTestContent(suite)
        writeln("]] run test-analyze to SUCCEED()")
    }

    /**
     * Writes the reference retention tests.
     * This is a single test with multiple expectations.
     *
     * @param suite the test suite
     */
    private fun Writer.writeReferenceRetentionTests(suite: TestSuite) {
        suite.cases.forEachIndexed { i, case ->
            writeReferenceRetentionTestCase(suite, case, i)
        }
    }

    /**
     * Writes the reference retention tests.
     * This is a single test with multiple expectations.
     *
     * @param suite the test suite
     */
    private fun Writer.writeReferenceRetentionTestCase(suite: TestSuite, case: TestCase, index: Int) {
        writeln()
        if (suite.isDisabled) {
            writeln("// [[@disabled]]")
            return
        }
        writeln("test refret $index: ${suite.name} [[")
        val refId = suite.identifiers[case.refIndex]
        val declId = suite.identifiers[case.declIndex]
        writeTestContent(suite, listOf(refId, declId), mapOf(refId to case.originalRefText))
        writeln("]] run fix-reference(|#${if (case.refIndex < case.declIndex) 1 else 2}, #${if (case.refIndex < case.declIndex) 2 else 1}) to [[")
        writeTestContent(suite)
        writeln("]]")
    }

    /**
     * Writes the test packages and units.
     *
     * @param suite the test suite
     * @param selections the references and declarations selected by surrounding them with square brackets
     * @param replacements the text to replace (some of) the selected references and declarations with, if any
     */
    private fun Writer.writeTestContent(suite: TestSuite, selections: List<JavaId> = emptyList(), replacements: Map<JavaId, String> = emptyMap()) {
        val orderedSelections = selections.sortedBy { it.range.first }

        // Adjust the text to include the selections
        val newText = suite.expectedText.replaceAll(orderedSelections, JavaId::range) { s, t, _ ->
            "[[${replacements[s] ?: t}]]"
        }
        write("  ")
        writeln(newText.prependIndent("  ").trim())
    }
}