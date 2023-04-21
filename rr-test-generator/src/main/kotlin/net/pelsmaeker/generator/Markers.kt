package net.pelsmaeker.generator

import net.pelsmaeker.generator.stage2.RefRetTestSuiteReader

private val markerRegex = Regex("""\[\[([^|\]]+)((?:\|[^|\]]+)*)\]\]""")

fun readMarkers(text: String): List<Marker> {
    return readMarkers(text) { range, operator, values ->
        when {
            operator.startsWith('@') -> DeclMarker.read(range, operator, values)
            operator.startsWith("->") -> RefMarker.read(range, operator, values)
            operator.startsWith("{") -> AnnotationMarker.read(range, operator, values)
            operator.startsWith("#") -> CommentMarker.read(range, operator, values)
            else -> error("Unknown operator: $operator")
        }
    }
}

private fun <T: Marker> readMarkers(text: String, transformer: (IntRange, String, List<String>) -> T?): List<T> {
    val markers = mutableListOf<T>()
    markerRegex.findAll(text).forEach { match ->
        val range = match.groups[0]!!.range
        val operator = match.groups[1]!!.value
        val values = match.groups[2]!!.value
            .takeIf { it.isNotBlank() }?.let {it.substring(1).split('|').toMutableList() } ?: mutableListOf()

        val marker = transformer(range, operator, values)
        if (marker != null) markers.add(marker)
    }
    return markers
}

/** Common interface for markers. */
interface Marker {
    /** The replacement text for the marker, which is the actual text for a declaration and the expected text for a reference. */
    val replacementText: String
    /** The range of the marker in the text. */
    val range: IntRange

    /**
     * Converts the marker to a [Highlight].
     *
     * @param start the final start index of the marker in the expected text
     * @return the [Highlight]
     */
    fun toHighlight(start: Int): Highlight =
        Highlight(start until start + replacementText.length)
}

/** A comment marker. */
data class CommentMarker(
    override val range: IntRange,
): Marker {
    override val replacementText get() = ""
    override fun toString(): String = "#<comment>"

    companion object {
        fun read(range: IntRange, operator: String, arguments: List<String>): CommentMarker {
            assert(operator.startsWith('#'))
            return CommentMarker(range)
        }
    }
}

/** A reference marker. */
data class RefMarker(
    /** The identifier of the declaration to which the reference should resolve. */
    val declId: String,
    /** The identifiers of the context markers. */
    val contextIds: List<String>,
    /** The input text of the reference. */
    val text: String,
    /** The expected text of the reference. */
    val expectedText: String,
    override val range: IntRange,
): Marker {
    override val replacementText get() = expectedText
    override fun toString(): String = StringBuilder().apply {
        append("->$declId")
        append(contextIds.joinToString("") { "|&$it" })
        append("|$text")
        if (text != expectedText) append("|$expectedText")
    }.toString()

    companion object {
        fun read(range: IntRange, operator: String, arguments: List<String>): RefMarker {
            assert(operator.startsWith("->"))
            // Reference
            val id = operator.substring(2).trim()
            // Context specifiers
            val contexts = arguments.filter { it.startsWith("&") }.map { it.substring(1).trim() }
            val otherValues = arguments.filter { !it.startsWith("&") && !it.startsWith("@") && !it.startsWith("->") }
            val t = otherValues.firstOrNull() ?: error("No initial name for reference $operator")
            val et = otherValues.drop(1).firstOrNull() ?: t
            return RefMarker(id, contexts, t, et, range)
        }
    }
}

/** A declaration marker. */
data class DeclMarker(
    /** The identifier of the declaration. */
    val id: String,
    /** The text of the declaration. */
    val text: String,
    override val range: IntRange,
): Marker {
    override val replacementText get() = text
    override fun toString(): String = "@$id|$text"

    companion object {
        fun read(range: IntRange, operator: String, arguments: List<String>): DeclMarker {
            assert(operator.startsWith('@'))
            val id = operator.substring(1).trim()
            val t = arguments.firstOrNull() ?: error("No name for declaration $operator")
            return DeclMarker(id, t, range)
        }
    }
}

/** An annotation marker. */
data class AnnotationMarker(
    val name: String,
    val arguments: List<String>,
    override val range: IntRange,
): Marker {
    override val replacementText get() = ""
    override fun toString(): String = "$name(${arguments.joinToString(", ")})"

    companion object {
        fun read(range: IntRange, operator: String, arguments: List<String>): AnnotationMarker {
            assert(operator.startsWith('{'))
            assert(operator.startsWith('}'))
            val text = operator.substring(1, operator.length - 1)
            val name = text.substringBefore('(')
            val parameters = text.substringAfter('(').substringBefore(')').split(',').map { it.trim() }
            return AnnotationMarker(name, parameters, range)
        }
    }
}