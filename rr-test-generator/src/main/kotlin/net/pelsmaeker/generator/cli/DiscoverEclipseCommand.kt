package net.pelsmaeker.generator.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import net.pelsmaeker.generator.stage1.EclipseTestFinder
import net.pelsmaeker.generator.stage1.JavaProject
import net.pelsmaeker.generator.stage1.TestSuiteGenerator
import java.nio.file.Files
import java.nio.file.Path

/** Generate basic test suite files command. */
class DiscoverEclipseCommand: CliktCommand(
    name = "discover-eclipse",
    help = "Discovers Eclipse test files"
) {

    /** The input paths. These directories are searched for subdirectories and files. */
    private val inputs: List<Path> by argument(help="Directory with input directories and files")
        .path(mustExist = true, canBeFile = false, canBeDir = true, mustBeReadable = true)
        .multiple()

    /** The output path. This directory is used to create subdirectories and write the generated test suite files. */
    private val output: Path by option("-o", "--out", help="Directory for output directories and files")
        .path(mustExist = false, canBeFile = false, canBeDir = true)
        .required()

    /** Whether to force overwriting existing generated files. */
    private val force: Boolean by option("-f", "--force", help="Force overwrite of existing files")
        .flag(default = false)

    override fun run() {
        // Gather all projects
        val javaProjects = inputs.flatMap { input ->
            Cli.info("Finding test Java projects in: $input")
            val projects = EclipseTestFinder.findAllJavaProjects(input, input)
            projects
        }
        Cli.info("Found ${javaProjects.size} test Java projects.")

        // Ensure the output directory exists
        Cli.info("Creating output directory: $output")
        Files.createDirectories(output)

        // Write each Java project out as a SPT test suite file
        Cli.info("Generating test suite files in: $output")
        val skippedProjects = mutableListOf<JavaProject>()
        for (javaProject in javaProjects) {
            val path = TestSuiteGenerator.writeToFile(javaProject, output, force)
            if (path != null) {
                Cli.info("  ${javaProject.directory}/${javaProject.name}_${javaProject.qualifier}")
            } else {
                skippedProjects.add(javaProject)
            }
        }
        if (skippedProjects.isNotEmpty()) {
            Cli.warn("Skipped generating ${skippedProjects.size} test suite files, because they already exist:")
            for (skippedProject in skippedProjects) {
                Cli.warn("  ${skippedProject.directory}/${skippedProject.name}_${skippedProject.qualifier}")
            }
        }
        Cli.info("Generated ${javaProjects.size - skippedProjects.size} test suite files.")

        Cli.info("Done!")
    }
}