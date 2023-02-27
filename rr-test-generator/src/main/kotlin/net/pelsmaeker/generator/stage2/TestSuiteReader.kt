package net.pelsmaeker.generator.stage2

import net.pelsmaeker.generator.utils.replaceAll

/**
 * Reads test cases from a file.
 *
 * The file should be a Java file with multiple packages and multiple compilation units (as required by Java SPT tests).
 * Relevant references and declarations should be surrounded by `[[ ... ]]`. A declaration starts with an identifier
 * (e.g., a number), followed by a pipe, followed by the name of the declaration. For example, `[[1|foo]]` is a
 * declaration of a member named `foo`.
 *
 * A reference starts with an arrow, an identifier of the declaration it should resolve to (e.g., a number), followed by
 * a pipe, followed by the input name of the reference, followed by the expected (qualified) name of the reference.
 * If the expected name is omitted, it is assumed to be equal to the input name. Whitespace around the names or code is
 * ignored. For example, `[[->1|foo|B.foo]]` is a reference to the declaration `[[1|foo]]` in class `B`.
 */
object TestSuiteReader {

    // Annotations: "[[{disabled}]]"
    private val annotationRegex = Regex("""\[\[\{([^|\]]+)\}\]\]""")
    private val markerRegex = Regex("""\[\[(->[a-zA-Z0-9\_]+|@[a-zA-Z0-9\_]+)((?:\|[^|\]]+)*)\]\]""")

    /**
     * Reads a test suite from the given text with markers.
     *
     * A declaration should be marked with `[[id|name]]`, where `id` is an identifier and `name` is the name of the
     * declaration. A reference should be marked with `[[->id|initial|expected]]`, where `id` is the identifier of the
     * declaration to which the reference should resolve, `initial` is the initial (unrefactored, possibly wrong, maybe
     * qualified) name of the reference, and `expected` is the expected qualified name of the reference.
     *
     * @param name the test suite name
     * @param name the test suite qualifier; or `null`
     * @param directory the test suite directory
     * @param text the text with markers
     * @return the test suite
     */
    fun readTestSuite(name: String, directory: String, text: String): TestSuite {
        val refs = mutableListOf<Ref>()
        val decls = mutableListOf<Decl>()

        markerRegex.findAll(text).forEach { match ->
            val range = match.groups[0]!!.range
            val operator = match.groups[1]!!.value
            val values = match.groups[2]!!.value.substring(1).split('|')

            when {
                operator.startsWith('@') -> {
                    // Declaration
                    val id = operator.substring(1).trim()
                    val t = if (values.size > 0) values[0] else error("No initial name for reference $operator")
                    val decl = Decl(id, t, range)
                    decls.add(decl)
                }
                operator.startsWith("->") -> {
                    // Reference
                    val id = operator.substring(2).trim()
                    val t = if (values.size > 0) values[0] else error("No initial name for reference $operator")
                    val et = if (values.size > 1) values[1] else t
                    val ref = Ref(id, t, et, range)
                    refs.add(ref)
                }
                else -> error("Unknown operator: $operator")
            }
        }

        // Build the 'expected text' (the code without any markers and with the expected qualified references).
        // At the same time, convert the markers to JavaIds and build a list of all identifiers in order from first to last.
        val markerToId = mutableMapOf<Marker, JavaId>()
        val ids = mutableListOf<JavaId>()
        val expectedText = text.replaceAll(refs + decls, { it.range }) { m, _, s ->
            val id = m.toId(s)
            markerToId[m] = id
            ids.add(id)
            m.replacementText
        }

        // For each reference, find the corresponding declaration and build a test case.
        val cases = mutableListOf<TestCase>()
        refs.forEach { ref ->
            val decl = decls.firstOrNull { it.id == ref.toId } ?: error("No declaration for reference $ref")
            val declIndex = ids.indexOf(markerToId[decl])
            val refIndex = ids.indexOf(markerToId[ref])
            cases.add(TestCase(refIndex, declIndex, ref.text))
        }

        // Process any annotations
        val annotations = annotationRegex.findAll(text).map { it.groups[1]!!.value }.toSet()
        val isDisabled = annotations.contains("disabled")

        return TestSuite(
            name = name,
            directory = directory,
            expectedText = expectedText,
            identifiers = ids,
            cases = cases,
            isDisabled = isDisabled,
        )
    }

    /** Common interface for markers. */
    private sealed interface Marker {
        /** The replacement text for the marker, which is the actual text for a declaration and the expected text for a reference. */
        val replacementText: String
        /** The range of the marker in the text. */
        val range: IntRange

        /**
         * Converts the marker to a [JavaId].
         *
         * @param start the final start index of the marker in the expected text
         * @return the [JavaId]
         */
        fun toId(start: Int): JavaId {
            return JavaId(start until start + replacementText.length)
        }
    }

    /** A reference marker. */
    private data class Ref(
        /** The identifier of the declaration to which the reference should resolve. */
        val toId: String,
        /** The input text of the reference. */
        val text: String,
        /** The expected text of the reference. */
        val expectedText: String,
        override val range: IntRange,
    ): Marker {
        override val replacementText get() = expectedText
        override fun toString(): String = "->$toId|$text|$expectedText"
    }

    /** A declaration marker. */
    private data class Decl(
        /** The identifier of the declaration. */
        val id: String,
        /** The text of the declaration. */
        val text: String,
        override val range: IntRange,
    ): Marker {
        override val replacementText get() = text
        override fun toString(): String = "$id|$text"
    }
}