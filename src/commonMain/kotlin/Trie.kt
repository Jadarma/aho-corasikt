// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt

import io.github.jadarma.ahocorasikt.CaseSensitivity.Sensitive
import io.github.jadarma.ahocorasikt.MatchType.Unrestricted
import io.github.jadarma.ahocorasikt.Overlap.Ignored

/**
 * A text search trie for efficient string matching, allowing for matching against an arbitrary number of dictionary
 * entries in linear time.
 *
 * _Based on the [Bell Labs paper](https://cr.yp.to/bib/1975/aho.pdf) by Alfred Aho and Margaret Corasick._
 */
public interface Trie {

    /** How case sensitivity is handled in input strings. */
    public val caseSensitivity: CaseSensitivity

    /** In what context are matches emitted. */
    public val matchType: MatchType

    /** How this trie handles matches whose ranges overlap. */
    public val overlap: Overlap

    /** The depth of the trie, and the maximum length of any match this trie can find in an input string. */
    public val depth: Int

    /**
     * The number of nodes in the internal representation.
     * This value is informative and does not affect anything.
     */
    public val size: Int

    /**
     * Applies the search algorithm on the given text.
     *
     * @param text The input character sequence to search.
     * @return A lazy iterable of [IntRange]s representing the indices of the [text] where a match was found.
     */
    public fun search(text: CharSequence): Sequence<IntRange>

    /** A trie builder. */
    public interface Builder {

        /**
         * Add a new word to the match dictionary.
         *
         * @param word The word to add as a match.
         *
         * @return The same builder reference.
         *
         * @throws IllegalStateException if builder already consumed.
         */
        public fun add(word: CharSequence): Builder

        /** Calls the [add] function for all [words]. */
        public fun addAll(words: Iterable<CharSequence>): Builder = apply { words.forEach(::add) }

        /** Calls the [add] function for all [words]. */
        public fun addAll(vararg words: CharSequence): Builder = addAll(words.asIterable())

        /**
         * Finalize building the trie.
         *
         * @throws IllegalStateException If builder was already consumed.
         */
        public fun build(): Trie
    }

    public companion object {

        /**
         * Create a new trie builder.
         *
         * @param caseSensitivity How case sensitivity is handled in input strings.
         *                        By default, every match is case-sensitive.
         * @param matchType       In what context should matches be emitted.
         *                        By default, any match is valid.
         * @param overlap         How the trie should handle matches whose ranges overlap.
         *                        By default, all matches are emitted.
         */
        public fun builder(
            caseSensitivity: CaseSensitivity = Sensitive,
            matchType: MatchType = Unrestricted,
            overlap: Overlap = Ignored,
        ): Builder = TODO()
    }
}
