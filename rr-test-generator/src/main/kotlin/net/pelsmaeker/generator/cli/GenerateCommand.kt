package net.pelsmaeker.generator.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import net.pelsmaeker.generator.stage1.TestSuiteGenerator
import net.pelsmaeker.generator.stage2.SptTestGenerator
import net.pelsmaeker.generator.stage2.TestSuiteFinder
import java.nio.file.Files
import java.nio.file.Path

/** Generate SPT test files command. */
class GenerateCommand: CliktCommand(
    name = "generate",
    help = "Generate SPT test files"
) {

    /** The input paths. These directories are searched for subdirectories and files. */
    private val inputs: List<Path> by argument(help="Directory with input directories and files")
        .path(mustExist = true, canBeFile = false, canBeDir = true, mustBeReadable = true)
        .multiple()
    /** The output path. This directory is used to create subdirectories and write the generated test suite files. */
    private val output: Path by option("-o", "--out", help="Directory for output directories and files")
        .path(mustExist = false, canBeFile = false, canBeDir = true)
        .required()

    override fun run() {
        // Gather all test suites
        val testSuites = inputs.flatMap { input ->
            Cli.info("Finding test suites in: $input")
            val suites = TestSuiteFinder.findAllTestSuites(input, input)
            suites
        }
        Cli.info("Found ${testSuites.size} test suites.")

        // Ensure the output directory exists
        Cli.info("Creating output directory: $output")
        Files.createDirectories(output)

        // Write each SPT test out to a file
        Cli.info("Generating SPT test files in: $output")
        for (testSuite in testSuites) {
            val dest = SptTestGenerator.writeToFile(testSuite, output)
            Cli.info("Wrote test suite for ${testSuite.name} to: $dest")
        }
        Cli.info("Generated ${testSuites.size} SPT test files.")

        Cli.info("Done!")
    }
}