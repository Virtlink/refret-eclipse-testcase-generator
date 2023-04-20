package net.pelsmaeker.generator.stage2

import net.pelsmaeker.generator.*
import net.pelsmaeker.generator.cli.Cli
import net.pelsmaeker.generator.utils.replaceAll
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * Reads refactoring test cases from a file.
 */
object RefactoringTestSuiteReader {

    /**
     * Reads all test suites in the given directory structure.
     *
     * @param directory the directory structure
     * @param root the root path relative to which the name of the test is determined
     * @return a list of test suites
     */
    fun readAll(directory: Path, root: Path): List<TestSuite> {
        val entries = directory.listDirectoryEntries()

        // Group the entries by their normalized name (including directories).
        val entryGroups = entries.groupBy { entry ->
            val entryName = entry.fileName.name
            if (entryName.endsWith("_before.java")) {
                entryName.substringBeforeLast("_before.java")
            } else if (entryName.endsWith("_after.java")) {
                entryName.substringBeforeLast("_after.java")
            } else {
                // This also includes the case where the file name does not end in ".java".
                entryName.substringBeforeLast(".java")
            }
        }

        return entryGroups.flatMap { (name, entries) ->
            if (entries.size == 1 && entries.first().isDirectory()) {
                // Recurse if it is a directory.
                readAll(entries.first(), root)
            } else if (entries.size == 2) {
                // A before and after file
                val after = entries.first { it.fileName.name.endsWith("_after.java") }
                val before = entries.filterNot { it == after }.first()  // Eiher "_before.java" or just ".java"
                // Read a test file.
                readFromFile(before, after, root)?.let { listOf(it) } ?: emptyList()
            } else {
                // Too many entries
                Cli.warn("Skipped, too many files for test suite '$name': $entries")
                emptyList()
            }
        }
    }

    /**
     * Reads a test suite from the specified file.
     *
     * @param beforeFile the file path to the 'before' file
     * @param afterFile the file path to the 'after' file
     * @param root the root path relative to which the name of the test is determined
     * @return the test suite; or `null` when none found
     */
    fun readFromFile(beforeFile: Path, afterFile: Path, root: Path): TestSuite? {
        val beforeContent = beforeFile.readText()
        val afterContent = afterFile.readText()

        val pathComponents = root.relativize(beforeFile).map { it.toString() }.toList()
        val testDir = pathComponents.dropLast(1).joinToString("/")
        // Trick to get the sortest test name without the suffix
        val testName = pathComponents.last().let {
            listOf(
                it.substringBeforeLast(".java"),
                it.substringBeforeLast("_before.java"),
                it.substringBeforeLast("_after.java"),
            )
        }.minBy { it.length }

        return read(
            testName,
            testDir,
            beforeContent,
            afterContent,
        )
    }

    /**
     * Reads a test suite from the given contents with markers.
     *
     * The content must have an annotation `[[{move-class(1, 2)}]]` to indicate the refactoring to perform.
     *
     * @param name the test suite name
     * @param directory the test suite directory
     * @param beforeContent the 'before' content, with markers
     * @param afterContent the 'after' content
     * @return the test suite; or `null` when none found
     */
    private fun read(name: String, directory: String, beforeContent: String, afterContent: String): TestSuite? {
        // Read the markers
        val markers = readMarkers(beforeContent)
        val decls = markers.filterIsInstance<DeclMarker>()
        val annotations = markers.filterIsInstance<AnnotationMarker>()
        val isDisabled = annotations.any { it.name == "disabled" }
        val moveClassAnno = annotations.firstOrNull { it.name == "move-class" && it.arguments.size == 2 } ?: return null
        val moveClassFrom = moveClassAnno.arguments[0]
        val moveClassToPkg = moveClassAnno.arguments[1]

        // Build the 'before text' (the code without any markers).
        // At the same time, convert the relevant markers to Highlights and build a list of all identifiers in order from first to last.
        val markerToId = mutableMapOf<Marker, Highlight>()
        val highlights = mutableListOf<Highlight>()
        val beforeContentWithoutMarkers = beforeContent.replaceAll(markers, { it.range }) { m, _, s ->
            if (m is DeclMarker) {
                val id = m.toHighlight(s)
                markerToId.compute(m) { _, v -> v?.let { error("Duplicate marker: $it") } ?: id }
                highlights.add(id)
            }
            m.replacementText
        }
        // Build the 'after text' (the code without any markers).
        val afterContentWithoutMarkers = afterContent.replaceAll(readMarkers(afterContent), { it.range }) { m, _, _ -> m.replacementText }

        // Build the test cases
        val cases = mutableListOf<TestCase>()

        // Parser tests
        cases.add(ParseTestCase(
            "$name: parse 'before' test",
            isDisabled,
            beforeContentWithoutMarkers,
        ))

        cases.add(ParseTestCase(
            "$name: parse 'after' test",
            isDisabled,
            afterContentWithoutMarkers,
        ))

        // Analysis tests
        cases.add(AnalysisTestCase(
            "$name: default 'before' analysis",
            isDisabled,
            beforeContentWithoutMarkers,
        ))
        cases.add(AnalysisTestCase(
            "$name: default 'after' analysis",
            isDisabled,
            afterContentWithoutMarkers,
        ))

        val clsId = decls.firstOrNull { it.id == moveClassFrom } ?: error("No declaration $moveClassFrom")
        val pkgId = decls.firstOrNull { it.id == moveClassToPkg } ?: error("No declaration $moveClassToPkg")
        val clsIndex = highlights.indexOf(markerToId[clsId]).takeIf { it >= 0 } ?: error("Declaration not found: $moveClassFrom")
        val pkgIndex = highlights.indexOf(markerToId[pkgId]).takeIf { it >= 0 } ?: error("Declaration not found: $moveClassToPkg")
        cases.add(TestMoveClassTestCase(
            "$name: move class test",
            isDisabled,
            beforeContentWithoutMarkers,
            afterContentWithoutMarkers,
            highlights,
            clsIndex,
            pkgIndex,
        ))

        return TestSuite(
            name = name,
            directory = directory,
            cases = cases,
            isDisabled = isDisabled,
        )
    }
}