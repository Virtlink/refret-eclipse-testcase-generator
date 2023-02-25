package net.pelsmaeker.generator.stage1

import net.pelsmaeker.generator.cli.Cli
import java.nio.file.Path
import kotlin.io.path.*

/** Finds Java projects in the Eclipse refactoring test directory. */
object JavaProjectFinder {

    /**
     * Finds all Java projects in the given directory structure.
     *
     * @param directory the directory structure
     * @param root the root path relative to which the name of the test is determined
     * @return a list of Java projects
     */
    fun findAllJavaProjects(directory: Path, root: Path): List<JavaProject> {
        val entries = directory.listDirectoryEntries()

        return if (entries.any { it.name == "in" } || entries.any { it.name == "out" }) {
            // If the directory contains a directory `in` and/or a directory `out`, we have a java project in each directory
            val remainingEntries = entries.filter { !it.isDirectory() }
            if (remainingEntries.isNotEmpty()) {
                Cli.warn {
                    "Directory with `in`/`out` directories also contains files, skipped:\n  " +
                    remainingEntries.joinToString("\n  ")
                }
            }
            entries.filter { it.isDirectory() }.map { readJavaProjectFromDirectory(it, root) }
        } else if (entries.any { it.name.endsWith("_in.java") } || entries.any { it.name.endsWith("_out.java") }) {
            // If the directory contains one or more files that end in `_in` and `_out`, then each is a java project
            val entriesAndProjects = entries.map { it to readJavaProjectFromFile(it, root) }
            val skippedEntries = entriesAndProjects.filter { it.second == null }.map { it.first }
            if (skippedEntries.isNotEmpty()) {
                Cli.warn {
                    "Test suite name could not be determined from file, skipped:\n  " +
                    skippedEntries.joinToString("\n  ")
                }
            }
            entriesAndProjects.mapNotNull { it.second }
        } else {
            // Recurse if it is a directory.
            entries.filter { it.isDirectory() }.flatMap { entry -> findAllJavaProjects(entry, root) }
        }
    }

    /**
     * Reads a Java project from a single file with the naming pattern `Class_testName_in.java`
     * or `Class_testName_out.java`.
     *
     * @param file the file path
     * @param root the root path relative to which the name of the test is determined
     * @return the Java project; or `null` if it was skipped
     */
    fun readJavaProjectFromFile(file: Path, root: Path): JavaProject? {
        val testSuiteNames = getTestSuiteName(file.fileName.toString()) ?: return null  // Skipped
        val (unitName, testName, testQualifier) = testSuiteNames

        val text = file.readText()
        val packageName = getPackageName(text) ?: ""

        val pathComponents = root.relativize(file).map { it.toString() }.toList()
        val testDir = pathComponents.dropLast(1).joinToString("/")

        return JavaProject(
            "${unitName}_$testName",
            testQualifier,
            testDir,
            listOf(JavaPackage(
                    packageName,
                    listOf(JavaUnit(
                            unitName,
                            text,
                    ))
            ))
        )
    }

    /**
     * Reads a Java project from the files in a directory, such as an `out` directory.
     *
     * @param directory the directory path, ending with a directory such as `out`
     * @param root the root path relative to which the name of the test is determined
     * @return the Java project
     */
    fun readJavaProjectFromDirectory(directory: Path, root: Path): JavaProject {
        val javaFiles = directory.listDirectoryEntries("*.java")

        val packages = mutableMapOf<String, MutableList<JavaUnit>>()
        for (javaFile in javaFiles) {
            val text = javaFile.readText()
            val packageName = getPackageName(text) ?: ""
            val unitsInPackage = packages.computeIfAbsent(packageName) { mutableListOf() }
            unitsInPackage.add(JavaUnit(javaFile.fileName.nameWithoutExtension, text))
        }

        val pathComponents = root.relativize(directory).map { it.toString() }.toList()
        val testDir = pathComponents.dropLast(2).joinToString("/")
        val testName = pathComponents.dropLast(1).last()
        val testQualifier = pathComponents.last()

        return JavaProject(
            testName,
            testQualifier,
            testDir,
            packages.map { (packageName, units) ->
                JavaPackage(packageName, units)
            }
        )
    }

    /** Regex for finding the package name in a Java file. */
    private val packageRegex = Regex("""^\s*package\s+([^;]+);\s*${'$'}""", RegexOption.MULTILINE)

    /**
     * Reads the Java package name from the Java code.
     *
     * @return the package name; or `null` if it could not be determined
     */
    private fun getPackageName(text: String): String? {
        return packageRegex.find(text)?.let { it.groups[1]?.value }
    }


    /** Regex for matching a test suite file name into a (unitName, testSuiteName, testSuiteQualifier). */
    private val suiteFilenameRegex = Regex("""^([^_\.]+)(?:_([^\.]+))?_(in|out).java${'$'}""")

    /**
     * Determines the unit name and test suite name and qualifier.
     *
     * @param filename the filename to parse
     * @return the names; or `null` if they could not be determined
     */
    private fun getTestSuiteName(filename: String): Triple<String, String, String>? {
        val match = suiteFilenameRegex.find(filename) ?: return null
        val unitName = match.groups[1]!!.value
        val testSuiteName = match.groups[2]?.value ?: unitName
        val testSuiteQualifier = match.groups[3]!!.value
        return Triple(unitName, testSuiteName, testSuiteQualifier)
    }
}