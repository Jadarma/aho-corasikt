// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt

import io.github.jadarma.ahocorasikt.CaseSensitivity.Sensitive
import io.github.jadarma.ahocorasikt.MatchType.Unrestricted
import io.github.jadarma.ahocorasikt.Overlap.Allowed

/** Syntactic sugar wrapping the normal [Trie.Builder] into a builder function. */
public inline fun buildTrie(
    caseSensitivity: CaseSensitivity = Sensitive,
    matchType: MatchType = Unrestricted,
    overlap: Overlap = Allowed,
    builderAction: Trie.Builder.() -> Unit,
): Trie = Trie.builder(caseSensitivity, matchType, overlap).apply(builderAction).build()

/** Syntactic sugar wrapping the normal [PayloadTrie.Builder] into a builder function. */
public inline fun <T: Any> buildTrie(
    caseSensitivity: CaseSensitivity = Sensitive,
    matchType: MatchType = Unrestricted,
    overlap: Overlap = Allowed,
    builderAction: PayloadTrie.Builder<T>.() -> Unit,
): PayloadTrie<T> = PayloadTrie.builder<T>(caseSensitivity, matchType, overlap).apply(builderAction).build()

/** Constructs a [Trie] matching these [words] using specified settings. */
public fun trieOf(
    words: Iterable<CharSequence>,
    caseSensitivity: CaseSensitivity = Sensitive,
    matchType: MatchType = Unrestricted,
    overlap: Overlap = Allowed,
): Trie = buildTrie(caseSensitivity, matchType, overlap) { addAll(words) }

/** Constructs a [PayloadTrie] matching these [words], associated with payloads, using specified settings. */
public fun <T: Any> trieOf(
    words: Map<out CharSequence, T>,
    caseSensitivity: CaseSensitivity = Sensitive,
    matchType: MatchType = Unrestricted,
    overlap: Overlap = Allowed,
): PayloadTrie<T> = buildTrie<T>(caseSensitivity, matchType, overlap) { addAll(words) }

/** Constructs a [Trie] matching these [words] using specified settings. */
public fun trieOf(
    vararg words: CharSequence,
    caseSensitivity: CaseSensitivity = Sensitive,
    matchType: MatchType = Unrestricted,
    overlap: Overlap = Allowed,
): Trie = buildTrie(caseSensitivity, matchType, overlap) { addAll(words.asIterable()) }

/** Constructs a [PayloadTrie] matching these [words], associated with payloads, using specified settings. */
public fun <T: Any> trieOf(
    vararg words: Pair<out CharSequence, out T>,
    caseSensitivity: CaseSensitivity = Sensitive,
    matchType: MatchType = Unrestricted,
    overlap: Overlap = Allowed,
): PayloadTrie<T> = buildTrie<T>(caseSensitivity, matchType, overlap) {
    for (entry in words) {
        add(word = entry.first, payload = entry.second)
    }
}
