package net.pelsmaeker.generator.stage2

import net.pelsmaeker.generator.TestSuite

/**
 * Reads refactoring test cases from a file.
 */
object RefactoringTestSuiteReader: TestSuiteReader() {

    override fun read(name: String, directory: String, text: String): TestSuite {
        TODO()
        // Read the input code (with markers)
        // Read the expected code
        // Read any annotations (e.g., disabled)
        // Find a marker of the form [[@ name]] of the class to move
        // Find a marker of the form [[-> name]] of the package to move to
        // Create highlights for the markers, replacing them with their text in the input text
        // Create parse test, analysis test
        // Create refactoring test
        // Return test suite
    }

}