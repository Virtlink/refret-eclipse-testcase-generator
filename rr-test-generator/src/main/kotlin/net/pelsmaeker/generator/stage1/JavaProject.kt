package net.pelsmaeker.generator.stage1


/** A Java project. */
data class JavaProject(
    /** The name of the project, such as `testStaticImport5`. */
    val name: String,
    /** The qualifier for the project, such as `out`, or `null`. */
    val qualifier: String?,
    /** The directory with the project, such as `RenameStaticMethod`. */
    val directory: String,
    /** The packages in the project. */
    val packages: List<JavaPackage>,
)

/** A Java package. */
data class JavaPackage(
    /** The name of the package; or an empty string if it has no name. */
    val name: String,
    /** The compilation units in the package. */
    val units: List<JavaUnit>,
)

/** A Java compilation unit. */
data class JavaUnit(
    /** The name of the compilation unit. */
    val name: String,
    /** The text content of the file. */
    val text: String,
)
