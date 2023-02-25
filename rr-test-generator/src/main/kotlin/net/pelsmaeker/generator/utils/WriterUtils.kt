package net.pelsmaeker.generator.utils

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