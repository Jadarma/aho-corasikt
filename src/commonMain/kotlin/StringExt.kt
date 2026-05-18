// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
@file:Suppress("TooManyFunctions", "NOTHING_TO_INLINE")

package io.github.jadarma.ahocorasikt

/**
 * Inverts the [Trie.search] function to have the text as receiver.
 * Equivalent of calling `trie.search(this)`.
 */
public inline fun CharSequence.findRanges(trie: Trie): Sequence<IntRange> =
    trie.search(this)

/** Returns whether any substring matches the [trie]. */
public inline infix fun CharSequence.matches(trie: Trie): Boolean =
    trie in this

/** Returns whether any substring matches the [trie]. */
public operator fun CharSequence.contains(trie: Trie): Boolean =
    trie.search(this).any()

/** Returns whether the trie contains the [word] as a match. */
public operator fun Trie.contains(word: CharSequence): Boolean {
    if(word.length > depth) return false
    return 0..word.lastIndex in search(word)
}

/** Returns the number of substrings matching the [trie]. */
public fun CharSequence.count(trie: Trie): Int =
    trie.search(this).count()

/** Returns a sequence of substrings that match the [trie], together with their starting index in the sequence. */
public fun CharSequence.find(trie: Trie): Sequence<Pair<Int, String>> =
    trie.search(this).map { range -> range.first to substring(range) }

/** Returns the first substring that matches the [trie], or `null` if no such sequence was found. */
public fun CharSequence.findFirst(trie: Trie): String? =
    trie.search(this).firstOrNull()?.let(this::substring)

/** Returns the first substring that matches the [trie], or `null` if no such sequence was found. */
public fun CharSequence.findLast(trie: Trie): String? =
    trie.search(this).lastOrNull()?.let(this::substring)

/** Finds the first occurrence of any substring matching the [trie], starting from the specified [startIndex]. */
public fun CharSequence.findAnyOf(trie: Trie, startIndex: Int = 0): Pair<Int, String>? {
    if (startIndex !in indices) throw IndexOutOfBoundsException("startIndex $startIndex outside of text $indices.")
    val match = trie.search(slice(startIndex..lastIndex)).firstOrNull() ?: return null
    val actualMatch = startIndex + match.first..startIndex + match.last
    return actualMatch.first to substring(actualMatch)
}

/** Finds the first occurrence of any substring matching the [trie], starting from the specified [startIndex]. */
public fun CharSequence.findLastAnyOf(trie: Trie, startIndex: Int = 0): Pair<Int, String>? {
    if (startIndex !in indices) throw IndexOutOfBoundsException("startIndex $startIndex outside of text $indices.")
    val match = trie.search(slice(startIndex..lastIndex)).lastOrNull() ?: return null
    val actualMatch = startIndex + match.first..startIndex + match.last
    return actualMatch.first to substring(actualMatch)
}

/** Returns the index of the first substring matching the [trie], or -1 if the char sequence does not contain any. */
public fun CharSequence.indexOfFirst(trie: Trie): Int =
    trie.search(this).firstOrNull()?.first ?: -1

/** Returns the index of the last substring matching the [trie], or -1 if the char sequence does not contain any. */
public fun CharSequence.indexOfLast(trie: Trie): Int =
    trie.search(this).lastOrNull()?.first ?: -1

/**
 * Splits the sequence to a sequence of substrings delimited by [trie] matches.
 * The even-indexed values are non-matches, and the odd-indexed values are matches.
 * Since the matches need to not overlap for the split to make sense, respects the [trie]'s overlap configuration.
 * If set to [Overlap.Allowed], then [Overlap.Leftmost] will be used instead.
 */
public fun CharSequence.splitToSequence(trie: Trie, limit: Int = 0): Sequence<String> {
    require(limit >= 0) { "Limit must be non-negative, but was $limit" }
    val search = trie.search(this, requireNoOverlaps = true)
    val spans = if (limit == 0) search else search.take(limit)
    return sequence {
        var index = 0
        for (span in spans) {
            yield(substring(index until span.first))
            yield(substring(span))
            index = span.last + 1
        }
        if (index <= lastIndex) {
            yield(substring(index..lastIndex))
        }
    }
}

/**
 * Splits the sequence to a list of substrings delimited by [trie] matches.
 * The even-indexed values are non-matches, and the odd-indexed values are matches.
 * Since the matches need to not overlap for the split to make sense, respects the [trie]'s overlap configuration.
 * If set to [Overlap.Allowed], then [Overlap.Leftmost] will be used instead.
 */
