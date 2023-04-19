package net.pelsmaeker.generator.utils

import net.pelsmaeker.generator.Highlight
import java.io.Writer
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.bufferedWriter

fun Path.overwritingBufferedWriter(overwrite: Boolean): Writer {
    val openOptions = if (overwrite)
        arrayOf(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
    else
        arrayOf(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
    return this.bufferedWriter(options = *openOptions)
}

/**
 * Writes the test packages and units content.
 *
 * @param content the test content
 * @param selections the references and declarations selected by surrounding them with square brackets
 * @param replacements the text to replace (some of) the selected references and declarations with, if any
 */
fun Writer.writeTestContent(
    content: String,
    selections: List<Highlight> = emptyList(),
    replacements: Map<Highlight, String> = emptyMap(),
) {
    val orderedSelections = selections.sortedBy { it.range.first }

    // Adjust the text to include the selections
    val newText = content.replaceAll(orderedSelections, Highlight::range) { s, t, _ ->
        "[[${replacements[s] ?: t}]]"
    }
    write("  ")
    writeln(newText.prependIndent("  ").trim())
}