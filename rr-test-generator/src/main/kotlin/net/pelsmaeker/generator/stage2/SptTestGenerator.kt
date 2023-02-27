package net.pelsmaeker.generator.stage2

import net.pelsmaeker.generator.utils.overwritingBufferedWriter
import net.pelsmaeker.generator.utils.replaceAll
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
        writeAnalysisTests(suite)
//        writeReferenceRetentionTests(suite)
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
            writeln("// [[{disabled}]]")
            return
        }
        writeln("test ${suite.name}: parsing [[")
        writeTestContent(suite)
        writeln("]] parse succeeds")
    }

    /**
     * Writes an analysis test.
     *
     * @param suite the test suite
     */
    private fun Writer.writeAnalysisTests(suite: TestSuite) {
        writeln()
        if (suite.isDisabled) {
            writeln("// [[{disabled}]]")
            return
        }
        writeln("test ${suite.name}: default analysis [[")
        writeTestContent(suite)
        writeln("]] analysis succeeds")

        writeln()
        writeln("test ${suite.name}: test analysis [[")
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
            writeln("// [[{disabled}]]")
            return
        }

        // Hack to get the indices in the right order.
        val indices = listOf(
            "ref" to case.refIndex,
            "decl" to case.declIndex,
            "ctx" to case.contextIndex,
        ).sortedWith { a, b ->
            val a = a.second
            val b = b.second
            when {
                a === b -> 0
                a == null -> 1  // sort "ctx" last
                b == null -> -1
                else -> a.compareTo(b)
            }
        }.map { it.first }

        writeln("test ${suite.name}: refret test ${index + 1} [[")
        val refId = suite.identifiers[case.refIndex]
        val declId = suite.identifiers[case.declIndex]
        if (case.contextIndex != null) {
            val ctxId = suite.identifiers[case.contextIndex]
            writeTestContent(suite, listOf(refId, declId, ctxId), mapOf(refId to case.originalRefText))
            writeln("]] run fix-reference(|#${indices.indexOf("ref") + 1}, #${indices.indexOf("decl") + 1}, #${indices.indexOf("ctx") + 1}) to [[")
        } else {
            writeTestContent(suite, listOf(refId, declId), mapOf(refId to case.originalRefText))
            writeln("]] run fix-reference(|#${indices.indexOf("ref") + 1}, #${indices.indexOf("decl") + 1}) to [[")
        }
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
    private fun Writer.writeTestContent(suite: TestSuite, selections: List<Highlight> = emptyList(), replacements: Map<Highlight, String> = emptyMap()) {
        val orderedSelections = selections.sortedBy { it.range.first }

        // Adjust the text to include the selections
        val newText = suite.expectedText.replaceAll(orderedSelections, Highlight::range) { s, t, _ ->
            "[[${replacements[s] ?: t}]]"
        }
        write("  ")
        writeln(newText.prependIndent("  ").trim())
    }
}