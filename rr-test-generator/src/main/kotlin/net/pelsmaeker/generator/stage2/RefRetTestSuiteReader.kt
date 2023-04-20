package net.pelsmaeker.generator.stage2

import net.pelsmaeker.generator.*
import net.pelsmaeker.generator.utils.replaceAll
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

/**
 * Reads reference retention test cases from a file.
 *
 * The file should be a Java file with multiple packages and multiple compilation units (as required by Java SPT tests).
 * Relevant references and declarations should be surrounded by `[[ ... ]]`. A declaration starts with an `'@'` symbol,
 * identifier (e.g., a number), followed by a pipe, followed by the name of the declaration. For example, `[[@1|foo]]`
 * is a declaration of a member named `foo`.
 *
 * A reference starts with an arrow, an identifier of the declaration it should resolve to (e.g., a number), followed by
 * a pipe, followed by the input name of the reference, followed by the expected (qualified) name of the reference.
 * If the expected name is omitted, it is assumed to be equal to the input name. Whitespace around the names or code is
 * ignored. For example, `[[->1|foo|B.foo]]` is a reference to the declaration `[[@1|foo]]` in class `B`.
 */
object RefRetTestSuiteReader {

    /**
     * Reads all test suites in the given directory structure.
     *
     * @param directory the directory structure
     * @param root the root path relative to which the name of the test is determined
     * @return a list of test suites
     */
    fun readAll(directory: Path, root: Path): List<TestSuite> {
        val entries = directory.listDirectoryEntries()

        return entries.flatMap { entry ->
            if (entry.isDirectory()) {
                // Recurse if it is a directory.
                readAll(entry, root)
            } else {
                // Read a test file.
                listOf(readFromFile(entry, root))
            }
        }
    }

    /**
     * Reads a test suite from the specified file.
     *
     * @param file the file path
     * @param root the root path relative to which the name of the test is determined
     * @return the test suite
     */
    fun readFromFile(file: Path, root: Path): TestSuite {
        val content = file.readText()

        val pathComponents = root.relativize(file).map { it.toString() }.toList()
        val testDir = pathComponents.dropLast(1).joinToString("/")
        val testName = pathComponents.last().substringBeforeLast(".java")

        return read(
            testName,
            testDir,
            content,
        )
    }

    /**
     * Reads a test suite from the given text with markers.
     *
     * A declaration should be marked with `[[@id|name]]`, where `id` is an identifier and `name` is the name of the
     * declaration. A reference should be marked with `[[->id|initial|expected]]`, where `id` is the identifier of the
     * declaration to which the reference should resolve, `initial` is the initial (unrefactored, possibly wrong, maybe
     * qualified) name of the reference, and `expected` is the expected qualified name of the reference.
     *
     * @param name the test suite name
     * @param directory the test suite directory
     * @param content the text with markers
     * @return the test suite
     */
    private fun read(name: String, directory: String, content: String): TestSuite {
        // Read the markers
        val markers = readMarkers(content)
        val refs = markers.filterIsInstance<RefMarker>()
        val decls = markers.filterIsInstance<DeclMarker>()
        val annotations = markers.filterIsInstance<AnnotationMarker>()
        val isDisabled = annotations.any { it.name == "disabled" }

        // Build the 'expected text' (the code without any markers and with the expected qualified references).
        // At the same time, convert the relevant markers to Highlights and build a list of all identifiers in order from first to last.
        val markerToId = mutableMapOf<Marker, Highlight>()
        val highlights = mutableListOf<Highlight>()
        val expectedText = content.replaceAll(markers, { it.range }) { m, _, s ->
            if (m is RefMarker || m is DeclMarker) {
                val id = m.toHighlight(s)
                markerToId.compute(m) { _, v -> v?.let { error("Duplicate marker: $it") } ?: id }
                highlights.add(id)
            }
            m.replacementText
        }


        // Build the test cases
        val cases = mutableListOf<TestCase>()

        // Parser test
        cases.add(ParseTestCase(
            "$name: parsing",
            isDisabled,
            expectedText,
        ))

        // Analysis tests
        cases.add(AnalysisTestCase(
            "$name: default analysis",
            isDisabled,
            expectedText,
        ))
        cases.add(TestAnalyzeTestCase(
            "$name: test analysis",
            isDisabled,
            expectedText,
        ))

        // Tests for individual references
        refs.forEachIndexed { i, ref ->
            val decl = decls.firstOrNull { it.id == ref.declId } ?: error("No declaration for reference $ref")
            val declIndex = highlights.indexOf(markerToId[decl]).takeIf { it >= 0 } ?: error("Declaration not found: $decl")
            val refIndex = highlights.indexOf(markerToId[ref]).takeIf { it >= 0 } ?: error("Reference not found: $ref")
            val contexts = ref.contextIds.map { ctxId -> decls.first { it.id == ctxId } }
            val contextIndexes = contexts.map { highlights.indexOf(markerToId[it]).takeIf { it >= 0 } ?: error("Context not found: $it") }
            cases.add(RefRetTestCase(
                "$name: refret test ${i + 1}",
                isDisabled,
                expectedText,
                highlights,
                ref.text,
                refIndex,
                declIndex,
                contextIndexes,
            ))
        }

        // TODO: Do something with the contexts

        return TestSuite(
            name = name,
            directory = directory,
            cases = cases,
            isDisabled = isDisabled,
        )
    }

}