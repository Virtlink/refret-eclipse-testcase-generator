package net.pelsmaeker.generator.stage1

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
            entries.map { readJavaProjectFromDirectory(it, root) }
        } else if (entries.any { it.name.endsWith("_in.java") } || entries.any { it.name.endsWith("_out.java") }) {
            // If the directory contains one or more files that end in `_in` and `_out`, then each is a java project
            entries.map { readJavaProjectFromFile(it, root) }
        } else {
            // Recurse if it is a directory.
            entries.filter { it.isDirectory() }.flatMap { entry -> findAllJavaProjects(entry, root) }
        }
    }

    /** Regex for matching a test suite file name into a (unitName, testSuiteName, testSuiteQualifier). */
    private val suiteFilenameRegex = Regex("""^([^_]+)_([^_]+)_([^_]+).java${'$'}""")

    /**
     * Reads a Java project from a single file with the naming pattern `Class_testName_in.java`
     * or `Class_testName_out.java`.
     *
     * @param file the file path
     * @param root the root path relative to which the name of the test is determined
     * @return the Java project
     */
    fun readJavaProjectFromFile(file: Path, root: Path): JavaProject {
        val (unitName, testSuiteName, testSuiteQualifier) = suiteFilenameRegex.find(file.fileName.toString())!!.destructured
        val text = file.readText()
        val packageName = getPackageName(text)

        val testName = root.relativize(file).joinToString("_") + "_" + testSuiteName + "_" + testSuiteQualifier

        return JavaProject(
            testName,
            listOf(
                JavaPackage(
                    packageName,
                    listOf(
                        JavaUnit(
                        unitName,
                        text,
                    )
                    )
                )
            )
        )
    }

    /**
     * Reads a Java project from the files in a directory, such as an `out` directory.
     *
     * @param directory the directory path
     * @param root the root path relative to which the name of the test is determined
     * @return the Java project
     */
    fun readJavaProjectFromDirectory(directory: Path, root: Path): JavaProject {
        val javaFiles = directory.listDirectoryEntries("*.java")

        val packages = mutableMapOf<String, MutableList<JavaUnit>>()
        for (javaFile in javaFiles) {
            val text = javaFile.readText()
            val packageName = getPackageName(text)
            val unitsInPackage = packages.computeIfAbsent(packageName) { mutableListOf() }
            unitsInPackage.add(JavaUnit(javaFile.fileName.nameWithoutExtension, text))
        }

        val testName = root.relativize(directory).joinToString("_")

        return JavaProject(
            testName,
            packages.map { (packageName, units) ->
                JavaPackage(packageName, units)
            }
        )
    }

    /** Regex for finding the package name in a Java file. */
    private val packageRegex = Regex("""^\s*package\s+([^;]);\s*${'$'}""", RegexOption.MULTILINE)

    /**
     * Reads the Java package name from the Java code.
     *
     * @return the package name
     */
    private fun getPackageName(text: String): String {
        return packageRegex.find(text)!!.groups[1]!!.value
    }
}