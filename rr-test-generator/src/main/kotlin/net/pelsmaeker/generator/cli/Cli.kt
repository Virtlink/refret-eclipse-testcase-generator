package net.pelsmaeker.generator.cli

import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.rendering.Whitespace
import com.github.ajalt.mordant.terminal.Terminal

/** The CLI. */
object Cli {

    /** The terminal object. */
    private val terminal = Terminal()

    /** Prints an INFO message to the console. */
    fun info(message: String) {
        terminal.info(message, whitespace = Whitespace.PRE_LINE, overflowWrap = OverflowWrap.BREAK_WORD)
    }

    /** Prints a WARN message to the console. */
    fun warn(message: String) {
        terminal.warning(message, whitespace = Whitespace.PRE_LINE, overflowWrap = OverflowWrap.BREAK_WORD)
    }

    /** Prints an ERROR message to the console. */
    fun error(message: String) {
        terminal.danger(message, whitespace = Whitespace.PRE_LINE, overflowWrap = OverflowWrap.BREAK_WORD)
    }


    /** Prints an INFO message to the console. */
    fun info(messageProvider: () -> String) = info(messageProvider())

    /** Prints an WARN message to the console. */
    fun warn(messageProvider: () -> String) = warn(messageProvider())

    /** Prints an ERROR message to the console. */
    fun error(messageProvider: () -> String) = error(messageProvider())

}