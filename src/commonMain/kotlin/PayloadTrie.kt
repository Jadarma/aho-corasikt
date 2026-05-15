// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt

import io.github.jadarma.ahocorasikt.CaseSensitivity.Sensitive
import io.github.jadarma.ahocorasikt.MatchType.Unrestricted
import io.github.jadarma.ahocorasikt.Overlap.Allowed
import io.github.jadarma.ahocorasikt.Overlap.Leftmost
import io.github.jadarma.ahocorasikt.internal.builder.TrieBuilder

/**
 * A payload trie also associates a value of [T] to every keyword in the dictionary.
 * It can also function as a regular [Trie], but has extension functions that also allow the emitting of the associated
 * payload data.
 *
 * The type [T] should implement an equality check.
 * The trie stores only the unique payloads, which are shared if multiple keywords map to the same value.
 */
public interface PayloadTrie<T : Any> : Trie {

    /** Returns whether the [payload] is associated with any match of this trie. */
    public operator fun contains(payload: T): Boolean

    /**
     * Applies the search algorithm on the given text.
     *
     * @param text              The input character sequence to search.
     * @param requireNoOverlaps If `true` and this tries' [overlap] is [Allowed], forces the use of [Leftmost] instead,
     *                          in order to ensure no yielded match ranges overlap.
     * @return A lazy iterable of emits, comprising two values:
     *         - An [IntRange] representing the indices of the [text] where a match was found.
     *         - The [T] payload associated with the matched keyword.
     */
    public fun searchWithPayload(
        text: CharSequence,
        requireNoOverlaps: Boolean = false,
    ): Sequence<Pair<IntRange, T>>

    /** A payload trie builder. */
    public interface Builder<T : Any> {

        /**
         * Add a new word to the match dictionary.
         *
         * @param word    The word to add as a match.
         * @param payload The payload to associate with this match.
         *
         * @return The same builder reference.
         *
         * @throws IllegalArgumentException if the given [word] is empty.
         * @throws IllegalStateException    if builder already consumed.
         */
        public fun add(word: CharSequence, payload: T): Builder<T>

        /**
         * Calls the [add] function for all [words].
         * If the function failed for a word, stops processing the rest and propagates the exception.
         *
         * @param words   The words to add as matches.
         * @param payload The payload to associate with all these matches.
         * @return The same builder reference.
         *
         * @throws IllegalArgumentException if any given word is empty.
         * @throws IllegalStateException    if builder was already consumed.
         */
        public fun addAll(words: Iterable<CharSequence>, payload: T): Builder<T> =
            apply { words.forEach { add(it, payload) } }

        /**
         * Calls the [add] function for all [words].
         * If the function failed for a word, stops processing the rest and propagates the exception.
         *
         * @param words   The words to add as matches.
         *                Every entry will match the key and associate it with the value.
         *
         * @return The same builder reference.
         *
         * @throws IllegalArgumentException if any given word is empty.
         * @throws IllegalStateException    if builder was already consumed.
         */
        public fun addAll(words: Map<out CharSequence, T>): Builder<T> =
            apply { words.forEach { add(it.key, it.value) } }

        /**
         * Finalize building the payload trie.
         *
         * @throws IllegalArgumentException if no words were added.
         * @throws IllegalStateException    if builder was already consumed.
         */
        public fun build(): PayloadTrie<T>
    }

    public companion object {
        /**
         * Create a new trie builder.
         *
         * @param T               The type of payloads this trie can emit.
         * @param caseSensitivity How case sensitivity is handled in input strings.
         *                        By default, every match is case-sensitive.
         * @param matchType       In what context should matches be emitted.
         *                        By default, any match is valid.
         * @param overlap         How the trie should handle matches whose ranges overlap.
         *                        By default, all matches are emitted.
         */
        public fun <T: Any> builder(
            caseSensitivity: CaseSensitivity = Sensitive,
            matchType: MatchType = Unrestricted,
            overlap: Overlap = Allowed,
        ): Builder<T> = TrieBuilder(caseSensitivity, matchType, overlap)
    }
}
