package net.pelsmaeker.generator.utils

/**
 * Applies all replacements from the specified map of replacements to functions.
 *
 * @param replacements the replacements to apply
 * @param toRange the function to get the range of the replacement
 * @param toReplacement the function to get the replacement, the replaced text, and the start index of the replaced text
 * @return the string with all replacements applied
 */
fun <T> String.replaceAll(
    replacements: Iterable<T>,
    toRange: (T) -> IntRange,
    toReplacement: (T, String, Int) -> String,
): String = StringBuilder(this).apply {
    var adjustment = 0
    for((o, r) in replacements.map { r -> r to toRange(r) }.sortedBy { it.second.first }) {
        val start = r.first + adjustment
        val end = (r.last + 1) + adjustment
        val replacement = toReplacement(o, substring(start, end), start)
        replace(start, end, replacement)
        val oldLength = end - start
        val newLength = replacement.length
        adjustment += newLength - oldLength
    }
}.toString()