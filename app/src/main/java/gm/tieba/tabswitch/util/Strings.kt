package gm.tieba.tabswitch.util

/**
 * Returns a substring between the first occurrence of [after] and [before].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
fun String.substringBetween(
    after: String, before: String, missingDelimiterValue: String = this
): String {
    val indexOfAfter = indexOf(after)
    if (indexOfAfter == -1) return missingDelimiterValue
    val indexOfBefore = indexOf(before)
    if (indexOfBefore == -1) return missingDelimiterValue
    if (indexOfAfter > indexOfBefore) return missingDelimiterValue
    return substring(indexOfAfter + after.length, indexOfBefore)
}
