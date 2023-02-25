package net.pelsmaeker.generator.cli

/** Information about the operating system. */
object OsInfo {
    /** The name of the OS on which the application instance is running. */
    val name: String = System.getProperty("os.name", null)!!
    /** The version of the OS on which the application instance is running. */
    val version: String = System.getProperty("os.version", null)!!
    /** The architecture of the OS on which the application instance is running. */
    val architecture: String = System.getProperty("os.arch", null)!!
}