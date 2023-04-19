package net.pelsmaeker.generator

import net.pelsmaeker.generator.utils.writeTestContent
import net.pelsmaeker.generator.utils.writeln
import java.io.Writer

/** Specifies the kind of test to generate. */
enum class TestKind {
    Parsing,
    Analysis,
    RefRet,
//    MoveClass,
}

/** A reference, declaration, or context highlight in the Java source code. */
data class Highlight(
    /** A range in the expected source code. */
    val range: IntRange,
)

/** A test suite. */
data class TestSuite(
    /** The test suite name, such as `testStaticImport5`. */
    val name: String,
    /** The directory with the project, such as `RenameStaticMethod`. */
    val directory: String,
    /** The test cases. */
    val cases: List<TestCase>,
    /** Whether the test suite is disabled. */
    val isDisabled: Boolean,
)

/** A test case. */
interface TestCase {
    /** The name of the test. */
    val name: String
    /** Whether the test is disabled. */
    val isDisabled: Boolean
    /** The input content. */
    val inputContent: String

    /**
     * Writes the test case to the specified writer.
     *
     * @param writer the writer to write to
     */
    fun write(writer: Writer)

    /**
     * Whether the test case is acceptable for the specified kinds of tests.
     *
     * @param kinds the kinds of tests
     * @return `true` if the test case is acceptable for the specified kinds of tests; otherwise, `false`
     */
    fun isAcceptable(kinds: Collection<TestKind>): Boolean
}

/** Parse test case. */
data class ParseTestCase(
    /** The name of the test. */
    override val name: String,
    /** Whether the test is disabled. */
    override val isDisabled: Boolean,
    /** The input content. */
    override val inputContent: String,
) : TestCase {
    override fun write(writer: Writer) {
        writer.apply {
            writeln()
            if (isDisabled) writeln("/*")
            writeln("test $name [[")
            writeTestContent(inputContent)
            writeln("]] parse succeeds")
            if (isDisabled) writeln("*/")
        }
    }

    override fun isAcceptable(kinds: Collection<TestKind>): Boolean = kinds.isEmpty() || kinds.contains(TestKind.Parsing)
}


/** Analysis test case. */
data class AnalysisTestCase(
    /** The name of the test. */
    override val name: String,
    /** Whether the test is disabled. */
    override val isDisabled: Boolean,
    /** The input content. */
    override val inputContent: String,
) : TestCase {
    override fun write(writer: Writer) {
        writer.apply {
            writeln()
            if (isDisabled) writeln("/*")
            writeln("test $name [[")
            writeTestContent(inputContent)
            writeln("]] analysis succeeds")
            if (isDisabled) writeln("*/")
        }
    }

    override fun isAcceptable(kinds: Collection<TestKind>): Boolean = kinds.isEmpty() || kinds.contains(TestKind.Analysis)
}


/** `test-analyze` test case. */
data class TestAnalyzeTestCase(
    /** The name of the test. */
    override val name: String,
    /** Whether the test is disabled. */
    override val isDisabled: Boolean,
    /** The input content. */
    override val inputContent: String,
) : TestCase {
    override fun write(writer: Writer) {
        writer.apply {
            writeln()
            if (isDisabled) writeln("/*")
            writeln("test $name [[")
            writeTestContent(inputContent)
            writeln("]] run test-analyze to SUCCEED()")
            if (isDisabled) writeln("*/")
        }
    }

    override fun isAcceptable(kinds: Collection<TestKind>): Boolean = kinds.isEmpty() || kinds.contains(TestKind.Analysis)
}

/** Reference retention test case. */
data class RefRetTestCase(
    /** The name of the test. */
    override val name: String,
    /** Whether the test is disabled. */
    override val isDisabled: Boolean,
    /** The input content. */
    override val inputContent: String,
    /** The selections in the input content; ordered from first to last. */
    val selections: List<Highlight>,
    /** The original reference text, that is to be replaced with the expected reference. */
    val originalRefText: String,
    /** The zero-based index of a reference (in [selections]). */
    val refIndex: Int,
    /** The zero-based index of a declaration (in [selections]) to which the reference should resolve. */
    val declIndex: Int,
    /** The zero-based index of the contexts (in [selections]) used with the reference. */
    val contextIndexes: List<Int>,
) : TestCase {
    override fun write(writer: Writer) {
        writer.apply {
            // Hack to get the indices in the right order.
            val indices = (listOf(
                "ref" to refIndex,
                "decl" to declIndex
            ) + contextIndexes
                .mapIndexed { i, e -> "ctx$i" to e })
                .sortedWith { (_, a), (_, b) -> a.compareTo(b) }
                .map { it.first }

            writeln()
            if (isDisabled) writeln("/*")
            writeln("test $name [[")
            val refId = selections[refIndex]
            val declId = selections[declIndex]
            if (contextIndexes.isNotEmpty()) {
                val ctxIds = contextIndexes.map { selections[it] }
                writeTestContent(inputContent, listOf(refId, declId) + ctxIds, mapOf(refId to originalRefText))
                writeln("]] run fix-reference(|#${indices.indexOf("ref") + 1}, #${indices.indexOf("decl") + 1}, ${List(contextIndexes.size) { i -> "#${indices.indexOf("ctx$i") + 1}" }.joinToString(", ")}) to [[")
            } else {
                writeTestContent(inputContent, listOf(refId, declId), mapOf(refId to originalRefText))
                writeln("]] run fix-reference(|#${indices.indexOf("ref") + 1}, #${indices.indexOf("decl") + 1}) to [[")
            }
            writeTestContent(inputContent)
            writeln("]]")
            if (isDisabled) writeln("*/")
        }
    }

    override fun isAcceptable(kinds: Collection<TestKind>): Boolean = kinds.isEmpty() || kinds.contains(TestKind.RefRet)
}