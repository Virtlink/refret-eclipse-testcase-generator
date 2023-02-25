package net.pelsmaeker.generator.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.rendering.*
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

/** The CLI. */
class Cli(
    /** The subcommands of the CLI. */
    subcommands: Set<@JvmSuppressWildcards CliktCommand>,
): NoOpCliktCommand(
    name = "rr-test-generator",
    help = "Generate test cases for the Round Robin algorithm",
) {
    init {
        versionOption("${ApplicationInfo.version} (${ApplicationInfo.revision})") { applicationInfoToString() }
        subcommands(subcommands)
    }

    constructor(vararg subcommands: CliktCommand): this(subcommands.toSet())

    companion object {
        /** The terminal object. */
        private val terminal = Terminal()

        /** Prints an INFO message to the console. */
        fun info(message: String) {
            terminal.info(message, whitespace = Whitespace.PRE)//, overflowWrap = OverflowWrap.BREAK_WORD)
        }

        /** Prints a WARN message to the console. */
        fun warn(message: String) {
            terminal.warning(message, whitespace = Whitespace.PRE)//, overflowWrap = OverflowWrap.BREAK_WORD)
        }

        /** Prints an ERROR message to the console. */
        fun error(message: String) {
            terminal.danger(message, whitespace = Whitespace.PRE)//, overflowWrap = OverflowWrap.BREAK_WORD)
        }

        /** Prints an ERROR message on the console and abort the program by throwing a [ProgramResult] exception. */
        fun fatal(message: String): Nothing {
            terminal.danger(message, whitespace = Whitespace.PRE)//, overflowWrap = OverflowWrap.BREAK_WORD)
            throw ProgramResult(2)
        }


        /** Prints an INFO message to the console. */
        fun info(messageProvider: () -> String) = info(messageProvider())

        /** Prints an WARN message to the console. */
        fun warn(messageProvider: () -> String) = warn(messageProvider())

        /** Prints an ERROR message to the console. */
        fun error(messageProvider: () -> String) = error(messageProvider())

        /** Prints an ERROR message on the console and abort the program by throwing a [ProgramResult] exception. */
        fun fatal(messageProvider: () -> String): Nothing = fatal(messageProvider())

        /**
         * Formats an instant as a long date/time with the system's default locale and time zone.
         */
        private val longDateTimeFormatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.LONG)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())

        /**
         * Renders a formatted string representation of the application information without color.
         *
         * @return the string representation
         */
        fun applicationInfoToString(): String {
            return Terminal(AnsiLevel.NONE).render(getApplicationInfoTable())
        }

        /**
         * Prints a formatted string representation of the application information to STDOUT.
         *
         * This automatically detects the capabilities of the terminal,
         * such as its support for colors and its size.
         */
        fun printApplicationInfo() {
            terminal.println(getApplicationInfoTable())
        }

        /**
         * Gets a formatted table with the application version information.
         *
         * @return the formatted table
         */
        private fun getApplicationInfoTable(): Widget = table {
            align = TextAlign.LEFT
            tableBorders = Borders.NONE
            cellBorders = Borders.NONE
            borderType = BorderType.DOUBLE
            overflowWrap = OverflowWrap.BREAK_WORD
            column(0) {
                style(TextColors.magenta, bold = true)
            }
            header {
                style(TextColors.red, bold = true)
                row {
                    cellBorders = Borders.TOM_BOTTOM
                    cell("${ApplicationInfo.name} ${ApplicationInfo.version} (${ApplicationInfo.tag})") { columnSpan = 2 }
                }
            }
            body {
                rowStyles(TextColors.blue, TextColors.blue)
                row("Version:", ApplicationInfo.version)
                row("Revision:", ApplicationInfo.revision)
                row("Built on:", ApplicationInfo.buildAt.let { longDateTimeFormatter.format(it) })
                row()
                row("JVM:", "${JvmInfo.javaVersion} (${JvmInfo.vendor} ${JvmInfo.version})")
                row("OS:", "${OsInfo.name} ${OsInfo.version} (${OsInfo.architecture})")
                row()
                row("User:", SystemInfo.userName)
                row("User home:", SystemInfo.userHome)
                row("Working dir:", SystemInfo.workingDirectory)
                row()
                row("Memory:", "${SystemInfo.memoryLimit} bytes")
                row("Processors:", SystemInfo.processorLimit)
            }
        }
    }

}