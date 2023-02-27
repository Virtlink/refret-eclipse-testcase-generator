package net.pelsmaeker.generator.stage2

/** A test suite. */
data class TestSuite(
    /** The test suite name, such as `testStaticImport5`. */
    val name: String,
    /** The directory with the project, such as `RenameStaticMethod`. */
    val directory: String,
    /** The expected text. */
    val expectedText: String,
    /** The declarations and references highlighted in the test; ordered from first to last. */
    val identifiers: List<Highlight>,
    /** The test cases. */
    val cases: List<TestCase>,
    /** Whether the test suite is disabled. */
    val isDisabled: Boolean,
)

/** A test case. */
data class TestCase(
    /** The zero-based index of a reference (in [TestSuite.identifiers]). */
    val refIndex: Int,
    /** The zero-based index of a declaration (in [TestSuite.identifiers]) to which the reference should resolve. */
    val declIndex: Int,
    /** The zero-based index of the context (in [TestSuite.identifiers]) used with the reference, if any; otherwise, `null`. */
    val contextIndex: Int?,
    /** The original reference text, that is to be replaced with the expected reference. */
    val originalRefText: String,
)

/** A reference, declaration, or context highlight in the Java source code. */
data class Highlight(
    /** A range in the expected source code. */
    val range: IntRange,
)
