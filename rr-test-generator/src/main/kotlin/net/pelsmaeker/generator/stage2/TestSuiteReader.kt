package net.pelsmaeker.generator.stage2

import net.pelsmaeker.generator.*
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

/**
 * Reads test cases from a file.
 */
abstract class TestSuiteReader {

    // Annotations: "[[{disabled}]]"
    protected val annotationRegex = Regex("""\[\[\{([^|\]]+)\}\]\]""")
    private val markerRegex = Regex("""\[\[([^|\]]+)((?:\|[^|\]]+)*)\]\]""")

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
     * @param name the test suite name
     * @param name the test suite qualifier; or `null`
     * @param directory the test suite directory
     * @param text the text with markers
     * @return the test suite
     */
    protected abstract fun read(name: String, directory: String, text: String): TestSuite

    protected fun readMarkers(text: String, transformer: (IntRange, String, List<String>) -> Marker?): List<Marker> {
        val markers = mutableListOf<Marker>()
        markerRegex.findAll(text).forEach { match ->
            val range = match.groups[0]!!.range
            val operator = match.groups[1]!!.value
            val values = match.groups[2]!!.value
                .takeIf { it.isNotBlank() }?.let {it.substring(1).split('|').toMutableList() } ?: mutableListOf()

            val marker = transformer(range, operator, values)
            if (marker != null) markers.add(marker)
        }
        return markers
    }

    /** Common interface for markers. */
    interface Marker {
        /** The replacement text for the marker, which is the actual text for a declaration and the expected text for a reference. */
        val replacementText: String
        /** The range of the marker in the text. */
        val range: IntRange

        /**
         * Converts the marker to a [Highlight].
         *
         * @param start the final start index of the marker in the expected text
         * @return the [Highlight]
         */
        fun toHighlight(start: Int): Highlight =
            Highlight(start until start + replacementText.length)
    }

}