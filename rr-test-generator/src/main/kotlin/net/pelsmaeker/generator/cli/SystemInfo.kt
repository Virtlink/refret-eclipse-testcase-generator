package net.pelsmaeker.generator.cli

/** Information about the system. */
object SystemInfo {
    /** The name of the user that runs the application instance. */
    val userName: String = System.getProperty("user.name", null)!!
    /** The home directory of the user that runs the application instance. */
    val userHome: String = System.getProperty("user.home", null)!!
    /** The working directory of the application instance. */
    val workingDirectory: String = System.getProperty("user.dir", null)!!
    /** The maximum available memory for the application instance, in bytes. */
    val memoryLimit: Long = Runtime.getRuntime().maxMemory()
    /** The maximum available processors for the application instance, in cores. */
    val processorLimit: Int = Runtime.getRuntime().availableProcessors()
}