package net.pelsmaeker.generator.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import net.pelsmaeker.generator.stage1.JavaProjectFinder
import net.pelsmaeker.generator.stage1.TestSuiteGenerator
import java.nio.file.Files
import java.nio.file.Path

/** Generate basic test suite files command. */
class DiscoverCommand: CliktCommand(
    name = "discover",
    help = "Discovers Eclipse test files"
) {

    /** The input path. This directory is searched for subdirectories and files. */
    private val inputs: List<Path> by argument(help="Directory with input directories and files")
        .path(mustExist = true, canBeFile = false, canBeDir = true, mustBeReadable = true)
        .multiple()
    /** The output path. This directory is used to create subdirectories and write the generated test suite files. */
    private val output: Path by option("-o", "--out", help="Directory for output directories and files")
        .path(mustExist = false, canBeFile = false, canBeDir = true)
        .required()

    override fun run() {
        // Gather all projects
        val javaProjects = inputs.flatMap { input ->
            Cli.info("Finding test Java projects in: $input")
            val projects = JavaProjectFinder.findAllJavaProjects(input, input)
            Cli.info("Found ${projects.size} test Java projects.")
            projects
        }

        // Ensure the output directory exists
        Files.createDirectories(output)

        // Write each Java project out as a SPT test suite file
        Cli.info("Generating test suite files in: $output")
        for (javaProject in javaProjects) {
            TestSuiteGenerator.writeToFile(javaProject, output)
            Cli.info("Generated test suite file for: ${javaProject.name}")
        }
        Cli.info("Generated ${javaProjects.size} test suite files.")

        Cli.info("Done!")
    }
}