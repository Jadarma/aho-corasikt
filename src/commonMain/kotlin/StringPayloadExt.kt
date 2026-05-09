// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
@file:Suppress("TooManyFunctions", "NOTHING_TO_INLINE")

package io.github.jadarma.ahocorasikt

/** Return whether any substring matches the [payload] of the [trie]. */
public fun <T: Any> CharSequence.contains(trie: PayloadTrie<T>, payload: T): Boolean =
    trie.searchWithPayload(this).any { it.third == payload }

/** Returns the number of substring matching the [payload] in the [trie]. */
public fun <T: Any> CharSequence.count(trie: PayloadTrie<T>, payload: T): Int =
    trie.searchWithPayload(this).count { it.third == payload }

/**
 * Returns a sequence of substrings that match the [trie], together with their starting index in the sequence and their
 * associated payload.
 */
public fun <T: Any> CharSequence.findWithPayload(trie: PayloadTrie<T>): Sequence<Triple<Int, CharSequence, T>> =
    trie
        .searchWithPayload(this)
        .map { (range, match, payload) -> Triple(range.first, match, payload) }

/**
 * Returns the payload associated with the first substring that matches the [trie], or `null` if no such sequence was
 * found.
 */
public fun <T: Any> CharSequence.findFirstPayload(trie: PayloadTrie<T>): T? =
    trie
        .searchWithPayload(this)
        .firstOrNull()
        ?.third

/**
 * Returns the payload associated with the last substring that matches the [trie], or `null` if no such sequence was
 * found.
 */
public fun <T: Any> CharSequence.findLastPayload(trie: PayloadTrie<T>): T? =
    trie
        .searchWithPayload(this)
        .lastOrNull()
        ?.third

/** Finds the first occurrence of any substring matching the [trie], starting from the specified [startIndex]. */
public fun <T: Any> CharSequence.findAnyPayloadOf(
    trie: PayloadTrie<T>,
    startIndex: Int = 0,
): Triple<Int, CharSequence, T>? {
    if (startIndex !in indices) throw IndexOutOfBoundsException("startIndex $startIndex outside of text $indices.")
    val (range, match, payload) = trie
        .searchWithPayload(slice(startIndex..lastIndex))
        .firstOrNull() ?: return null
    return Triple(startIndex + range.first, match, payload)
}

/** Finds the last occurrence of any substring matching the [trie], starting from the specified [startIndex]. */
public fun <T: Any> CharSequence.findLastAnyPayloadOf(
    trie: PayloadTrie<T>,
    startIndex: Int = 0,
): Triple<Int, CharSequence, T>? {
    if (startIndex !in indices) throw IndexOutOfBoundsException("startIndex $startIndex outside of text $indices.")
    val (range, match, payload) = trie
        .searchWithPayload(slice(startIndex..lastIndex))
        .lastOrNull() ?: return null
    return Triple(startIndex + range.first, match, payload)
}

/**
 * Splits the sequence to a sequence of substrings delimited by [trie] matches.
 * The even-indexed values are non-matches and have a `null` payload, and the odd-indexed values are matches.
 * Since the matches need to not overlap for the split to make sense, respects the [trie]'s overlap configuration.
 * If set to [Overlap.Allowed], then [Overlap.Leftmost] will be used instead.
 */
public fun <T: Any> CharSequence.splitWithPayloadToSequence(
    trie: PayloadTrie<T>,
    limit: Int = 0,
): Sequence<Pair<String, T?>> {
    require(limit >= 0) { "Limit must be non-negative, but was $limit" }
    val search = trie.searchWithPayload(this, requireNoOverlaps = true)
    val spans = if (limit == 0) search else search.take(limit)
    return sequence {
        var index = 0
        for ((range, match, payload) in spans) {
            yield(substring(index until range.first) to null)
            yield(match.toString() to payload)
            index = range.last + 1
        }
        if (index <= lastIndex) {
            yield(substring(index..lastIndex) to null)
        }
    }
}

/**
 * Splits the sequence to a list of substrings delimited by [trie] matches.
 * The even-indexed values are non-matches and have a `null` payload, and the odd-indexed values are matches.
 * Since the matches need to not overlap for the split to make sense, respects the [trie]'s overlap configuration.
 * If set to [Overlap.Allowed], then [Overlap.Leftmost] will be used instead.
 * If the payload is `null
 */
public inline fun <T: Any> CharSequence.splitWithPayload(trie: PayloadTrie<T>, limit: Int = 0): List<Pair<String, T?>> =
    splitWithPayloadToSequence(trie, limit).toList()

/**
 * Returns a sequence containing the results of applying the given [transform] function to each substring in the
 * original sequence that matches the [trie] and it's associated payload.
 */
public fun <T: Any, R> CharSequence.map(
    trie: PayloadTrie<T>,
    transform: (String, payload: T) -> R,
): Sequence<R> =
    trie
        .searchWithPayload(this)
        .map { (_, match, payload) -> transform(match.toString(), payload) }

