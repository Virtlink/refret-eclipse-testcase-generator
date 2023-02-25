package net.pelsmaeker.generator.cli

/** Information about the JVM. */
object JvmInfo {
    /** The Java version on which the Labback instance is running. */
    val javaVersion: String = System.getProperty("java.version", null)!!
    /** The JVM version on which the Labback instance is running. */
    val version: String = System.getProperty("java.vm.version", null)!!
    /** The JVM vendor on which the Labback instance is running. */
    val vendor: String = System.getProperty("java.vm.vendor", null)!!
}