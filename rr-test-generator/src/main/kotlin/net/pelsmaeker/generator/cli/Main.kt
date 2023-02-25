package net.pelsmaeker.generator.cli

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

/**
 * The main entry point.
 *
 * @param args the command-line arguments
 */
fun main(args: Array<String>) {
    MainCommand().subcommands(
        DiscoverCommand(),
        GenerateSptTestsCommand(),
    ).main(args)
}

/** The root command. */
private class MainCommand: NoOpCliktCommand(
    name = "rr-test-generator",
    help = "Generate test cases for the Round Robin algorithm"
)

