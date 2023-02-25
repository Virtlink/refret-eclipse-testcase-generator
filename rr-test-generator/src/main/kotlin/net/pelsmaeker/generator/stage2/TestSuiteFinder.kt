package net.pelsmaeker.generator.stage2

import net.pelsmaeker.generator.cli.Cli
import net.pelsmaeker.generator.stage1.JavaProjectFinder
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

/** Finds test suites in the specified directory. */
object TestSuiteFinder {

    /**
     * Finds all test suites in the given directory structure.
     *
     * @param directory the directory structure
     * @param root the root path relative to which the name of the test is determined
     * @return a list of test suites
     */
    fun findAllTestSuites(directory: Path, root: Path): List<TestSuite> {
        val entries = directory.listDirectoryEntries()

        return entries.flatMap { entry ->
            if (entry.isDirectory()) {
                // Recurse if it is a directory.
                findAllTestSuites(entry, root)
            } else {
                // Read a test file.
                listOf(readTestSuiteFromFile(entry, root))
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
    fun readTestSuiteFromFile(file: Path, root: Path): TestSuite {
        val content = file.readText()

        val pathComponents = root.relativize(file).map { it.toString() }.toList()
        val testDir = pathComponents.dropLast(1).joinToString("/")
        val testFileName = pathComponents.last().substringBeforeLast(".java")
        val testName = testFileName.substringBeforeLast('_')
        val testQualifier = testFileName.substringAfterLast('_', missingDelimiterValue = "").takeIf { it.isNotBlank() }

        return TestSuiteReader.readTestSuite(
            testName,
            testQualifier,
            testDir,
            content,
        )
    }

}