public inline fun CharSequence.split(trie: Trie, limit: Int = 0): List<String> =
    splitToSequence(trie, limit).toList()

/**
 * Returns a sequence containing the results of applying the given [transform] function to each substring in the
 * original sequence that matches the [trie].
 */
public fun <R> CharSequence.map(trie: Trie, transform: (String) -> R): Sequence<R> =
    trie
        .search(this)
        .map(::substring)
        .map(transform)

/**
 * Returns a sequence containing only the non-null results of applying the given [transform] function to each substring
 * in the original sequence that matches the [trie].
 */
public fun <R : Any> CharSequence.mapNotNull(trie: Trie, transform: (String) -> R?): Sequence<R> =
    trie
        .search(this)
        .map(::substring)
        .mapNotNull(transform)

/**
 * Returns a sequence containing the results of applying the given [transform] function to each substring in the
 * original sequence that matches the [trie] and its starting index.
 */
public fun <R> CharSequence.mapIndexed(trie: Trie, transform: (index: Int, String) -> R): Sequence<R> =
    trie
        .search(this)
        .map { range -> transform(range.first, substring(range)) }

/**
 * Returns a sequence containing only the non-null results of applying the given [transform] function to each substring
 * in the original sequence that matches the [trie] and its starting index, and keeping only the non-null results.
 */
public fun <R : Any> CharSequence.mapIndexedNotNull(trie: Trie, transform: (index: Int, String) -> R?): Sequence<R> =
    trie
        .search(this)
        .mapNotNull { range -> transform(range.first, substring(range)) }

/**
 * Returns a sequence of all elements yielded from the [transform] function being invoked on each substring in the
 * original sequence that matches the [trie].
 */
public fun <R> CharSequence.flatMap(trie: Trie, transform: (String) -> Iterable<R>): Sequence<R> =
    trie
        .search(this)
        .map(::substring)
        .flatMap(transform)

/**
 * Returns a sequence of all elements yielded from the [transform] function being invoked on each substring in the
 * original sequence that matches the [trie] and its starting index.
 */
public fun <R> CharSequence.flatMapIndexed(trie: Trie, transform: (index: Int, String) -> Iterable<R>): Sequence<R> =
    trie
        .search(this)
        .flatMap { range -> transform(range.first, substring(range)) }

/**
 * Returns a char sequence obtained by removing all subsequences that match the [trie].
 * Since the matches need to not overlap for the split to make sense, respects the [trie]'s overlap configuration.
 * If set to [Overlap.Allowed], then [Overlap.Leftmost] will be used instead.
 */
public fun CharSequence.remove(trie: Trie): String =
    this.replace(trie, replacement = "")

/**
 * Returns a new string obtained by replacing each substring of this char sequence that matches the given [trie] with
 * a [replacement].
 * Since the matches need to not overlap for the split to make sense, respects the [trie]'s overlap configuration.
 * If set to [Overlap.Allowed], then [Overlap.Leftmost] will be used instead.
 */
public fun CharSequence.replace(trie: Trie, replacement: String): String =
    this.replace(trie) { replacement }

/**
 * Returns a new string obtained by replacing each substring of this char sequence that matches the give [trie] with
 * the result of the given [transform] function that takes the matching substring and returns a string to be used as a
 * replacement for that match.
 * Since the matches need to not overlap for the split to make sense, respects the [trie]'s overlap configuration.
 * If set to [Overlap.Allowed], then [Overlap.Leftmost] will be used instead.
 */
public fun CharSequence.replace(trie: Trie, transform: (String) -> CharSequence): String {
    val sb = StringBuilder()
    var isMatch = false
    for (span in splitToSequence(trie)) {
        if (isMatch) sb.append(transform(span))
        else sb.append(span)
        isMatch = !isMatch
    }
    return sb.toString()
}

/**
 * Returns a new string obtained by applying the [transform] function on each span as resulted from [splitting][split]
 * the original sequence using the [trie], and whether it is a match or not.
 */
public fun CharSequence.reformat(trie: Trie, transform: (String, isMatch: Boolean) -> CharSequence): String {
    val sb = StringBuilder()
    var isMatch = false
    for (span in splitToSequence(trie)) {
        sb.append(transform(span, isMatch))
        isMatch = !isMatch
    }
    return sb.toString()
}