/**
 * Returns a sequence containing only the non-null results of applying the given [transform] function to each substring
 * in the original sequence that matches the [trie] and it's associated payload.
 */
public fun <T: Any, R> CharSequence.mapNotNull(
    trie: PayloadTrie<T>,
    transform: (String, payload: T) -> R,
): Sequence<R> =
    trie
        .searchWithPayload(this)
        .mapNotNull { (_, match, payload) -> transform(match.toString(), payload) }

/**
 * Returns a sequence containing the results of applying the given [transform] function to each substring in the
 * original sequence that matches the [trie] and its starting index and associated payload.
 */
public fun <T: Any, R> CharSequence.mapIndexed(
    trie: PayloadTrie<T>,
    transform: (index: Int, String, payload: T) -> R,
): Sequence<R> =
    trie
        .searchWithPayload(this)
        .map { (range, match, payload) -> transform(range.first, match.toString(), payload) }

/**
 * Returns a sequence containing only the non-null results of applying the given [transform] function to each substring
 * in the original sequence that matches the [trie] and its starting index and associated payload.
 */
public fun <T: Any, R: Any> CharSequence.mapIndexedNotNull(
    trie: PayloadTrie<T>,
    transform: (index: Int, String, payload: T) -> R?,
): Sequence<R> =
    trie
        .searchWithPayload(this)
        .mapNotNull { (range, match, payload) -> transform(range.first, match.toString(), payload) }

/**
 * Returns a sequence of all elements yielded from the [transform] function being invoked on each substring in the
 * original sequence that matches the [trie] and its associated payload.
 */
public fun <T: Any, R> CharSequence.flatMap(
    trie: PayloadTrie<T>,
    transform: (String, payload: T) -> Iterable<R>,
): Sequence<R> =
    trie
        .searchWithPayload(this)
        .flatMap { (_, match, payload) -> transform(match.toString(), payload) }

/**
 * Returns a sequence of all elements yielded from the [transform] function being invoked on each substring in the
 * original sequence that matches the [trie] and its starting index and associated payload.
 */
public fun <T: Any, R> CharSequence.flatMapIndexed(
    trie: PayloadTrie<T>,
    transform: (index: Int, String, payload: T) -> Iterable<R>,
): Sequence<R> =
    trie
        .searchWithPayload(this)
        .flatMap { (range, match, payload) -> transform(range.first, match.toString(), payload) }

/**
 * Returns a char sequence obtained by removing all subsequences that match the given [payload] of the [trie].
 * Since the matches need to not overlap for the split to make sense, respects the [trie]'s overlap configuration.
 * If set to [Overlap.Allowed], then [Overlap.Leftmost] will be used instead.
 */
public fun <T: Any> CharSequence.remove(trie: PayloadTrie<T>, payload: T): String =
    replace(trie, payload, replacement = "")

/**
 * Returns a new string obtained by replacing each substring of this char sequence that matches the given [payload] of
 * the [trie] with a [replacement].
 * Since the matches need to not overlap for the split to make sense, respects the [trie]'s overlap configuration.
 * If set to [Overlap.Allowed], then [Overlap.Leftmost] will be used instead.
 */
public fun <T: Any> CharSequence.replace(trie: PayloadTrie<T>, payload: T, replacement: String): String =
    this.reformatWithPayload(trie) { span, foundPayload ->
        when(foundPayload) {
            null -> span
            payload -> replacement
            else -> span
        }
    }

/**
 * Returns a new string obtained by replacing each substring of this char sequence that matches the give [trie] with
 * the result of the given [transform] function that takes the matching substring and its associated payload and
 * returns a string to be used as a replacement for that match.
 * Since the matches need to not overlap for the split to make sense, respects the [trie]'s overlap configuration.
 * If set to [Overlap.Allowed], then [Overlap.Leftmost] will be used instead.
 */
public fun <T: Any> CharSequence.replace(
    trie: PayloadTrie<T>,
    transform: (String, payload: T) -> CharSequence,
): String {
    val sb = StringBuilder()
    for ((span, payload) in splitWithPayloadToSequence(trie)) {
        if (payload != null) sb.append(transform(span, payload))
        else sb.append(span)
    }
    return sb.toString()
}

/**
 * Returns a new string obtained by applying the [transform] function on each span as resulted from [splitting][split]
 * the original sequence using the [trie], and its associated payload.
 * The given string is a match if and only if the payload is not `null`.
 */
public fun <T: Any> CharSequence.reformatWithPayload(
    trie: PayloadTrie<T>,
    transform: (String, payload: T?) -> CharSequence,
): String {
    val sb = StringBuilder()
    for ((span, payload) in splitWithPayloadToSequence(trie)) {
        sb.append(transform(span, payload))
    }
    return sb.toString()
}
