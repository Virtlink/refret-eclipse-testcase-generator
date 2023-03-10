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

    /** Specifies the kind of test to generate. */
    enum class TestKind {
        Parsing,
        Analysis,
        ReferenceRetention,
    }

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
            if (kinds.isEmpty() || kinds.contains(TestKind.Parsing)) writeParsingTest(suite)
            if (kinds.isEmpty() || kinds.contains(TestKind.Analysis)) writeAnalysisTests(suite)
            if (kinds.isEmpty() || kinds.contains(TestKind.ReferenceRetention)) writeReferenceRetentionTests(suite)
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

    /**
     * Writes a parsing test.
     *
     * @param suite the test suite
     */
    private fun Writer.writeParsingTest(suite: TestSuite) {
        writeln()
        if (suite.isDisabled) writeln("/*")
        writeln("test ${suite.name}: parsing [[")
        writeTestContent(suite)
        writeln("]] parse succeeds")
        if (suite.isDisabled) writeln("*/")
    }

    /**
     * Writes an analysis test.
     *
     * @param suite the test suite
     */
    private fun Writer.writeAnalysisTests(suite: TestSuite) {
        writeln()
        if (suite.isDisabled) writeln("/*")
        writeln("test ${suite.name}: default analysis [[")
        writeTestContent(suite)
        writeln("]] analysis succeeds")
        if (suite.isDisabled) writeln("*/")

        writeln()
        if (suite.isDisabled) writeln("/*")
        writeln("test ${suite.name}: test analysis [[")
        writeTestContent(suite)
        writeln("]] run test-analyze to SUCCEED()")
        if (suite.isDisabled) writeln("*/")
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
        // Hack to get the indices in the right order.
        val indices = (listOf(
            "ref" to case.refIndex,
            "decl" to case.declIndex
        ) + case.contextIndexes.mapIndexed { i, e -> "ctx$i" to e }).sortedWith { a, b ->
            val a = a.second
            val b = b.second
            when {
                a === b -> 0
                a == null -> 1  // sort "ctx" last
                b == null -> -1
                else -> a.compareTo(b)
            }
        }.map { it.first }

        writeln()
        if (suite.isDisabled) writeln("/*")
        writeln("test ${suite.name}: refret test ${index + 1} [[")
        val refId = suite.identifiers[case.refIndex]
        val declId = suite.identifiers[case.declIndex]
        if (case.contextIndexes.isNotEmpty()) {
            val ctxIds = case.contextIndexes.map { suite.identifiers[it] }
            writeTestContent(suite, listOf(refId, declId) + ctxIds, mapOf(refId to case.originalRefText))
            writeln("]] run fix-reference(|#${indices.indexOf("ref") + 1}, #${indices.indexOf("decl") + 1}, ${List(case.contextIndexes.size) { i -> "#${indices.indexOf("ctx$i") + 1}" }.joinToString(", ")}) to [[")
        } else {
            writeTestContent(suite, listOf(refId, declId), mapOf(refId to case.originalRefText))
            writeln("]] run fix-reference(|#${indices.indexOf("ref") + 1}, #${indices.indexOf("decl") + 1}) to [[")
        }
        writeTestContent(suite)
        writeln("]]")
        if (suite.isDisabled) writeln("*/")
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