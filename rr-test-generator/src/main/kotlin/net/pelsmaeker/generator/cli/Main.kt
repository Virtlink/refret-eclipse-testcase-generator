package net.pelsmaeker.generator.cli

/**
 * The main entry point.
 *
 * @param args the command-line arguments
 */
fun main(args: Array<String>) {
    Cli(
        DiscoverCommand(),
        GenerateSptTestsCommand(),
    ).main(args)
}
